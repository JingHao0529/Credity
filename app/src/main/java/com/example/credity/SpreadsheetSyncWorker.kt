package com.example.credity

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result as WorkResult
import java.util.concurrent.TimeUnit

class SpreadsheetSyncWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): WorkResult {
        return try {
            val sheetsRepo = GoogleSheetsRepository()
            val result = sheetsRepo.fetchLiveTransactionHistory()

            if (result.isSuccess) {
                val prefs = appContext.getSharedPreferences("credity_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("live_sheets_csv", result.getOrNull()).apply()

                // Update App Widget with latest figures
                CredityWidgetProvider.updateAllWidgets(appContext)
            }

            WorkResult.success()
        } catch (e: Exception) {
            WorkResult.retry()
        }
    }

    companion object {
        fun scheduleHourlySync(context: Context) {
            val syncRequest = PeriodicWorkRequestBuilder<SpreadsheetSyncWorker>(
                1, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "SpreadsheetHourlySync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
