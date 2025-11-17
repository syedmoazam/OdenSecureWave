package com.odensecurewave

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * WorkManagerServiceManager - WorkManager-based service management
 * This replaces the JobService implementation with WorkManager for better reliability and modern architecture
 */
object WorkManagerServiceManager {
    private const val TAG = "WorkManagerServiceManager"
    private const val WORK_NAME = "PeriodicBackgroundWork"
    private const val INTERVAL_MINUTES = 15L
    
    /**
     * Start the periodic background service using WorkManager
     * This will schedule a PeriodicWorkRequest to run every 15 minutes
     */
    fun startPeriodicService(context: Context) {
        try {
            Log.d(TAG, "Starting WorkManager periodic background service")
            ErrorLogger.logInfo(context, TAG, "startPeriodicService", "Starting WorkManager periodic service")
            
            // Create constraints for the work
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Require active internet connection
                .setRequiresBatteryNotLow(false) // Can run on low battery (Device Owner privilege)
                .setRequiresCharging(false) // Don't require charging
                .setRequiresDeviceIdle(false) // Don't require device idle
                .build()
            
            // Create the periodic work request
            val periodicWorkRequest = PeriodicWorkRequestBuilder<PeriodicWorker>(
                INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag(WORK_NAME)
                .build()
            
            // Enqueue the work with replace policy to avoid duplicates
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
            
            
            Log.d(TAG, "WorkManager periodic service started successfully")
            ErrorLogger.logInfo(context, TAG, "startPeriodicService", "WorkManager periodic service started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start WorkManager periodic service", e)
            ErrorLogger.logError(context, TAG, "startPeriodicService", e)
            throw e
        }
    }

    /**
     * Stop the periodic background service
     * This will cancel the scheduled WorkManager work
     */
    fun stopPeriodicService(context: Context) {
        try {
            Log.d(TAG, "Stopping WorkManager periodic background service")
            ErrorLogger.logInfo(context, TAG, "stopPeriodicService", "Stopping WorkManager periodic service")
            
            // Cancel the work by unique name
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            
            
            Log.d(TAG, "WorkManager periodic service stopped successfully")
            ErrorLogger.logInfo(context, TAG, "stopPeriodicService", "WorkManager periodic service stopped successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop WorkManager periodic service", e)
            ErrorLogger.logError(context, TAG, "stopPeriodicService", e)
            throw e
        }
    }

    
    /**
     * Initialize the service on app startup
     * This will start the service unconditionally
     */
    fun initializeService(context: Context) {
        try {
            Log.d(TAG, "Initializing service...")
            ErrorLogger.logInfo(context, TAG, "initializeService", "Initializing service")
            startPeriodicService(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize service", e)
            ErrorLogger.logError(context, TAG, "initializeService", e)
        }
    }
}
