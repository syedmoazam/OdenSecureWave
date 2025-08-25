package com.odensecurewave

import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

/**
 * PeriodicWorker - WorkManager implementation for periodic background tasks
 * This worker executes every 30 minutes using WorkManager's periodic work requests
 */
class PeriodicWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    
    companion object {
        private const val TAG = "PeriodicWorker"
        private const val WAKELOCK_TAG = "OdenSecureWave:PeriodicWorker"
        private const val WAKELOCK_TIMEOUT = 5 * 60 * 1000L // 5 minutes max
    }

    override fun doWork(): Result {
        return try {
            Log.d(TAG, "PeriodicWorker started")
            ErrorLogger.logInfo(applicationContext, TAG, "doWork", "PeriodicWorker started")
            
            // Acquire wake lock to ensure task completes
            val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKELOCK_TAG
            )
            
            try {
                wakeLock.acquire(WAKELOCK_TIMEOUT)
                Log.d(TAG, "Wake lock acquired for periodic work")
                
                // Execute the periodic work
                performPeriodicWork()
                
                Log.d(TAG, "PeriodicWorker completed successfully")
                ErrorLogger.logInfo(applicationContext, TAG, "doWork", "PeriodicWorker completed successfully")
                
                Result.success()
                
            } finally {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                    Log.d(TAG, "Wake lock released")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in PeriodicWorker", e)
            ErrorLogger.logError(applicationContext, TAG, "doWork", e)
            
            // Return retry to reschedule the work
            Result.retry()
        }
    }
    
    /**
     * Perform the actual periodic work
     * This is where you implement your specific periodic tasks
     */
    private fun performPeriodicWork() {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            Log.d(TAG, "Performing periodic work at: $timestamp")
            
            // REQUIREMENT 1: Only execute if app is NOT currently running
            if (AppStateManager.isAppRunning(applicationContext)) {
                Log.d(TAG, "App is currently running - skipping periodic work")
                ErrorLogger.logInfo(applicationContext, TAG, "performPeriodicWork", "App is running - skipping periodic work")
                return
            }
            
            Log.d(TAG, "App is not running - proceeding with periodic work")
            ErrorLogger.logInfo(applicationContext, TAG, "performPeriodicWork", "App is not running - proceeding with periodic work")
            
            // REQUIREMENT 2: Fetch device status from Firebase and handle accordingly
            runBlocking {
                checkDeviceStatusAndAct()
            }
            
            // Execute other periodic tasks
            logDeviceStatus()
            checkDeviceAdminStatus()
            logSystemStats()
            verifyDeviceOwnerStatus()
            
            ErrorLogger.logInfo(applicationContext, TAG, "performPeriodicWork", "Periodic work completed", 
                mapOf("timestamp" to timestamp))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error performing periodic work", e)
            ErrorLogger.logError(applicationContext, TAG, "performPeriodicWork", e)
            throw e
        }
    }
    
    /**
     * Check device status from Firebase and take appropriate action
     */
    private suspend fun checkDeviceStatusAndAct() {
        try {
            Log.d(TAG, "Checking device status from Firebase")
            
            // Check if Firebase is available
            if (!FirebaseDeviceStatusManager.isFirebaseAvailable()) {
                Log.w(TAG, "Firebase is not available - skipping status check")
                ErrorLogger.logWarning(applicationContext, TAG, "checkDeviceStatusAndAct", "Firebase is not available")
                return
            }
            
            // Get device IMEI for Firebase operations
            val deviceId = FirebaseDeviceStatusManager.getDeviceImei(applicationContext)
            if (deviceId == null) {
                Log.w(TAG, "Could not get device IMEI - skipping status check")
                ErrorLogger.logWarning(applicationContext, TAG, "checkDeviceStatusAndAct", "Could not get device IMEI")
                return
            }
            
            // Fetch device status from Firebase
            val deviceStatus = FirebaseDeviceStatusManager.getDeviceStatus(applicationContext, deviceId)
            Log.d(TAG, "Device status from Firebase: $deviceStatus")
            
            when (deviceStatus) {
                "lock" -> {
                    Log.d(TAG, "Device status is 'lock' - executing lock procedures")
                    ErrorLogger.logInfo(applicationContext, TAG, "checkDeviceStatusAndAct", "Device status is 'lock' - executing lock procedures")
                    
                    // REQUIREMENT 3: Get current location and update Firebase
                    val locationData = DeviceLocationManager.getCurrentLocation(applicationContext)
                    if (locationData != null) {
                        Log.d(TAG, "Location retrieved: $locationData")
                        
                        // Update Firebase with location
                        val locationUpdated = FirebaseDeviceStatusManager.updateDeviceLocation(applicationContext, deviceId, locationData)
                        if (locationUpdated) {
                            Log.d(TAG, "Location updated in Firebase successfully")
                            ErrorLogger.logInfo(applicationContext, TAG, "checkDeviceStatusAndAct", "Location updated in Firebase successfully")
                        } else {
                            Log.w(TAG, "Failed to update location in Firebase")
                            ErrorLogger.logWarning(applicationContext, TAG, "checkDeviceStatusAndAct", "Failed to update location in Firebase")
                        }
                    } else {
                        Log.w(TAG, "Could not retrieve device location")
                        ErrorLogger.logWarning(applicationContext, TAG, "checkDeviceStatusAndAct", "Could not retrieve device location")
                    }
                    
                    // REQUIREMENT 4: Launch the app
                    val appLaunched = AppStateManager.launchApp(applicationContext)
                    if (appLaunched) {
                        Log.d(TAG, "App launched successfully for lock status")
                        ErrorLogger.logInfo(applicationContext, TAG, "checkDeviceStatusAndAct", "App launched successfully for lock status")
                    } else {
                        Log.w(TAG, "Failed to launch app for lock status")
                        ErrorLogger.logWarning(applicationContext, TAG, "checkDeviceStatusAndAct", "Failed to launch app for lock status")
                    }
                }
                "active" -> {
                    Log.d(TAG, "Device status is 'active' - no special action required")
                    ErrorLogger.logInfo(applicationContext, TAG, "checkDeviceStatusAndAct", "Device status is 'active' - no special action required")
                }
                null -> {
                    Log.w(TAG, "Could not retrieve device status from Firebase")
                    ErrorLogger.logWarning(applicationContext, TAG, "checkDeviceStatusAndAct", "Could not retrieve device status from Firebase")
                }
                else -> {
                    Log.d(TAG, "Device status is '$deviceStatus' - no special action defined")
                    ErrorLogger.logInfo(applicationContext, TAG, "checkDeviceStatusAndAct", "Device status retrieved", mapOf("status" to deviceStatus))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking device status and acting", e)
            ErrorLogger.logError(applicationContext, TAG, "checkDeviceStatusAndAct", e)
        }
    }
    

    
    /**
     * Log current device status
     */
    private fun logDeviceStatus() {
        try {
            val deviceInfo = mutableMapOf<String, Any>()
            
            // Battery information
            val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                deviceInfo["isPowerSaveMode"] = powerManager.isPowerSaveMode
                deviceInfo["isIgnoringBatteryOptimizations"] = powerManager.isIgnoringBatteryOptimizations(applicationContext.packageName)
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                deviceInfo["isDeviceIdleMode"] = powerManager.isDeviceIdleMode
            }
            
            // Network connectivity (basic check)
            val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = connectivityManager.activeNetworkInfo
            deviceInfo["isNetworkConnected"] = activeNetwork?.isConnected ?: false
            
            Log.d(TAG, "Device status: $deviceInfo")
            ErrorLogger.logInfo(applicationContext, TAG, "logDeviceStatus", "Device status logged", deviceInfo)
            
        } catch (e: Exception) {
            Log.w(TAG, "Could not log device status", e)
            ErrorLogger.logWarning(applicationContext, TAG, "logDeviceStatus", "Could not log device status", mapOf("error" to e.message))
        }
    }
    
    /**
     * Check device admin status
     */
    private fun checkDeviceAdminStatus() {
        try {
            val devicePolicyManager = applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            val adminComponentName = android.content.ComponentName(applicationContext, AppDeviceAdminReceiver::class.java)
            
            val isAdminActive = devicePolicyManager.isAdminActive(adminComponentName)
            val isDeviceOwner = try {
                devicePolicyManager.isDeviceOwnerApp(applicationContext.packageName)
            } catch (e: Exception) {
                false
            }
            
            val adminStatus = mapOf(
                "isAdminActive" to isAdminActive,
                "isDeviceOwner" to isDeviceOwner
            )
            
            Log.d(TAG, "Device admin status: $adminStatus")
            ErrorLogger.logInfo(applicationContext, TAG, "checkDeviceAdminStatus", "Device admin status checked", adminStatus)
            
            // If admin privileges are lost, this might be a security concern
            if (!isAdminActive) {
                ErrorLogger.logWarning(applicationContext, TAG, "checkDeviceAdminStatus", "Device admin is no longer active!")
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Could not check device admin status", e)
            ErrorLogger.logError(applicationContext, TAG, "checkDeviceAdminStatus", e)
        }
    }
    
    /**
     * Log system statistics
     */
    private fun logSystemStats() {
        try {
            val runtime = Runtime.getRuntime()
            val systemStats = mapOf(
                "totalMemory" to runtime.totalMemory(),
                "freeMemory" to runtime.freeMemory(),
                "maxMemory" to runtime.maxMemory(),
                "availableProcessors" to runtime.availableProcessors(),
                "currentTimeMillis" to System.currentTimeMillis()
            )
            
            Log.d(TAG, "System stats: $systemStats")
            ErrorLogger.logInfo(applicationContext, TAG, "logSystemStats", "System stats logged", systemStats)
            
        } catch (e: Exception) {
            Log.w(TAG, "Could not log system stats", e)
            ErrorLogger.logWarning(applicationContext, TAG, "logSystemStats", "Could not log system stats", mapOf("error" to e.message))
        }
    }
    
    /**
     * Verify Device Owner status (important for security)
     */
    private fun verifyDeviceOwnerStatus() {
        try {
            val devicePolicyManager = applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(applicationContext.packageName)
            
            if (isDeviceOwner) {
                Log.d(TAG, "Device Owner status confirmed")
                ErrorLogger.logInfo(applicationContext, TAG, "verifyDeviceOwnerStatus", "Device Owner status confirmed")
            } else {
                Log.w(TAG, "Device Owner status lost or not set")
                ErrorLogger.logWarning(applicationContext, TAG, "verifyDeviceOwnerStatus", "Device Owner status lost or not set")
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Could not verify Device Owner status", e)
            ErrorLogger.logWarning(applicationContext, TAG, "verifyDeviceOwnerStatus", "Could not verify Device Owner status", mapOf("error" to e.message))
        }
    }
}
