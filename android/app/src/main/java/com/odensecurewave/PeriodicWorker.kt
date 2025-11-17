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
            
            // Execute the periodic work flow
            runBlocking {
                executePeriodicWorkFlow()
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
     * Execute the periodic work flow according to requirements:
     * 1. Check if device record exists in Firebase - if not, do nothing
     * 2. Update device location to Firebase
     * 3. Check if app is running - if yes, do nothing
     * 4. If app not running, check device status and launch app if status is 'lock'
     */
    private suspend fun executePeriodicWorkFlow() {
        try {
            Log.d(TAG, "Starting periodic work flow")
            ErrorLogger.logInfo(applicationContext, TAG, "executePeriodicWorkFlow", "Starting periodic work flow")
            
            // Step 1: Get device IMEI number (needed for Firebase operations)
            val deviceImei = getDeviceImei()
            if (deviceImei == null) {
                Log.e(TAG, "Could not retrieve device IMEI - aborting periodic work")
                ErrorLogger.logError(applicationContext, TAG, "executePeriodicWorkFlow", Exception("Could not retrieve device IMEI"))
                return
            }
            
            Log.d(TAG, "Device IMEI retrieved: $deviceImei")
            
            // Step 2: Check if device record exists in Firebase
            val devicePath = "monitoredDevices/$deviceImei"
            val recordExists = FirebaseUtils.dataExists(applicationContext, devicePath)
            
            if (!recordExists) {
                Log.d(TAG, "Device record not found in Firebase - skipping periodic work")
                ErrorLogger.logInfo(applicationContext, TAG, "executePeriodicWorkFlow", 
                    "Device record not found in Firebase - skipping periodic work", 
                    mapOf("imei" to deviceImei, "path" to devicePath))
                return
            }
            
            Log.d(TAG, "Device record found in Firebase - proceeding with periodic work")
            ErrorLogger.logInfo(applicationContext, TAG, "executePeriodicWorkFlow", 
                "Device record found in Firebase", mapOf("imei" to deviceImei))
            
            // Step 3: Always update device location to Firebase
            updateDeviceLocation(deviceImei)
            
            // Step 4: Check if app is running
            if (AppStateManager.isAppRunning(applicationContext)) {
                Log.d(TAG, "App is currently running - no further action required")
                ErrorLogger.logInfo(applicationContext, TAG, "executePeriodicWorkFlow", "App is running - no further action")
                return
            }
            
            Log.d(TAG, "App is not running - checking device status")
            
            // Step 5: Get device status from Firebase
            val referencePath = "monitoredDevices/$deviceImei/status"
            val deviceStatus = FirebaseUtils.getData(applicationContext, referencePath)
            
            if (deviceStatus == null) {
                Log.w(TAG, "Could not retrieve device status from Firebase")
                ErrorLogger.logWarning(applicationContext, TAG, "executePeriodicWorkFlow", "Could not retrieve device status", mapOf("path" to referencePath))
                return
            }
            
            val statusString = deviceStatus.toString()
            Log.d(TAG, "Device status retrieved: $statusString")
            ErrorLogger.logInfo(applicationContext, TAG, "executePeriodicWorkFlow", "Device status retrieved", mapOf("status" to statusString, "imei" to deviceImei))
            
            // Step 6: Launch app if status is 'lock'
            if (statusString.lowercase() == "lock") {
                Log.d(TAG, "Device status is 'lock' - launching app")
                ErrorLogger.logInfo(applicationContext, TAG, "executePeriodicWorkFlow", "Device status is lock - launching app")
                
                val appLaunched = AppStateManager.launchApp(applicationContext)
                if (appLaunched) {
                    Log.d(TAG, "App launched successfully")
                    ErrorLogger.logInfo(applicationContext, TAG, "executePeriodicWorkFlow", "App launched successfully", mapOf("imei" to deviceImei))
                } else {
                    Log.e(TAG, "Failed to launch app")
                    ErrorLogger.logError(applicationContext, TAG, "executePeriodicWorkFlow", Exception("Failed to launch app"), mapOf("imei" to deviceImei))
                }
            } else {
                Log.d(TAG, "Device status is '$statusString' - no app launch required")
                ErrorLogger.logInfo(applicationContext, TAG, "executePeriodicWorkFlow", "No app launch required", mapOf("status" to statusString))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in executePeriodicWorkFlow", e)
            ErrorLogger.logError(applicationContext, TAG, "executePeriodicWorkFlow", e)
        }
    }
    
    /**
     * Update device location to Firebase
     */
    private suspend fun updateDeviceLocation(deviceImei: String) {
        try {
            Log.d(TAG, "Updating device location")
            
            val locationData = DeviceLocationManager.getLocation(applicationContext)
            if (locationData == null) {
                Log.w(TAG, "Could not retrieve device location")
                ErrorLogger.logWarning(applicationContext, TAG, "updateDeviceLocation", "Could not retrieve device location")
                return
            }
            
            Log.d(TAG, "Location retrieved successfully")
            ErrorLogger.logInfo(applicationContext, TAG, "updateDeviceLocation", "Location retrieved successfully", 
                mapOf("latitude" to locationData.latitude, "longitude" to locationData.longitude))
            
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
                ErrorLogger.logInfo(applicationContext, TAG, "updateDeviceLocation", "Location updated in Firebase", 
                    mapOf("path" to locationPath, "imei" to deviceImei))
            } else {
                Log.w(TAG, "Failed to update location in Firebase")
                ErrorLogger.logWarning(applicationContext, TAG, "updateDeviceLocation", "Failed to update location in Firebase")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating device location", e)
            ErrorLogger.logError(applicationContext, TAG, "updateDeviceLocation", e)
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
