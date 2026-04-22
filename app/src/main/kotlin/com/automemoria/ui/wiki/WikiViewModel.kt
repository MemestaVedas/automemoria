package com.automemoria.ui.wiki

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

private const val PREFS_NAME = "wiki_prefs"
private const val KEY_VAULT_URI = "vault_uri"

data class WikiPage(
    val title: String,
    val path: String,
    val uri: Uri,
    val content: String,
    val breadcrumbs: List<String>,
    val backlinks: List<String>
)

data class WikiTreeItem(
    val name: String,
    val path: String,
    val depth: Int,
    val isDirectory: Boolean
)

data class WikiUiState(
    val isLoading: Boolean = false,
    val isIndexingLinks: Boolean = false,
    val vaultUri: String? = null,
    val treeItems: List<WikiTreeItem> = emptyList(),
    val selectedPage: WikiPage? = null,
    val error: String? = null
)

private data class MdFile(
    val title: String,
    val path: String,
    val uri: Uri,
    val breadcrumbs: List<String>
)

@HiltViewModel
class WikiViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(WikiUiState())
    val uiState: StateFlow<WikiUiState> = _uiState.asStateFlow()

    private var cachedFiles: List<MdFile> = emptyList()
    private val contentCache = mutableMapOf<String, String>()
    private var backlinksByTargetPath: Map<String, List<String>> = emptyMap()

    init {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_VAULT_URI, null)
        if (!saved.isNullOrBlank()) {
            loadVault(saved)
        }
    }

    fun onVaultSelected(uri: Uri) {
        val uriString = uri.toString()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_VAULT_URI, uriString)
            .apply()
        loadVault(uriString)
    }

    fun reload() {
        val uri = _uiState.value.vaultUri ?: return
        loadVault(uri)
    }

    fun openPage(path: String) {
        viewModelScope.launch {
            val page = cachedFiles.find { it.path == path } ?: return@launch
            val content = withContext(Dispatchers.IO) { readContent(page) }
            _uiState.value = _uiState.value.copy(
                selectedPage = WikiPage(
                    title = page.title,
                    path = page.path,
                    uri = page.uri,
                    content = content,
                    breadcrumbs = page.breadcrumbs,
                    backlinks = backlinksByTargetPath[page.path].orEmpty()
                )
            )
        }
    }

    fun openBacklink(path: String) = openPage(path)

    private fun loadVault(vaultUri: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isIndexingLinks = false,
                error = null,
                vaultUri = vaultUri
            )
            try {
                val (items, files) = withContext(Dispatchers.IO) {
                    scanVault(Uri.parse(vaultUri))
                }

                cachedFiles = files
                contentCache.clear()
                backlinksByTargetPath = emptyMap()

                val firstPage = files.firstOrNull()?.let { first ->
                    val content = withContext(Dispatchers.IO) { readContent(first) }
                    WikiPage(
                        title = first.title,
                        path = first.path,
                        uri = first.uri,
                        content = content,
                        breadcrumbs = first.breadcrumbs,
                        backlinks = emptyList()
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    treeItems = items,
                    selectedPage = firstPage
                )

                buildBacklinkIndex(files)
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isIndexingLinks = false,
                    treeItems = emptyList(),
                    selectedPage = null,
                    error = t.message ?: "Failed to read vault"
                )
            }
        }
    }

    private fun buildBacklinkIndex(files: List<MdFile>) {
        if (files.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isIndexingLinks = true)
            val backlinks = withContext(Dispatchers.IO) {
                val pathLookup = files.associateBy { normalizePathKey(it.path) }
                val titleLookup = files.groupBy { normalizePathKey(it.title) }

                val backlinksMap = mutableMapOf<String, MutableSet<String>>()

                files.forEach { source ->
                    val targets = parseWikiTargets(readContent(source))
                        .mapNotNull { target ->
                            resolveTargetToPath(target, pathLookup, titleLookup)
                        }
                        .distinct()

                    targets.forEach { targetPath ->
                        backlinksMap.getOrPut(targetPath) { mutableSetOf() }.add(source.path)
                    }
                }

                backlinksMap.mapValues { it.value.toList().sorted() }
            }

            backlinksByTargetPath = backlinks
            val selected = _uiState.value.selectedPage
            _uiState.value = _uiState.value.copy(
                isIndexingLinks = false,
                selectedPage = selected?.copy(backlinks = backlinks[selected.path].orEmpty())
            )
        }
    }

    private fun scanVault(treeUri: Uri): Pair<List<WikiTreeItem>, List<MdFile>> {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalStateException("Cannot open selected folder")

        val treeItems = mutableListOf<WikiTreeItem>()
        val files = mutableListOf<MdFile>()

        fun walk(current: DocumentFile, parentPath: String, depth: Int) {
            val children = current.listFiles().sortedWith(
                compareBy<DocumentFile> { !it.isDirectory }
                    .thenBy { it.name?.lowercase(Locale.US) ?: "" }
            )

            for (child in children) {
                val childName = child.name ?: continue
                val fullPath = if (parentPath.isBlank()) childName else "$parentPath/$childName"

                if (child.isDirectory) {
                    treeItems += WikiTreeItem(
                        name = childName,
                        path = fullPath,
                        depth = depth,
                        isDirectory = true
                    )
                    walk(child, fullPath, depth + 1)
                } else if (child.isFile && childName.endsWith(".md", ignoreCase = true)) {
                    val title = removeMdExtension(childName)
                    val logicalPath = removeMdExtension(fullPath)
                    val crumbs = logicalPath.split('/').dropLast(1)
                    treeItems += WikiTreeItem(
                        name = childName,
                        path = logicalPath,
                        depth = depth,
                        isDirectory = false
                    )
                    files += MdFile(
                        title = title,
                        path = logicalPath,
                        uri = child.uri,
                        breadcrumbs = crumbs
                    )
                }
            }
        }

        walk(root, "", 0)
        return treeItems to files.sortedBy { it.path.lowercase(Locale.US) }
    }

    private fun readContent(file: MdFile): String {
        contentCache[file.path]?.let { return it }
        val text = context.contentResolver.openInputStream(file.uri)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: ""
        contentCache[file.path] = text
        return text
    }
}

private fun parseWikiTargets(content: String): List<String> {
    val pattern = "\\[\\[([^\\]]+)\\]\\]".toRegex()
    return pattern.findAll(content).mapNotNull { match ->
        val raw = match.groupValues[1]
            .substringBefore('|')
            .substringBefore('#')
            .trim()
        raw.ifBlank { null }
    }.toList()
}

private fun resolveTargetToPath(
    target: String,
    pathLookup: Map<String, MdFile>,
    titleLookup: Map<String, List<MdFile>>
): String? {
    val normalizedTarget = normalizePathKey(target)
    val directPath = pathLookup[normalizedTarget]
    if (directPath != null) return directPath.path

    val byTitle = titleLookup[normalizedTarget]
    if (!byTitle.isNullOrEmpty()) return byTitle.first().path

    return null
}

private fun normalizePathKey(value: String): String =
    removeMdExtension(value)
        .replace('\\', '/')
        .trim('/')
        .lowercase(Locale.US)

private fun removeMdExtension(value: String): String {
    return if (value.lowercase(Locale.US).endsWith(".md")) {
        value.dropLast(3)
    } else {
        value
    }
}
