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
    val vaultUri: String? = null,
    val treeItems: List<WikiTreeItem> = emptyList(),
    val selectedPage: WikiPage? = null,
    val error: String? = null
)

private data class MdFile(
    val title: String,
    val path: String,
    val uri: Uri,
    val content: String,
    val breadcrumbs: List<String>
)

@HiltViewModel
class WikiViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(WikiUiState())
    val uiState: StateFlow<WikiUiState> = _uiState.asStateFlow()

    private var cachedFiles: List<MdFile> = emptyList()

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
        val page = cachedFiles.find { it.path == path } ?: return
        val backlinks = cachedFiles
            .filter { source ->
                parseWikiTargets(source.content).any { target -> resolvesToPath(target, page.path, cachedFiles) }
            }
            .map { it.path }
            .sorted()
        _uiState.value = _uiState.value.copy(
            selectedPage = WikiPage(
                title = page.title,
                path = page.path,
                uri = page.uri,
                content = page.content,
                breadcrumbs = page.breadcrumbs,
                backlinks = backlinks
            )
        )
    }

    fun openBacklink(path: String) = openPage(path)

    private fun loadVault(vaultUri: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, vaultUri = vaultUri)
            try {
                val (items, files) = withContext(Dispatchers.IO) {
                    scanVault(Uri.parse(vaultUri))
                }
                cachedFiles = files
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    treeItems = items,
                    selectedPage = files.firstOrNull()?.let { first ->
                        WikiPage(
                            title = first.title,
                            path = first.path,
                            uri = first.uri,
                            content = first.content,
                            breadcrumbs = first.breadcrumbs,
                            backlinks = files
                                .filter { source ->
                                    parseWikiTargets(source.content)
                                        .any { target -> resolvesToPath(target, first.path, files) }
                                }
                                .map { it.path }
                                .sorted()
                        )
                    }
                )
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    treeItems = emptyList(),
                    selectedPage = null,
                    error = t.message ?: "Failed to read vault"
                )
            }
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
                    val title = childName.removeSuffix(".md")
                    val content = context.contentResolver.openInputStream(child.uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: ""
                    val crumbs = fullPath.removeSuffix(".md").split('/').dropLast(1)
                    treeItems += WikiTreeItem(
                        name = childName,
                        path = fullPath.removeSuffix(".md"),
                        depth = depth,
                        isDirectory = false
                    )
                    files += MdFile(
                        title = title,
                        path = fullPath.removeSuffix(".md"),
                        uri = child.uri,
                        content = content,
                        breadcrumbs = crumbs
                    )
                }
            }
        }

        walk(root, "", 0)
        return treeItems to files.sortedBy { it.path.lowercase(Locale.US) }
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

private fun resolvesToPath(target: String, expectedPath: String, files: List<MdFile>): Boolean {
    val normalizedTarget = target.removeSuffix(".md").lowercase(Locale.US)
    val expected = expectedPath.lowercase(Locale.US)

    if (normalizedTarget == expected) return true

    val expectedTitle = expectedPath.substringAfterLast('/').lowercase(Locale.US)
    if (normalizedTarget == expectedTitle) return true

    val pathMatch = files.firstOrNull { it.path.lowercase(Locale.US) == normalizedTarget }
    if (pathMatch != null && pathMatch.path.lowercase(Locale.US) == expected) return true

    val titleMatch = files.firstOrNull { it.title.lowercase(Locale.US) == normalizedTarget }
    return titleMatch?.path?.lowercase(Locale.US) == expected
}
