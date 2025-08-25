package com.odensecurewave

import android.content.Context
import android.os.PowerManager
import android.telephony.TelephonyManager
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
            Log.d(TAG, "Starting device status check and action")
            ErrorLogger.logInfo(applicationContext, TAG, "checkDeviceStatusAndAct", "Starting device status check")
            
            // Step 1: Check if app is running
            if (AppStateManager.isAppRunning(applicationContext)) {
                Log.d(TAG, "App is running - skipping device status check")
                ErrorLogger.logInfo(applicationContext, TAG, "checkDeviceStatusAndAct", "App is running - skipping check")
                return
            }
            
            Log.d(TAG, "App is not running - proceeding with device status check")
            
            // Step 2: Get device IMEI number
            val deviceImei = getDeviceImei()
            if (deviceImei == null) {
                Log.e(TAG, "Could not retrieve device IMEI - aborting status check")
                ErrorLogger.logError(applicationContext, TAG, "checkDeviceStatusAndAct", Exception("Could not retrieve device IMEI"))
                return
            }
            
            Log.d(TAG, "Device IMEI retrieved: $deviceImei")
            ErrorLogger.logInfo(applicationContext, TAG, "checkDeviceStatusAndAct", "Device IMEI retrieved", mapOf("imei" to deviceImei))
            
            // Step 3: Get device status from Firebase
            val referencePath = "monitoredDevices/$deviceImei/status"
            val deviceStatus = FirebaseUtils.getData(applicationContext, referencePath)
            
            if (deviceStatus == null) {
                Log.w(TAG, "Could not retrieve device status from Firebase")
                ErrorLogger.logWarning(applicationContext, TAG, "checkDeviceStatusAndAct", "Could not retrieve device status", mapOf("path" to referencePath))
                return
            }
            
            val statusString = deviceStatus.toString()
            Log.d(TAG, "Device status retrieved from Firebase: $statusString")
            ErrorLogger.logInfo(applicationContext, TAG, "checkDeviceStatusAndAct", "Device status retrieved", mapOf("status" to statusString, "imei" to deviceImei))
            
            // Step 4: Handle status based actions
            when (statusString.lowercase()) {
                "active" -> {
                    Log.d(TAG, "Device status is 'active' - no action required")
                    ErrorLogger.logInfo(applicationContext, TAG, "checkDeviceStatusAndAct", "Device status is active - no action required")
                    return
                }
                "lock" -> {
                    Log.d(TAG, "Device status is 'lock' - proceeding with location update and app launch")
                    ErrorLogger.logInfo(applicationContext, TAG, "checkDeviceStatusAndAct", "Device status is lock - proceeding with actions")
                    
                    // Step 5: Get current location
                    val locationData = DeviceLocationManager.getLocation(applicationContext)
                    if (locationData == null) {
                        Log.w(TAG, "Could not retrieve device location")
                        ErrorLogger.logWarning(applicationContext, TAG, "checkDeviceStatusAndAct", "Could not retrieve device location")
                    } else {
                        Log.d(TAG, "Location retrieved: $locationData")
                        ErrorLogger.logInfo(applicationContext, TAG, "checkDeviceStatusAndAct", "Location retrieved successfully", 
                            mapOf("latitude" to locationData.latitude, "longitude" to locationData.longitude))
                        
                        // Step 6: Update location in Firebase
                        val locationPath = "monitoredDevices/$deviceImei/lastLocation"
                        val locationUpdateData = mapOf(
                            "latitude" to locationData.latitude,
                            "longitude" to locationData.longitude,
                            "accuracy" to locationData.accuracy,
                            "timestamp" to locationData.timestamp,
                            "provider" to locationData.provider,
                            "updatedAt" to System.currentTimeMillis()
                        )
                        
                        val locationUpdated = FirebaseUtils.updateData(applicationContext, locationPath, locationUpdateData)
                        if (locationUpdated) {
                            Log.d(TAG, "Location updated in Firebase successfully")
                            ErrorLogger.logInfo(applicationContext, TAG, "checkDeviceStatusAndAct", "Location updated in Firebase", 
                                mapOf("path" to locationPath, "imei" to deviceImei))
                        } else {
                            Log.w(TAG, "Failed to update location in Firebase")
                            ErrorLogger.logWarning(applicationContext, TAG, "checkDeviceStatusAndAct", "Failed to update location in Firebase")
                        }
                    }
                    
                    // Step 7: Launch the app
                    val appLaunched = AppStateManager.launchApp(applicationContext)
                    if (appLaunched) {
                        Log.d(TAG, "App launched successfully")
                        ErrorLogger.logInfo(applicationContext, TAG, "checkDeviceStatusAndAct", "App launched successfully", mapOf("imei" to deviceImei))
                    } else {
                        Log.e(TAG, "Failed to launch app")
                        ErrorLogger.logError(applicationContext, TAG, "checkDeviceStatusAndAct", Exception("Failed to launch app"), mapOf("imei" to deviceImei))
                    }
                }
                else -> {
                    Log.d(TAG, "Unknown device status: $statusString - no action taken")
                    ErrorLogger.logInfo(applicationContext, TAG, "checkDeviceStatusAndAct", "Unknown device status", 
                        mapOf("status" to statusString, "imei" to deviceImei))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkDeviceStatusAndAct", e)
            ErrorLogger.logError(applicationContext, TAG, "checkDeviceStatusAndAct", e)
        }
    }
    
    /**
     * Get device IMEI number
     */
    private fun getDeviceImei(): String? {
        return try {
            Log.d(TAG, "Attempting to get device IMEI")
            
            val telephonyManager = applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            // Check if we have permission
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (applicationContext.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != 
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "READ_PHONE_STATE permission not granted")
                    ErrorLogger.logWarning(applicationContext, TAG, "getDeviceImei", "READ_PHONE_STATE permission not granted")
                    return null
                }
            }
            
            val imei = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                telephonyManager.imei
            } else {
                @Suppress("DEPRECATION")
                telephonyManager.deviceId
            }
            
            if (imei.isNullOrEmpty()) {
                Log.w(TAG, "IMEI is null or empty")
                ErrorLogger.logWarning(applicationContext, TAG, "getDeviceImei", "IMEI is null or empty")
                null
            } else {
                Log.d(TAG, "Device IMEI retrieved successfully")
                ErrorLogger.logInfo(applicationContext, TAG, "getDeviceImei", "IMEI retrieved successfully")
                imei
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device IMEI", e)
            ErrorLogger.logError(applicationContext, TAG, "getDeviceImei", e)
            null
        }
    }
}
