package com.odensecurewave

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserHandle
import android.util.Log
import kotlinx.coroutines.runBlocking

class AppDeviceAdminReceiver : DeviceAdminReceiver() {
    companion object {
        private const val TAG = "AppDeviceAdminReceiver"
        private const val PREFS_NAME = "device_admin_prefs"
        fun log(message: String) = Log.d("dpc::", message)

        private const val KEY_IS_FROM_BOOT_COMPLETED = "is_from_boot_completed"
        private const val KEY_IS_PROVISIONED = "is_provisioned"

        private fun getSharedPreferences(context: Context) = 
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun setIsFromBootCompleted(context: Context, value: Boolean) {
            try {
                val sharedPreferences = getSharedPreferences(context)
                sharedPreferences.edit().putBoolean(KEY_IS_FROM_BOOT_COMPLETED, value).apply()
                ErrorLogger.logInfo(context, TAG, "setIsFromBootCompleted", "Boot completed flag set to: $value")
            } catch (e: Exception) {
                ErrorLogger.logError(context, TAG, "setIsFromBootCompleted", e, mapOf("value" to value))
            }
        }

        fun isFromBootCompleted(context: Context): Boolean {
            return try {
                val sharedPreferences = getSharedPreferences(context)
                val result = sharedPreferences.getBoolean(KEY_IS_FROM_BOOT_COMPLETED, false)
                ErrorLogger.logInfo(context, TAG, "isFromBootCompleted", "Boot completed flag: $result")
                result
            } catch (e: Exception) {
                ErrorLogger.logError(context, TAG, "isFromBootCompleted", e)
                false
            }
        }
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        try {
            log("onProfileProvisioningComplete - Android ${Build.VERSION.SDK_INT}")
            ErrorLogger.logInfo(context, TAG, "onProfileProvisioningComplete", "Profile provisioning completed", 
                mapOf("androidVersion" to Build.VERSION.SDK_INT, "action" to intent.action))
            
            // Mark as provisioned
            getSharedPreferences(context)
                .edit().putBoolean(KEY_IS_PROVISIONED, true).apply()
            
            // Check if we became device owner
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(context.packageName)
            val isProfileOwner = devicePolicyManager.isProfileOwnerApp(context.packageName)
            
            log("Post-provisioning status: DeviceOwner=$isDeviceOwner, ProfileOwner=$isProfileOwner")
            ErrorLogger.logInfo(context, TAG, "onProfileProvisioningComplete", "Provisioning status checked", 
                mapOf(
                    "isDeviceOwner" to isDeviceOwner,
                    "isProfileOwner" to isProfileOwner,
                    "packageName" to context.packageName
                ))
            
            val i: Intent? = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(i)
                ErrorLogger.logInfo(context, TAG, "onProfileProvisioningComplete", "Successfully launched main activity")
            } else {
                log("Couldn't start activity")
                ErrorLogger.logWarning(context, TAG, "onProfileProvisioningComplete", "Could not find launch intent for package", 
                    mapOf("packageName" to context.packageName))
            }
        } catch (e: Exception) {
            ErrorLogger.logError(context, TAG, "onProfileProvisioningComplete", e, 
                mapOf("intentAction" to intent.action, "packageName" to context.packageName))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            super.onReceive(context, intent)
            val action = intent.action
            val extras = intent.extras
            log("onReceive: action: $action, extras: $extras, Android: ${Build.VERSION.SDK_INT}")
            
            ErrorLogger.logInfo(context, TAG, "onReceive", "Received broadcast", 
                mapOf(
                    "action" to action,
                    "extras" to extras?.toString(),
                    "scheme" to intent.scheme,
                    "data" to intent.dataString,
                    "androidVersion" to Build.VERSION.SDK_INT
                ))
            
            // Handle provisioning-related actions
            when (action) {
                DevicePolicyManager.ACTION_MANAGED_PROFILE_PROVISIONED,
                Intent.ACTION_MANAGED_PROFILE_ADDED,
                "android.app.action.PROVISIONING_SUCCESSFUL" -> {
                    try {
                        getSharedPreferences(context)
                            .edit().putBoolean(KEY_IS_PROVISIONED, true).apply()
                        
                        // Check device owner status after provisioning
                        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                        val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(context.packageName)
                        val isProfileOwner = devicePolicyManager.isProfileOwnerApp(context.packageName)
                        
                        log("Provisioning completed - DeviceOwner: $isDeviceOwner, ProfileOwner: $isProfileOwner")
                        ErrorLogger.logInfo(context, TAG, "onReceive", "Marked profile as provisioned for action: $action", 
                            mapOf(
                                "isDeviceOwner" to isDeviceOwner,
                                "isProfileOwner" to isProfileOwner,
                                "androidVersion" to Build.VERSION.SDK_INT
                            ))
                        
                        // For Android 12+, handle additional provisioning success
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            handleAndroid12Provisioning(context, intent)
                        }
                    } catch (e: Exception) {
                        ErrorLogger.logError(context, TAG, "onReceive-setProvisioned", e, mapOf("action" to action))
                    }
                }
                
                Intent.ACTION_BOOT_COMPLETED -> {
                    try {
                        setIsFromBootCompleted(context, true)
                        log("Boot completed - device admin receiver activated")
                        ErrorLogger.logInfo(context, TAG, "onReceive", "Boot completed - device admin receiver activated")
                        
                        // Initialize the WorkManager periodic service on boot
                        try {
                            WorkManagerServiceManager.initializeService(context)
                            log("WorkManager periodic service initialized on boot")
                            ErrorLogger.logInfo(context, TAG, "onReceive", "WorkManager periodic service initialized on boot")
                        } catch (serviceException: Exception) {
                            log("Failed to initialize WorkManager periodic service on boot: ${serviceException.message}")
                            ErrorLogger.logError(context, TAG, "onReceive-serviceInit", serviceException)
                        }
                        
                        // REQUIREMENT: Check Firebase status on device restart and launch app if needed
                        try {
                            log("=== STARTING FIREBASE STATUS CHECK ON BOOT ===")
                            ErrorLogger.logInfo(context, TAG, "onReceive", "Starting Firebase status check on boot")
                            checkFirebaseStatusOnBoot(context)
                            log("=== FIREBASE STATUS CHECK ON BOOT COMPLETED ===")
                        } catch (firebaseException: Exception) {
                            log("Failed to check Firebase status on boot: ${firebaseException.message}")
                            ErrorLogger.logError(context, TAG, "onReceive-firebaseCheck", firebaseException)
                        }
                        
                        // CRITICAL: Schedule delayed check FIRST in case we get killed by AutoStart Limit
                        try {
                            log("Scheduling delayed boot status check as primary mechanism")
                            BootStatusChecker.scheduleBootStatusCheck(context)
                            ErrorLogger.logInfo(context, TAG, "onReceive", "Delayed boot status check scheduled")
                        } catch (fallbackException: Exception) {
                            log("Failed to schedule delayed boot status check: ${fallbackException.message}")
                            ErrorLogger.logError(context, TAG, "onReceive-fallback", fallbackException)
                        }
                        
                    } catch (e: Exception) {
                        ErrorLogger.logError(context, TAG, "onReceive-bootCompleted", e)
                    }
                }
                
                else -> {
                    log("Received other action: $action")
                }
            }
        } catch (e: Exception) {
            ErrorLogger.logError(context, TAG, "onReceive", e, 
                mapOf(
                    "action" to intent.action,
                    "intentClass" to intent.javaClass.simpleName
                ))
        }
    }

    private fun handleAndroid12Provisioning(context: Context, intent: Intent) {
        try {
            log("Handling Android 12+ provisioning for action: ${intent.action}")
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            
            // Verify device owner status
            val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(context.packageName)
            if (isDeviceOwner) {
                log("Device owner successfully established on Android 12+")
                
                // Launch main activity
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(launchIntent)
                    ErrorLogger.logInfo(context, TAG, "handleAndroid12Provisioning", "Successfully launched main activity")
                } else {
                    ErrorLogger.logWarning(context, TAG, "handleAndroid12Provisioning", "Could not find launch intent")
                }
            } else {
                ErrorLogger.logWarning(context, TAG, "handleAndroid12Provisioning", "Device owner not established")
            }
        } catch (e: Exception) {
            ErrorLogger.logError(context, TAG, "handleAndroid12Provisioning", e)
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        try {
            super.onEnabled(context, intent)
            log("Device admin enabled")
            ErrorLogger.logInfo(context, TAG, "onEnabled", "Device admin was enabled", 
                mapOf(
                    "targetClass" to intent.component?.className,
                    "intentAction" to intent.action
                ))
        } catch (e: Exception) {
            ErrorLogger.logError(context, TAG, "onEnabled", e, 
                mapOf("intentAction" to intent.action))
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        try {
            super.onDisabled(context, intent)
            log("Device admin disabled")
            ErrorLogger.logInfo(context, TAG, "onDisabled", "Device admin was disabled",
                mapOf(
                    "targetClass" to intent.component?.className,
                    "intentAction" to intent.action
                ))
        } catch (e: Exception) {
            ErrorLogger.logError(context, TAG, "onDisabled", e,
                mapOf("intentAction" to intent.action))
        }
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        try {
            super.onLockTaskModeEntering(context, intent, pkg)
            log("Lock task mode entering: $pkg")
            ErrorLogger.logInfo(context, TAG, "onLockTaskModeEntering", "Lock task mode entering",
                mapOf(
                    "package" to pkg,
                    "intentAction" to intent.action
                ))
        } catch (e: Exception) {
            ErrorLogger.logError(context, TAG, "onLockTaskModeEntering", e,
                mapOf(
                    "package" to pkg,
                    "intentAction" to intent.action
                ))
        }
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        try {
            super.onLockTaskModeExiting(context, intent)
            log("Lock task mode exiting")
            ErrorLogger.logInfo(context, TAG, "onLockTaskModeExiting", "Lock task mode exiting",
                mapOf("intentAction" to intent.action))
        } catch (e: Exception) {
            ErrorLogger.logError(context, TAG, "onLockTaskModeExiting", e,
                mapOf("intentAction" to intent.action))
        }
    }

    override fun onPasswordChanged(context: Context, intent: Intent, userHandle: UserHandle) {
        try {
            super.onPasswordChanged(context, intent, userHandle)
            log("Password changed for user: $userHandle")
            ErrorLogger.logInfo(context, TAG, "onPasswordChanged", "Password changed",
                mapOf(
                    "userHandle" to userHandle.toString(),
                    "intentAction" to intent.action
                ))
        } catch (e: Exception) {
            ErrorLogger.logError(context, TAG, "onPasswordChanged", e,
                mapOf(
                    "userHandle" to userHandle.toString(),
                    "intentAction" to intent.action
                ))
        }
    }

    override fun onPasswordFailed(context: Context, intent: Intent, userHandle: UserHandle) {
        try {
            super.onPasswordFailed(context, intent, userHandle)
            log("Password failed for user: $userHandle")
            ErrorLogger.logWarning(context, TAG, "onPasswordFailed", "Password authentication failed",
                mapOf(
                    "userHandle" to userHandle.toString(),
                    "intentAction" to intent.action
                ))
        } catch (e: Exception) {
            ErrorLogger.logError(context, TAG, "onPasswordFailed", e,
                mapOf(
                    "userHandle" to userHandle.toString(),
                    "intentAction" to intent.action
                ))
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent, userHandle: UserHandle) {
        try {
            super.onPasswordSucceeded(context, intent, userHandle)
            log("Password succeeded for user: $userHandle")
            ErrorLogger.logInfo(context, TAG, "onPasswordSucceeded", "Password authentication succeeded",
                mapOf(
                    "userHandle" to userHandle.toString(),
                    "intentAction" to intent.action
                ))
        } catch (e: Exception) {
            ErrorLogger.logError(context, TAG, "onPasswordSucceeded", e,
                mapOf(
                    "userHandle" to userHandle.toString(),
                    "intentAction" to intent.action
                ))
        }
    }

    override fun onPasswordExpiring(context: Context, intent: Intent, userHandle: UserHandle) {
        try {
            super.onPasswordExpiring(context, intent, userHandle)
            log("Password expiring for user: $userHandle")
            ErrorLogger.logWarning(context, TAG, "onPasswordExpiring", "Password is expiring",
                mapOf(
                    "userHandle" to userHandle.toString(),
                    "intentAction" to intent.action
                ))
        } catch (e: Exception) {
            ErrorLogger.logError(context, TAG, "onPasswordExpiring", e,
                mapOf(
                    "userHandle" to userHandle.toString(),
                    "intentAction" to intent.action
                ))
        }
    }
    
    /**
     * Check Firebase status on boot and launch app if status is "lock"
     * This handles the requirement for device restart behavior
     */
    private fun checkFirebaseStatusOnBoot(context: Context) {
        try {
            log("Checking Firebase status on boot")
            ErrorLogger.logInfo(context, TAG, "checkFirebaseStatusOnBoot", "Starting Firebase status check on boot")
            
            // Check if Firebase is available
            if (!FirebaseDeviceStatusManager.isFirebaseAvailable()) {
                log("Firebase is not available on boot - skipping status check")
                ErrorLogger.logWarning(context, TAG, "checkFirebaseStatusOnBoot", "Firebase is not available")
                return
            }
            
            // Use coroutines to handle async Firebase operations
            runBlocking {
                try {
                    // Get device IMEI for Firebase operations
                    val deviceId = FirebaseDeviceStatusManager.getDeviceImei(context)
                    if (deviceId == null) {
                        log("Could not get device IMEI on boot - skipping status check")
                        ErrorLogger.logWarning(context, TAG, "checkFirebaseStatusOnBoot", "Could not get device IMEI")
                        return@runBlocking
                    }
                    
                    log("Fetching device status from Firebase for device: $deviceId")
                    
                    // Fetch device status from Firebase
                    val deviceStatus = FirebaseDeviceStatusManager.getDeviceStatus(context, deviceId)
                    log("Device status from Firebase on boot: $deviceStatus")
                    
                    when (deviceStatus) {
                        "lock" -> {
                            log("Device status is 'lock' on boot - launching app immediately")
                            ErrorLogger.logInfo(context, TAG, "checkFirebaseStatusOnBoot", 
                                "Device status is 'lock' on boot - launching app")
                            
                            // Launch the app immediately
                            val appLaunched = AppStateManager.launchApp(context)
                            if (appLaunched) {
                                log("App launched successfully on boot for lock status")
                                ErrorLogger.logInfo(context, TAG, "checkFirebaseStatusOnBoot", 
                                    "App launched successfully on boot for lock status")
                            } else {
                                log("Failed to launch app on boot for lock status")
                                ErrorLogger.logWarning(context, TAG, "checkFirebaseStatusOnBoot", 
                                    "Failed to launch app on boot for lock status")
                            }
                        }
                        "active" -> {
                            log("Device status is 'active' on boot - no immediate action required")
                            ErrorLogger.logInfo(context, TAG, "checkFirebaseStatusOnBoot", 
                                "Device status is 'active' on boot - no immediate action required")
                        }
                        null -> {
                            log("Could not retrieve device status from Firebase on boot")
                            ErrorLogger.logWarning(context, TAG, "checkFirebaseStatusOnBoot", 
                                "Could not retrieve device status from Firebase")
                        }
                        else -> {
                            log("Device status is '$deviceStatus' on boot - no special action defined")
                            ErrorLogger.logInfo(context, TAG, "checkFirebaseStatusOnBoot", 
                                "Device status retrieved on boot", mapOf("status" to deviceStatus))
                        }
                    }
                    
                } catch (e: Exception) {
                    log("Error in Firebase status check coroutine: ${e.message}")
                    ErrorLogger.logError(context, TAG, "checkFirebaseStatusOnBoot-coroutine", e)
                }
            }
            
        } catch (e: Exception) {
            log("Error checking Firebase status on boot: ${e.message}")
            ErrorLogger.logError(context, TAG, "checkFirebaseStatusOnBoot", e)
        }
    }
    

}
