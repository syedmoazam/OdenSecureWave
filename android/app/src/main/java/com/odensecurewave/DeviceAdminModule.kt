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
    fun getDeviceIMEI(promise: Promise) {
        try {
            val telephonyManager = reactApplicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            // Check if we have permission
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (reactApplicationContext.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != 
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    
                    Log.d("DeviceAdminModule", "READ_PHONE_STATE permission not granted, attempting to grant via Device Owner privileges")
                    ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "getDeviceIMEI", "Attempting to grant READ_PHONE_STATE permission")
                    
                    // Try to grant permission using Device Owner privileges
                    val permissionGranted = DeviceOwnerPrivilegeManager.grantPermission(
                        reactApplicationContext, 
                        android.Manifest.permission.READ_PHONE_STATE
                    )
                    
                    if (!permissionGranted) {
                        Log.w("DeviceAdminModule", "Failed to grant READ_PHONE_STATE permission via Device Owner")
                        ErrorLogger.logWarning(reactApplicationContext, "DeviceAdminModule", "getDeviceIMEI", "Failed to grant READ_PHONE_STATE permission")
                        promise.reject("ERROR", "READ_PHONE_STATE permission not granted and could not be granted automatically. Please ensure the app has Device Owner privileges or grant the permission manually.")
                        return
                    }
                    
                    Log.d("DeviceAdminModule", "READ_PHONE_STATE permission granted successfully via Device Owner")
                    ErrorLogger.logInfo(reactApplicationContext, "DeviceAdminModule", "getDeviceIMEI", "READ_PHONE_STATE permission granted successfully")
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
