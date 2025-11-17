package com.odensecurewave

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BootStatusChecker - Alternative approach to check Firebase status when boot receiver is blocked
 * Uses AlarmManager to schedule a check shortly after boot when the system is more ready
 */
object BootStatusChecker {
    private const val TAG = "BootStatusChecker"
    private const val BOOT_CHECK_ACTION = "com.odensecurewave.BOOT_STATUS_CHECK"
    private const val BOOT_CHECK_DELAY_MS = 60_000L // 1 minute after boot
    
    /**
     * Schedule a delayed Firebase status check after boot
     * This bypasses AutoStart Limit by using AlarmManager
     */
    fun scheduleBootStatusCheck(context: Context) {
        try {
            Log.d(TAG, "Scheduling delayed boot status check")
            ErrorLogger.logInfo(context, TAG, "scheduleBootStatusCheck", "Scheduling delayed boot status check")
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, BootStatusReceiver::class.java).apply {
                action = BOOT_CHECK_ACTION
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
            
            // Schedule the alarm to fire 1 minute after boot
            val triggerTime = SystemClock.elapsedRealtime() + BOOT_CHECK_DELAY_MS
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent)
            }
            
            Log.d(TAG, "Boot status check scheduled for ${BOOT_CHECK_DELAY_MS / 1000} seconds after boot")
            ErrorLogger.logInfo(context, TAG, "scheduleBootStatusCheck", "Boot status check scheduled successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling boot status check", e)
            ErrorLogger.logError(context, TAG, "scheduleBootStatusCheck", e)
        }
    }
}

/**
 * BootStatusReceiver - Receives the delayed alarm and performs Firebase status check
 */
class BootStatusReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootStatusReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        try {
            Log.d(TAG, "BootStatusReceiver triggered - checking Firebase status")
            ErrorLogger.logInfo(context, TAG, "onReceive", "Delayed boot status check triggered")
            
            if (intent.action == "com.odensecurewave.BOOT_STATUS_CHECK") {
                // Perform the Firebase status check in a coroutine
                CoroutineScope(Dispatchers.IO).launch {
                    performDelayedBootCheck(context)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in BootStatusReceiver", e)
            ErrorLogger.logError(context, TAG, "onReceive", e)
        }
    }
    
    private suspend fun performDelayedBootCheck(context: Context) {
        // try {
        //     Log.d(TAG, "Performing delayed boot Firebase status check")
        //     ErrorLogger.logInfo(context, TAG, "performDelayedBootCheck", "Starting delayed Firebase status check")
            
        //     // Check if Firebase is available
        //     if (!FirebaseDeviceStatusManager.isFirebaseAvailable()) {
        //         Log.w(TAG, "Firebase is not available for delayed boot check")
        //         ErrorLogger.logWarning(context, TAG, "performDelayedBootCheck", "Firebase is not available")
        //         return
        //     }
            
        //     // Get device IMEI
        //     val deviceId = FirebaseDeviceStatusManager.getDeviceImei(context)
        //     if (deviceId == null) {
        //         Log.w(TAG, "Could not get device IMEI for delayed boot check")
        //         ErrorLogger.logWarning(context, TAG, "performDelayedBootCheck", "Could not get device IMEI")
        //         return
        //     }
            
        //     Log.d(TAG, "Fetching device status from Firebase for delayed boot check")
            
        //     // Fetch device status from Firebase
        //     val deviceStatus = FirebaseDeviceStatusManager.getDeviceStatus(context, deviceId)
        //     Log.d(TAG, "Device status from Firebase in delayed boot check: $deviceStatus")
            
        //     when (deviceStatus) {
        //         "lock" -> {
        //             Log.d(TAG, "Device status is 'lock' in delayed boot check - launching app")
        //             ErrorLogger.logInfo(context, TAG, "performDelayedBootCheck", 
        //                 "Device status is 'lock' - launching app")
                    
        //             // Launch the app
        //             val appLaunched = AppStateManager.launchApp(context)
        //             if (appLaunched) {
        //                 Log.d(TAG, "App launched successfully in delayed boot check")
        //                 ErrorLogger.logInfo(context, TAG, "performDelayedBootCheck", 
        //                     "App launched successfully for lock status")
        //             } else {
        //                 Log.w(TAG, "Failed to launch app in delayed boot check")
        //                 ErrorLogger.logWarning(context, TAG, "performDelayedBootCheck", 
        //                     "Failed to launch app for lock status")
        //             }
        //         }
        //         "active" -> {
        //             Log.d(TAG, "Device status is 'active' in delayed boot check - no action needed")
        //             ErrorLogger.logInfo(context, TAG, "performDelayedBootCheck", 
        //                 "Device status is 'active' - no action needed")
        //         }
        //         null -> {
        //             Log.w(TAG, "Could not retrieve device status from Firebase in delayed boot check")
        //             ErrorLogger.logWarning(context, TAG, "performDelayedBootCheck", 
        //                 "Could not retrieve device status from Firebase")
        //         }
        //         else -> {
        //             Log.d(TAG, "Device status is '$deviceStatus' in delayed boot check - no special action")
        //             ErrorLogger.logInfo(context, TAG, "performDelayedBootCheck", 
        //                 "Device status retrieved", mapOf("status" to deviceStatus))
        //         }
        //     }
            
        // } catch (e: Exception) {
        //     Log.e(TAG, "Error in delayed boot Firebase check", e)
        //     ErrorLogger.logError(context, TAG, "performDelayedBootCheck", e)
        // }
    }
}
