package com.automemoria

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.automemoria.sync.NetworkObserver
import com.automemoria.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class AutomemoriaApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var networkObserver: NetworkObserver

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Keep local and remote in sync in the background.
        SyncWorker.schedulePeriodic(this)

        // Activate network callbacks; observer triggers immediate sync on reconnect.
        networkObserver.networkStatus
            .onEach { /* callback side-effect lives in NetworkObserver */ }
            .launchIn(appScope)

        // Schedule periodic tasks
        com.automemoria.notifications.StreakAtRiskWorker.schedule(this)
    }

    override fun onTerminate() {
        appScope.cancel()
        super.onTerminate()
    }

    // Hilt-aware WorkManager configuration
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
