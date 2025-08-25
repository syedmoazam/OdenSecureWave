package com.odensecurewave

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Process
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeviceAdminModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private val devicePolicyManager = reactContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponentName = ComponentName(reactContext, AppDeviceAdminReceiver::class.java)

    override fun getName(): String {
        return "DeviceAdminModule"
    }

    @ReactMethod
    fun testBridge(promise: Promise) {
        try {
            Log.d("DeviceAdminModule", "testBridge called successfully")
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "testBridge", "Bridge test successful")
            promise.resolve("Bridge is working!")
        } catch (e: Exception) {
            Log.e("DeviceAdminModule", "testBridge failed", e)
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "testBridge", e)
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun isDeviceAdminEnabled(promise: Promise) {
        try {
            val isEnabled = devicePolicyManager.isAdminActive(adminComponentName)
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "isDeviceAdminEnabled", "Admin status checked", mapOf("isEnabled" to isEnabled))
            promise.resolve(isEnabled)
        } catch (e: Exception) {
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "isDeviceAdminEnabled", e)
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun enableDeviceAdmin(promise: Promise) {
        try {
            Log.d("DeviceAdminModule", "enableDeviceAdmin called")
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "enableDeviceAdmin", "Enable device admin requested")
            
            // Check if already enabled
            if (devicePolicyManager.isAdminActive(adminComponentName)) {
                Log.d("DeviceAdminModule", "Device admin is already enabled")
                ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "enableDeviceAdmin", "Device admin already enabled")
                promise.resolve("Device admin is already enabled")
                return
            }

            Log.d("DeviceAdminModule", "Creating device admin intent")
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable device admin to manage security policies")
            
            // Try to get current activity first, fall back to application context with NEW_TASK
            val currentActivity = reactApplicationContext.currentActivity
            if (currentActivity != null) {
                Log.d("DeviceAdminModule", "Starting activity from current activity context")
                ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "enableDeviceAdmin", "Starting intent from current activity")
                currentActivity.startActivity(intent)
            } else {
                Log.d("DeviceAdminModule", "No current activity, starting with NEW_TASK flag")
                ErrorLogger.logWarning(reactApplicationContext, "DeviceAdminModule", "enableDeviceAdmin", "No current activity, using NEW_TASK flag")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                reactApplicationContext.startActivity(intent)
            }
            
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "enableDeviceAdmin", "Device admin intent started successfully")
            promise.resolve("Device admin request started")
        } catch (e: Exception) {
            Log.e("DeviceAdminModule", "Error enabling device admin", e)
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "enableDeviceAdmin", e, mapOf("hasCurrentActivity" to (reactApplicationContext.currentActivity != null)))
            promise.reject("ERROR", "Failed to start device admin request: ${e.message}")
        }
    }

    @ReactMethod
    fun lockDevice(promise: Promise) {
        try {
            if (devicePolicyManager.isAdminActive(adminComponentName)) {
                devicePolicyManager.lockNow()
                promise.resolve("Device locked successfully")
            } else {
                promise.reject("ERROR", "Device admin not enabled")
            }
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun wipeDevice(confirm: Boolean, promise: Promise) {
        try {
            if (devicePolicyManager.isAdminActive(adminComponentName) && confirm) {
                devicePolicyManager.wipeData(0)
                promise.resolve("Device wipe initiated")
            } else {
                promise.reject("ERROR", "Device admin not enabled or confirmation not provided")
            }
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun setPasswordPolicy(minLength: Int, promise: Promise) {
        try {
            if (devicePolicyManager.isAdminActive(adminComponentName)) {
                devicePolicyManager.setPasswordMinimumLength(adminComponentName, minLength)
                promise.resolve("Password policy set successfully")
            } else {
                promise.reject("ERROR", "Device admin not enabled")
            }
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun checkBootCompletedStatus(promise: Promise) {
        try {
            val isFromBoot = AppDeviceAdminReceiver.isFromBootCompleted(reactApplicationContext)
            promise.resolve(isFromBoot)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun setBootCompletedStatus(value: Boolean, promise: Promise) {
        try {
            AppDeviceAdminReceiver.setIsFromBootCompleted(reactApplicationContext, value)
            promise.resolve("Boot completed status set successfully")
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun setCameraDisabled(disabled: Boolean, promise: Promise) {
        try {
            Log.d("DeviceAdminModule", "setCameraDisabled called with: $disabled")
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "setCameraDisabled", "Camera control requested", mapOf("disabled" to disabled))
            
            if (!devicePolicyManager.isAdminActive(adminComponentName)) {
                Log.e("DeviceAdminModule", "Device admin not enabled - cannot control camera")
                ErrorLogger.logWarning(reactApplicationContext, "DeviceAdminModule", "setCameraDisabled", "Device admin not enabled")
                promise.reject("ERROR", "Device admin not enabled")
                return
            }
            
            Log.d("DeviceAdminModule", "Device admin is active, checking permissions...")
            
            // Check if we have device owner or profile owner privileges
            val isDeviceOwner = try {
                devicePolicyManager.isDeviceOwnerApp(reactApplicationContext.packageName)
            } catch (e: Exception) {
                ErrorLogger.logWarning(reactApplicationContext, "DeviceAdminModule", "setCameraDisabled", "Failed to check device owner status", mapOf("error" to e.message))
                false
            }
            
            val isProfileOwner = try {
                devicePolicyManager.isProfileOwnerApp(reactApplicationContext.packageName)
            } catch (e: Exception) {
                ErrorLogger.logWarning(reactApplicationContext, "DeviceAdminModule", "setCameraDisabled", "Failed to check profile owner status", mapOf("error" to e.message))
                false
            }
            
            Log.d("DeviceAdminModule", "Device owner: $isDeviceOwner, Profile owner: $isProfileOwner")
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "setCameraDisabled", "Privilege check completed", mapOf("isDeviceOwner" to isDeviceOwner, "isProfileOwner" to isProfileOwner))
            
            if (!isDeviceOwner && !isProfileOwner) {
                Log.w("DeviceAdminModule", "App is not device owner or profile owner - camera control may not work")
                ErrorLogger.logWarning(reactApplicationContext, "DeviceAdminModule", "setCameraDisabled", "Insufficient privileges for camera control")
                promise.reject("PERMISSION_ERROR", "App needs to be Device Owner or Profile Owner to control camera. Current privileges are insufficient.")
                return
            }
            
            // Check if we have the required permission
            try {
                // Get current status before change
                val currentStatus = devicePolicyManager.getCameraDisabled(adminComponentName)
                Log.d("DeviceAdminModule", "Current camera disabled status: $currentStatus")
                
                // Apply the change
                devicePolicyManager.setCameraDisabled(adminComponentName, disabled)
                
                // Verify the change
                val newStatus = devicePolicyManager.getCameraDisabled(adminComponentName)
                Log.d("DeviceAdminModule", "New camera disabled status after change: $newStatus")
                
                val status = if (disabled) "Camera disabled successfully" else "Camera enabled successfully"
                Log.d("DeviceAdminModule", status)
                ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "setCameraDisabled", status, mapOf("previousStatus" to currentStatus, "newStatus" to newStatus))
                promise.resolve(status)
                
            } catch (securityException: SecurityException) {
                Log.e("DeviceAdminModule", "Security exception when controlling camera", securityException)
                ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "setCameraDisabled", securityException, mapOf("operation" to "setCameraDisabled", "disabled" to disabled))
                promise.reject("PERMISSION_ERROR", "Insufficient permissions to control camera: ${securityException.message}")
            }
            
        } catch (e: Exception) {
            Log.e("DeviceAdminModule", "Error setting camera disabled", e)
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "setCameraDisabled", e, mapOf("disabled" to disabled))
            promise.reject("ERROR", "Failed to control camera: ${e.message}")
        }
    }

    @ReactMethod
    fun isCameraDisabled(promise: Promise) {
        try {
            Log.d("DeviceAdminModule", "isCameraDisabled called")
            
            if (devicePolicyManager.isAdminActive(adminComponentName)) {
                val isDisabled = devicePolicyManager.getCameraDisabled(adminComponentName)
                Log.d("DeviceAdminModule", "Camera disabled status: $isDisabled")
                promise.resolve(isDisabled)
            } else {
                Log.e("DeviceAdminModule", "Device admin not enabled - cannot check camera status")
                promise.reject("ERROR", "Device admin not enabled")
            }
        } catch (e: Exception) {
            Log.e("DeviceAdminModule", "Error checking camera disabled status", e)
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun setKeyguardDisabledFeatures(features: Int, promise: Promise) {
        try {
            if (devicePolicyManager.isAdminActive(adminComponentName)) {
                devicePolicyManager.setKeyguardDisabledFeatures(adminComponentName, features)
                promise.resolve("Keyguard features disabled successfully")
            } else {
                promise.reject("ERROR", "Device admin not enabled")
            }
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun getKeyguardDisabledFeatures(promise: Promise) {
        try {
            if (devicePolicyManager.isAdminActive(adminComponentName)) {
                val features = devicePolicyManager.getKeyguardDisabledFeatures(adminComponentName)
                promise.resolve(features)
            } else {
                promise.reject("ERROR", "Device admin not enabled")
            }
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun isDeviceOwner(promise: Promise) {
        try {
            val isOwner = devicePolicyManager.isDeviceOwnerApp(reactApplicationContext.packageName)
            Log.d("DeviceAdminModule", "Is device owner: $isOwner")
            promise.resolve(isOwner)
        } catch (e: Exception) {
            Log.e("DeviceAdminModule", "Error checking device owner status", e)
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun isProfileOwner(promise: Promise) {
        try {
            val isOwner = devicePolicyManager.isProfileOwnerApp(reactApplicationContext.packageName)
            Log.d("DeviceAdminModule", "Is profile owner: $isOwner")
            promise.resolve(isOwner)
        } catch (e: Exception) {
            Log.e("DeviceAdminModule", "Error checking profile owner status", e)
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun disableDeviceAdmin(promise: Promise) {
        try {
            Log.d("DeviceAdminModule", "disableDeviceAdmin called")
            
            if (!devicePolicyManager.isAdminActive(adminComponentName)) {
                Log.d("DeviceAdminModule", "Device admin is already disabled")
                promise.resolve("Device admin is already disabled")
                return
            }

            Log.d("DeviceAdminModule", "Attempting to disable device admin")
            
            try {
                // Try to remove device admin - this might fail if we don't have permission
                devicePolicyManager.removeActiveAdmin(adminComponentName)
                
                // Give it a moment to process
                Thread.sleep(500)
                
                // Verify it's disabled
                val isStillActive = devicePolicyManager.isAdminActive(adminComponentName)
                if (!isStillActive) {
                    Log.d("DeviceAdminModule", "Device admin disabled successfully")
                    promise.resolve("Device admin disabled successfully")
                } else {
                    Log.w("DeviceAdminModule", "Device admin is still active after removal attempt")
                    ErrorLogger.logWarning(reactApplicationContext, "DeviceAdminModule", "disableDeviceAdmin", "Device admin still active after removal attempt")
                    promise.reject("MANUAL_DISABLE_REQUIRED", "Device Admin cannot be disabled programmatically for security reasons. Please disable manually:\n\n1. Go to Settings → Security\n2. Find 'Device admin apps'\n3. Select 'OdenSecureWave'\n4. Tap 'Deactivate'\n\nThis is an Android security feature to prevent malicious apps from removing security controls.")
                }
                
            } catch (securityException: SecurityException) {
                Log.e("DeviceAdminModule", "Security exception when disabling device admin", securityException)
                ErrorLogger.logWarning(reactApplicationContext, "DeviceAdminModule", "disableDeviceAdmin", "Security restriction: programmatic disable not allowed", mapOf("exception" to securityException.message))
                promise.reject("MANUAL_DISABLE_REQUIRED", "For security reasons, Device Admin must be disabled manually. Go to: Settings → Security → Device admin apps → OdenSecureWave → Deactivate")
            }
            
        } catch (e: Exception) {
            Log.e("DeviceAdminModule", "Error disabling device admin", e)
            promise.reject("ERROR", "Cannot disable device admin: ${e.message}")
        }
    }

    @ReactMethod
    fun getErrorLogs(promise: Promise) {
        try {
            val logContent = ErrorLogger.getLogContent(reactApplicationContext)
            promise.resolve(logContent)
        } catch (e: Exception) {
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "getErrorLogs", e)
            promise.reject("ERROR", "Failed to get error logs: ${e.message}")
        }
    }

    @ReactMethod
    fun clearErrorLogs(promise: Promise) {
        try {
            val success = ErrorLogger.clearLogs(reactApplicationContext)
            if (success) {
                promise.resolve("Error logs cleared successfully")
            } else {
                promise.reject("ERROR", "Failed to clear error logs")
            }
        } catch (e: Exception) {
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "clearErrorLogs", e)
            promise.reject("ERROR", "Failed to clear error logs: ${e.message}")
        }
    }

    @ReactMethod
    fun getPrivilegeStatus(promise: Promise) {
        try {
            val isDeviceAdminActive = devicePolicyManager.isAdminActive(adminComponentName)
            
            val isDeviceOwner = try {
                devicePolicyManager.isDeviceOwnerApp(reactApplicationContext.packageName)
            } catch (e: Exception) {
                ErrorLogger.logWarning(reactApplicationContext, "DeviceAdminModule", "getPrivilegeStatus", "Failed to check device owner status", mapOf("error" to e.message))
                false
            }
            
            val isProfileOwner = try {
                devicePolicyManager.isProfileOwnerApp(reactApplicationContext.packageName)
            } catch (e: Exception) {
                ErrorLogger.logWarning(reactApplicationContext, "DeviceAdminModule", "getPrivilegeStatus", "Failed to check profile owner status", mapOf("error" to e.message))
                false
            }
            
            val canControlCamera = isDeviceOwner || isProfileOwner
            
            val status = WritableNativeMap().apply {
                putBoolean("isDeviceAdminActive", isDeviceAdminActive)
                putBoolean("isDeviceOwner", isDeviceOwner)
                putBoolean("isProfileOwner", isProfileOwner)
                putBoolean("canControlCamera", canControlCamera)
                putString("recommendation", if (canControlCamera) {
                    "Your app has sufficient privileges for camera control"
                } else if (isDeviceAdminActive) {
                    "Device Admin is active but you need Device Owner or Profile Owner privileges for camera control. Run: ./setup-device-owner.sh"
                } else {
                    "Please enable Device Admin first, then set up Device Owner privileges"
                })
            }
            
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "getPrivilegeStatus", "Privilege status checked", mapOf(
                "isDeviceAdminActive" to isDeviceAdminActive,
                "isDeviceOwner" to isDeviceOwner,
                "isProfileOwner" to isProfileOwner,
                "canControlCamera" to canControlCamera
            ))
            
            promise.resolve(status)
        } catch (e: Exception) {
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "getPrivilegeStatus", e)
            promise.reject("ERROR", "Failed to check privilege status: ${e.message}")
        }
    }

    @ReactMethod
    fun canDisableDeviceAdmin(promise: Promise) {
        try {
            val isDeviceAdminActive = devicePolicyManager.isAdminActive(adminComponentName)
            
            if (!isDeviceAdminActive) {
                promise.resolve(WritableNativeMap().apply {
                    putBoolean("canDisable", false)
                    putString("reason", "Device admin is not currently active")
                    putString("action", "none")
                })
                return
            }

            // Check if we're device owner (device owners can remove themselves)
            val isDeviceOwner = try {
                devicePolicyManager.isDeviceOwnerApp(reactApplicationContext.packageName)
            } catch (e: Exception) {
                false
            }

            val canDisable = isDeviceOwner
            val reason = if (canDisable) {
                "Device Owner can disable programmatically"
            } else {
                "Regular Device Admin must be disabled manually for security"
            }
            
            val action = if (canDisable) {
                "Use disable button in app"
            } else {
                "Go to Settings → Security → Device admin apps → OdenSecureWave → Deactivate"
            }

            promise.resolve(WritableNativeMap().apply {
                putBoolean("canDisable", canDisable)
                putString("reason", reason)
                putString("action", action)
                putBoolean("isDeviceOwner", isDeviceOwner)
                putBoolean("isDeviceAdminActive", isDeviceAdminActive)
            })

            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "canDisableDeviceAdmin", "Disable capability checked", mapOf(
                "canDisable" to canDisable,
                "isDeviceOwner" to isDeviceOwner,
                "isDeviceAdminActive" to isDeviceAdminActive
            ))

        } catch (e: Exception) {
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "canDisableDeviceAdmin", e)
            promise.reject("ERROR", "Failed to check disable capability: ${e.message}")
        }
    }

    @ReactMethod
    fun getDeviceIMEI(promise: Promise) {
        try {
            val telephonyManager = reactApplicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            // Check if we have permission
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (reactApplicationContext.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != 
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    promise.reject("ERROR", "READ_PHONE_STATE permission not granted")
                    return
                }
            }
            
            val imei = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                telephonyManager.imei
            } else {
                @Suppress("DEPRECATION")
                telephonyManager.deviceId
            }
            
            if (imei.isNullOrEmpty()) {
                promise.reject("ERROR", "Unable to get device IMEI")
            } else {
                Log.d("DeviceAdminModule", "Device IMEI retrieved successfully")
                ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "getDeviceIMEI", "IMEI retrieved")
                promise.resolve(imei)
            }
        } catch (e: Exception) {
            Log.e("DeviceAdminModule", "Error getting device IMEI", e)
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "getDeviceIMEI", e)
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun getDeviceInfo(promise: Promise) {
        try {
            val deviceInfo = Arguments.createMap()
            
            // Get manufacturer
            val manufacturer = android.os.Build.MANUFACTURER
            deviceInfo.putString("manufacturer", manufacturer)
            
            // Get model
            val model = android.os.Build.MODEL
            deviceInfo.putString("model", model)
            
            // Get additional device information
            val brand = android.os.Build.BRAND
            deviceInfo.putString("brand", brand)
            
            val device = android.os.Build.DEVICE
            deviceInfo.putString("device", device)
            
            val product = android.os.Build.PRODUCT
            deviceInfo.putString("product", product)
            
            val androidVersion = android.os.Build.VERSION.RELEASE
            deviceInfo.putString("androidVersion", androidVersion)
            
            val apiLevel = android.os.Build.VERSION.SDK_INT
            deviceInfo.putInt("apiLevel", apiLevel)
            
            Log.d("DeviceAdminModule", "Device info retrieved - Manufacturer: $manufacturer, Model: $model")
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "getDeviceInfo", "Device info retrieved", mapOf(
                "manufacturer" to manufacturer,
                "model" to model,
                "brand" to brand
            ))
            
            promise.resolve(deviceInfo)
        } catch (e: Exception) {
            Log.e("DeviceAdminModule", "Error getting device info", e)
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "getDeviceInfo", e)
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun lockApp(promise: Promise) {
        try {
            Log.d("DeviceAdminModule", "lockApp called - enabling kiosk mode")
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "lockApp", "Kiosk mode lock requested")
            
            // Check if device admin is enabled
            if (!devicePolicyManager.isAdminActive(adminComponentName)) {
                Log.e("DeviceAdminModule", "Device admin not enabled - cannot enable kiosk mode")
                ErrorLogger.logWarning(reactApplicationContext, "DeviceAdminModule", "lockApp", "Device admin not enabled")
                promise.reject("ERROR", "Device admin not enabled")
                return
            }
            
            // Check if we have device owner privileges (required for Lock Task Mode)
            val isDeviceOwner = try {
                devicePolicyManager.isDeviceOwnerApp(reactApplicationContext.packageName)
            } catch (e: Exception) {
                ErrorLogger.logWarning(reactApplicationContext, "DeviceAdminModule", "lockApp", "Failed to check device owner status", mapOf("error" to e.message))
                false
            }
            
            if (!isDeviceOwner) {
                Log.e("DeviceAdminModule", "App is not device owner - cannot enable Lock Task Mode")
                ErrorLogger.logWarning(reactApplicationContext, "DeviceAdminModule", "lockApp", "Insufficient privileges for Lock Task Mode")
                promise.reject("PERMISSION_ERROR", "App needs to be Device Owner to enable kiosk mode. Run: ./setup-device-owner.sh")
                return
            }
            
            try {
                // Set our app as the only allowed package in Lock Task Mode
                val packageName = reactApplicationContext.packageName
                val allowedPackages = arrayOf(packageName)
                
                Log.d("DeviceAdminModule", "Setting lock task packages: [${packageName}]")
                devicePolicyManager.setLockTaskPackages(adminComponentName, allowedPackages)
                
                // Start Lock Task Mode for the current activity
                val currentActivity = reactApplicationContext.currentActivity
                if (currentActivity != null) {
                    Log.d("DeviceAdminModule", "Starting lock task mode for current activity")
                    currentActivity.startLockTask()
                    
                    Log.d("DeviceAdminModule", "Kiosk mode enabled successfully")
                    ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "lockApp", "Kiosk mode enabled successfully", mapOf("packageName" to packageName))
                    promise.resolve("Kiosk mode enabled successfully. Device is now locked to this app.")
                } else {
                    Log.w("DeviceAdminModule", "No current activity found - Lock Task packages set but mode not started")
                    ErrorLogger.logWarning(reactApplicationContext, "DeviceAdminModule", "lockApp", "No current activity to start lock task mode")
                    promise.resolve("Lock Task packages configured. Please restart the app to enter kiosk mode.")
                }
                
            } catch (securityException: SecurityException) {
                Log.e("DeviceAdminModule", "Security exception when enabling kiosk mode", securityException)
                ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "lockApp", securityException, mapOf("operation" to "setLockTaskPackages"))
                promise.reject("PERMISSION_ERROR", "Insufficient permissions to enable kiosk mode: ${securityException.message}")
            }
            
        } catch (e: Exception) {
            Log.e("DeviceAdminModule", "Error enabling kiosk mode", e)
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "lockApp", e)
            promise.reject("ERROR", "Failed to enable kiosk mode: ${e.message}")
        }
    }

    @ReactMethod
    fun unlockApp(promise: Promise) {
        try {
            Log.d("DeviceAdminModule", "unlockApp called - disabling kiosk mode")
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "unlockApp", "Kiosk mode unlock requested")
            
            // Check if device admin is enabled
            if (!devicePolicyManager.isAdminActive(adminComponentName)) {
                Log.e("DeviceAdminModule", "Device admin not enabled - cannot disable kiosk mode")
                ErrorLogger.logWarning(reactApplicationContext, "DeviceAdminModule", "unlockApp", "Device admin not enabled")
                promise.reject("ERROR", "Device admin not enabled")
                return
            }
            
            // Check if we have device owner privileges
            val isDeviceOwner = try {
                devicePolicyManager.isDeviceOwnerApp(reactApplicationContext.packageName)
            } catch (e: Exception) {
                ErrorLogger.logWarning(reactApplicationContext, "DeviceAdminModule", "unlockApp", "Failed to check device owner status", mapOf("error" to e.message))
                false
            }
            
            if (!isDeviceOwner) {
                Log.e("DeviceAdminModule", "App is not device owner - cannot disable Lock Task Mode")
                ErrorLogger.logWarning(reactApplicationContext, "DeviceAdminModule", "unlockApp", "Insufficient privileges for Lock Task Mode")
                promise.reject("PERMISSION_ERROR", "App needs to be Device Owner to disable kiosk mode")
                return
            }
            
            try {
                // Stop Lock Task Mode for the current activity
                val currentActivity = reactApplicationContext.currentActivity
                if (currentActivity != null) {
                    Log.d("DeviceAdminModule", "Stopping lock task mode for current activity")
                    currentActivity.stopLockTask()
                }
                
                // Clear the Lock Task packages (allow all apps)
                Log.d("DeviceAdminModule", "Clearing lock task packages")
                devicePolicyManager.setLockTaskPackages(adminComponentName, arrayOf())
                
                Log.d("DeviceAdminModule", "Kiosk mode disabled successfully")
                ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "unlockApp", "Kiosk mode disabled successfully")
                promise.resolve("Kiosk mode disabled successfully. Device can now be used freely.")
                
            } catch (securityException: SecurityException) {
                Log.e("DeviceAdminModule", "Security exception when disabling kiosk mode", securityException)
                ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "unlockApp", securityException, mapOf("operation" to "clearLockTaskPackages"))
                promise.reject("PERMISSION_ERROR", "Insufficient permissions to disable kiosk mode: ${securityException.message}")
            }
            
        } catch (e: Exception) {
            Log.e("DeviceAdminModule", "Error disabling kiosk mode", e)
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "unlockApp", e)
            promise.reject("ERROR", "Failed to disable kiosk mode: ${e.message}")
        }
    }

    @ReactMethod
    fun launchApp(promise: Promise) {
        try {
            Log.d("DeviceAdminModule", "launchApp called - using AppStateManager")
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "launchApp", "App launch requested via AppStateManager")
            
            val success = AppStateManager.launchApp(reactApplicationContext)
            
            if (success) {
                Log.d("DeviceAdminModule", "App launched successfully via AppStateManager")
                ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "launchApp", "App launched successfully via AppStateManager")
                promise.resolve("App launched successfully")
            } else {
                Log.e("DeviceAdminModule", "Failed to launch app via AppStateManager")
                ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "launchApp", Exception("AppStateManager.launchApp returned false"))
                promise.reject("ERROR", "Failed to launch app")
            }
            
        } catch (e: Exception) {
            Log.e("DeviceAdminModule", "Error launching app", e)
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "launchApp", e)
            promise.reject("ERROR", "Failed to launch app: ${e.message}")
        }
    }

    @ReactMethod
    fun closeApp(promise: Promise) {
        try {
            Log.d("DeviceAdminModule", "closeApp called - using AppStateManager")
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "closeApp", "App close requested via AppStateManager")
            
            val success = AppStateManager.closeApp(reactApplicationContext)
            
            if (success) {
                Log.d("DeviceAdminModule", "App closed successfully via AppStateManager")
                ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "closeApp", "App closed successfully via AppStateManager")
                promise.resolve("App closed successfully")
            } else {
                Log.e("DeviceAdminModule", "Failed to close app via AppStateManager")
                ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "closeApp", Exception("AppStateManager.closeApp returned false"))
                promise.reject("ERROR", "Failed to close app")
            }
            
        } catch (e: Exception) {
            Log.e("DeviceAdminModule", "Error closing app", e)
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "closeApp", e)
            promise.reject("ERROR", "Failed to close app: ${e.message}")
        }
    }

    @ReactMethod
    fun startPeriodicService(promise: Promise) {
        try {
            WorkManagerServiceManager.startPeriodicService(reactApplicationContext)
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "startPeriodicService", "WorkManager periodic service started")
            promise.resolve("Periodic service started successfully with WorkManager")
        } catch (e: Exception) {
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "startPeriodicService", e)
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun stopPeriodicService(promise: Promise) {
        try {
            WorkManagerServiceManager.stopPeriodicService(reactApplicationContext)
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "stopPeriodicService", "WorkManager periodic service stopped")
            promise.resolve("Periodic service stopped successfully")
        } catch (e: Exception) {
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "stopPeriodicService", e)
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun isPeriodicServiceEnabled(promise: Promise) {
        try {
            val isEnabled = WorkManagerServiceManager.isServiceEnabled(reactApplicationContext)
            val isWorkScheduled = WorkManagerServiceManager.isWorkScheduled(reactApplicationContext)
            
            val result = WritableNativeMap().apply {
                putBoolean("enabled", isEnabled)
                putBoolean("workScheduled", isWorkScheduled)
            }
            
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "isPeriodicServiceEnabled", 
                "WorkManager service status checked", mapOf("enabled" to isEnabled, "workScheduled" to isWorkScheduled))
            promise.resolve(result)
        } catch (e: Exception) {
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "isPeriodicServiceEnabled", e)
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun configureDeviceOwnerPrivileges(promise: Promise) {
        try {
            val success = DeviceOwnerPrivilegeManager.configureDeviceOwnerPrivileges(reactApplicationContext)
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "configureDeviceOwnerPrivileges", 
                "Device Owner privileges configuration completed", mapOf("success" to success))
            
            if (success) {
                promise.resolve("Device Owner privileges configured successfully")
            } else {
                promise.resolve("Device Owner privileges configuration completed with some limitations")
            }
        } catch (e: Exception) {
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "configureDeviceOwnerPrivileges", e)
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun getServicePrivilegeStatus(promise: Promise) {
        try {
            val status = DeviceOwnerPrivilegeManager.getPrivilegeStatus(reactApplicationContext)
            val serviceStatus = WorkManagerServiceManager.getServiceStatus(reactApplicationContext)
            
            val result = WritableNativeMap().apply {
                // Device Owner privilege status
                putBoolean("isDeviceOwner", status["isDeviceOwner"] as? Boolean ?: false)
                putInt("androidVersion", status["androidVersion"] as? Int ?: 0)
                
                if (status.containsKey("isIgnoringBatteryOptimizations")) {
                    putBoolean("isIgnoringBatteryOptimizations", status["isIgnoringBatteryOptimizations"] as? Boolean ?: false)
                }
                
                if (status.containsKey("isDeviceIdleMode")) {
                    putBoolean("isDeviceIdleMode", status["isDeviceIdleMode"] as? Boolean ?: false)
                }
                
                // WorkManager service status
                putBoolean("serviceEnabled", serviceStatus["enabled"] as? Boolean ?: false)
                putBoolean("workScheduled", serviceStatus["workScheduled"] as? Boolean ?: false)
                putInt("intervalMinutes", serviceStatus["intervalMinutes"] as? Int ?: 30)
                
                if (serviceStatus.containsKey("workStates")) {
                    val workStates = serviceStatus["workStates"] as? List<*>
                    putString("workStates", workStates?.joinToString(", ") ?: "Unknown")
                }
                
                if (status.containsKey("error")) {
                    putString("error", status["error"] as? String)
                }
            }
            
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "getServicePrivilegeStatus", 
                "WorkManager service privilege status retrieved")
            promise.resolve(result)
        } catch (e: Exception) {
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "getServicePrivilegeStatus", e)
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun initializeServiceOnStartup(promise: Promise) {
        try {
            WorkManagerServiceManager.initializeService(reactApplicationContext)
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "initializeServiceOnStartup", 
                "WorkManager service initialization completed")
            promise.resolve("Service initialization completed with WorkManager")
        } catch (e: Exception) {
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "initializeServiceOnStartup", e)
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun simulateBootCompleted(promise: Promise) {
        try {
            Log.d("DeviceAdminModule", "simulateBootCompleted called - triggering delayed boot check directly")
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "simulateBootCompleted", "Simulating delayed boot check")
            
            // Instead of sending system broadcast, directly trigger the delayed boot check
            BootStatusChecker.scheduleBootStatusCheck(reactApplicationContext)
            
            // Also directly trigger the delayed receiver for immediate testing
            val intent = Intent("com.odensecurewave.BOOT_STATUS_CHECK")
            val receiver = BootStatusReceiver()
            receiver.onReceive(reactApplicationContext, intent)
            
            promise.resolve("Delayed boot check triggered successfully for testing")
            
        } catch (e: Exception) {
            Log.e("DeviceAdminModule", "Error simulating boot check", e)
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "simulateBootCompleted", e)
            promise.reject("ERROR", "Failed to simulate boot check: ${e.message}")
        }
    }

    @ReactMethod
    fun testBootFirebaseCheck(promise: Promise) {
        try {
            Log.d("DeviceAdminModule", "testBootFirebaseCheck called - simulating boot Firebase check")
            ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "testBootFirebaseCheck", "Simulating boot Firebase check")
            
            // Create a simplified version of the boot Firebase check
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Get device IMEI
                    val deviceId = FirebaseDeviceStatusManager.getDeviceImei(reactApplicationContext)
                    if (deviceId == null) {
                        Log.w("DeviceAdminModule", "Could not get device IMEI for boot test")
                        promise.reject("ERROR", "Could not get device IMEI")
                        return@launch
                    }
                    
                    Log.d("DeviceAdminModule", "Device IMEI for boot test: $deviceId")
                    
                    // Check Firebase status
                    val deviceStatus = FirebaseDeviceStatusManager.getDeviceStatus(reactApplicationContext, deviceId)
                    Log.d("DeviceAdminModule", "Device status from Firebase in boot test: $deviceStatus")
                    
                    when (deviceStatus) {
                        "lock" -> {
                            Log.d("DeviceAdminModule", "Status is 'lock' - would launch app")
                            val launched = AppStateManager.launchApp(reactApplicationContext)
                            if (launched) {
                                promise.resolve("Status is 'lock' - App launched successfully in boot test")
                            } else {
                                promise.reject("ERROR", "Status is 'lock' but failed to launch app")
                            }
                        }
                        "active" -> {
                            promise.resolve("Status is 'active' - No app launch needed in boot test")
                        }
                        null -> {
                            promise.reject("ERROR", "Could not retrieve device status from Firebase")
                        }
                        else -> {
                            promise.resolve("Status is '$deviceStatus' - No special action in boot test")
                        }
                    }
                    
                } catch (e: Exception) {
                    Log.e("DeviceAdminModule", "Error in boot Firebase check test", e)
                    promise.reject("ERROR", "Boot Firebase check test failed: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            Log.e("DeviceAdminModule", "Error starting boot Firebase check test", e)
            ErrorLogger.logError(reactApplicationContext, "DeviceAdminModule", "testBootFirebaseCheck", e)
            promise.reject("ERROR", "Failed to start boot Firebase check test: ${e.message}")
        }
    }

    // Send events to React Native
    private fun sendEvent(eventName: String, params: WritableMap?) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    // Call this method when you want to send an event to React Native
    fun notifyDeviceAdminEnabled() {
        val params = Arguments.createMap()
        params.putString("status", "Device admin enabled")
        sendEvent("onDeviceAdminStatusChanged", params)
    }
}
