package com.odensecurewave

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * WorkManagerServiceManager - WorkManager-based service management
 * This replaces the JobService implementation with WorkManager for better reliability and modern architecture
 */
object WorkManagerServiceManager {
    private const val TAG = "WorkManagerServiceManager"
    private const val PREFS_NAME = "workmanager_service_prefs"
    private const val KEY_SERVICE_ENABLED = "service_enabled"
    private const val WORK_NAME = "PeriodicBackgroundWork"
    private const val INTERVAL_MINUTES = 15L
    
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
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
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // No network required
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
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWorkRequest
            )
            
            // Mark service as enabled in preferences
            getSharedPreferences(context)
                .edit()
                .putBoolean(KEY_SERVICE_ENABLED, true)
                .apply()
            
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
            
            // Mark service as disabled in preferences
            getSharedPreferences(context)
                .edit()
                .putBoolean(KEY_SERVICE_ENABLED, false)
                .apply()
            
            Log.d(TAG, "WorkManager periodic service stopped successfully")
            ErrorLogger.logInfo(context, TAG, "stopPeriodicService", "WorkManager periodic service stopped successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop WorkManager periodic service", e)
            ErrorLogger.logError(context, TAG, "stopPeriodicService", e)
            throw e
        }
    }
    
    /**
     * Check if the periodic background service is enabled
     */
    fun isServiceEnabled(context: Context): Boolean {
        return try {
            val prefs = getSharedPreferences(context)
            val isEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
            Log.d(TAG, "Service enabled status: $isEnabled")
            isEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check service status", e)
            ErrorLogger.logError(context, TAG, "isServiceEnabled", e)
            false
        }
    }
    
    /**
     * Check if the WorkManager work is actually scheduled/running
     */
    fun isWorkScheduled(context: Context): Boolean {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WORK_NAME)
                .get()
            
            val isScheduled = workInfos.any { workInfo ->
                workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
            }
            
            Log.d(TAG, "Work scheduled status: $isScheduled")
            ErrorLogger.logInfo(context, TAG, "isWorkScheduled", "Work scheduled status checked", 
                mapOf("isScheduled" to isScheduled, "workInfoCount" to workInfos.size))
            
            isScheduled
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check work schedule status", e)
            ErrorLogger.logError(context, TAG, "isWorkScheduled", e)
            false
        }
    }
    
    /**
     * Initialize the service on app startup
     * This will restart the service if it was previously enabled
     */
    fun initializeService(context: Context) {
        try {
            if (isServiceEnabled(context)) {
                Log.d(TAG, "Service was previously enabled, restarting...")
                ErrorLogger.logInfo(context, TAG, "initializeService", "Service was previously enabled, restarting")
                startPeriodicService(context)
            } else {
                Log.d(TAG, "Service was not previously enabled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize service", e)
            ErrorLogger.logError(context, TAG, "initializeService", e)
        }
    }
    
    /**
     * Get comprehensive service status information for debugging
     */
    fun getServiceStatus(context: Context): Map<String, Any> {
        return try {
            val isEnabled = isServiceEnabled(context)
            val isWorkScheduled = isWorkScheduled(context)
            
            // Get detailed WorkManager information
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WORK_NAME)
                .get()
            
            val workStates = workInfos.map { it.state.name }
            val workTags = workInfos.flatMap { it.tags }
            
            mapOf(
                "enabled" to isEnabled,
                "workScheduled" to isWorkScheduled,
                "workStates" to workStates,
                "workTags" to workTags,
                "intervalMinutes" to INTERVAL_MINUTES,
                "workName" to WORK_NAME
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get service status", e)
            ErrorLogger.logError(context, TAG, "getServiceStatus", e)
            mapOf(
                "enabled" to false,
                "workScheduled" to false,
                "error" to (e.message ?: "Unknown error")
            )
        }
    }
    
    /**
     * Get the last execution information
     */
    fun getLastExecutionInfo(context: Context): Map<String, Any> {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WORK_NAME)
                .get()
            
            val lastWorkInfo = workInfos.maxByOrNull { it.outputData.keyValueMap.size }
            
            if (lastWorkInfo != null) {
                mapOf(
                    "lastState" to lastWorkInfo.state.name,
                    "runAttemptCount" to lastWorkInfo.runAttemptCount,
                    "outputData" to lastWorkInfo.outputData.keyValueMap,
                    "tags" to lastWorkInfo.tags.toList()
                )
            } else {
                mapOf("status" to "No execution information available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get last execution info", e)
            ErrorLogger.logError(context, TAG, "getLastExecutionInfo", e)
            mapOf("error" to (e.message ?: "Unknown error"))
        }
    }
}
