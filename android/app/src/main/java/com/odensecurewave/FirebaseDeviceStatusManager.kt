package com.odensecurewave

import android.content.Context
import android.util.Log
import com.google.firebase.database.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * FirebaseDeviceStatusManager - Handles Firebase operations for device status and location
 * Specifically designed for periodic worker background operations
 */
object FirebaseDeviceStatusManager {
    private const val TAG = "FirebaseDeviceStatusManager"
    private const val TIMEOUT_MS = 15_000L // 15 seconds timeout
    
    /**
     * Get device status from Firebase
     * Returns the status string or null if not found/error
     */
    suspend fun getDeviceStatus(context: Context, deviceId: String): String? {
        return try {
            Log.d(TAG, "Fetching device status for device: $deviceId")
            ErrorLogger.logInfo(context, TAG, "getDeviceStatus", "Fetching device status", 
                mapOf("deviceId" to deviceId))
            
            val database = FirebaseDatabase.getInstance()
            val reference = database.getReference("monitoredDevices").child(deviceId).child("status")
            
            suspendCoroutine { continuation ->
                val timeoutRunnable = Runnable {
                    Log.w(TAG, "Firebase read timeout for device status")
                    continuation.resumeWithException(Exception("Timeout reading device status"))
                }
                
                // Set timeout
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                handler.postDelayed(timeoutRunnable, TIMEOUT_MS)
                
                reference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        handler.removeCallbacks(timeoutRunnable)
                        try {
                            val status = snapshot.getValue(String::class.java)
                            Log.d(TAG, "Device status retrieved: $status")
                            ErrorLogger.logInfo(context, TAG, "getDeviceStatus", "Device status retrieved", 
                                mapOf("deviceId" to deviceId, "status" to status))
                            continuation.resume(status)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing device status", e)
                            ErrorLogger.logError(context, TAG, "getDeviceStatus-parse", e)
                            continuation.resume(null)
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        handler.removeCallbacks(timeoutRunnable)
                        Log.e(TAG, "Firebase read cancelled: ${error.message}")
                        ErrorLogger.logError(context, TAG, "getDeviceStatus-cancelled", 
                            Exception(error.message), mapOf("code" to error.code))
                        continuation.resume(null)
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device status", e)
            ErrorLogger.logError(context, TAG, "getDeviceStatus", e, mapOf("deviceId" to deviceId))
            null
        }
    }
    
    /**
     * Update device location in Firebase
     * Returns true if successful, false otherwise
     */
    suspend fun updateDeviceLocation(context: Context, deviceId: String, locationData: LocationData): Boolean {
        return try {
            Log.d(TAG, "Updating device location for device: $deviceId")
            ErrorLogger.logInfo(context, TAG, "updateDeviceLocation", "Updating device location", 
                mapOf("deviceId" to deviceId, "location" to locationData.toString()))
            
            val database = FirebaseDatabase.getInstance()
            val reference = database.getReference("monitoredDevices").child(deviceId).child("lastLocation")
            
            suspendCoroutine { continuation ->
                val timeoutRunnable = Runnable {
                    Log.w(TAG, "Firebase write timeout for device location")
                    continuation.resume(false)
                }
                
                // Set timeout
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                handler.postDelayed(timeoutRunnable, TIMEOUT_MS)
                
                reference.setValue(locationData.toMap()).addOnCompleteListener { task ->
                    handler.removeCallbacks(timeoutRunnable)
                    if (task.isSuccessful) {
                        Log.d(TAG, "Device location updated successfully")
                        ErrorLogger.logInfo(context, TAG, "updateDeviceLocation", "Device location updated successfully", 
                            mapOf("deviceId" to deviceId))
                        continuation.resume(true)
                    } else {
                        Log.e(TAG, "Failed to update device location", task.exception)
                        ErrorLogger.logError(context, TAG, "updateDeviceLocation", 
                            task.exception ?: Exception("Unknown Firebase error"), mapOf("deviceId" to deviceId))
                        continuation.resume(false)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating device location", e)
            ErrorLogger.logError(context, TAG, "updateDeviceLocation", e, 
                mapOf("deviceId" to deviceId, "location" to locationData.toString()))
            false
        }
    }
    
    /**
     * Get device IMEI for Firebase operations
     * Returns null if unable to get IMEI
     */
    suspend fun getDeviceImei(context: Context): String? {
        return try {
            Log.d(TAG, "Retrieving device IMEI")
            ErrorLogger.logInfo(context, TAG, "getDeviceImei", "Starting IMEI retrieval")
            
            // Use the existing IMEI retrieval logic from DeviceAdminModule
            val telephonyManager = context.getSystemService(android.content.Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
            
            // Check if we have permission (Device Owner should have this)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != 
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "READ_PHONE_STATE permission not granted")
                    ErrorLogger.logWarning(context, TAG, "getDeviceImei", "READ_PHONE_STATE permission not granted")
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
                Log.w(TAG, "Unable to get device IMEI - returned null or empty")
                ErrorLogger.logWarning(context, TAG, "getDeviceImei", "IMEI is null or empty")
                null
            } else {
                Log.d(TAG, "Device IMEI retrieved successfully")
                ErrorLogger.logInfo(context, TAG, "getDeviceImei", "IMEI retrieved successfully")
                imei
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device IMEI", e)
            ErrorLogger.logError(context, TAG, "getDeviceImei", e)
            null
        }
    }
    
    /**
     * Check if Firebase is available and connected
     */
    fun isFirebaseAvailable(): Boolean {
        return try {
            FirebaseDatabase.getInstance() != null
        } catch (e: Exception) {
            Log.w(TAG, "Firebase not available", e)
            false
        }
    }
}
