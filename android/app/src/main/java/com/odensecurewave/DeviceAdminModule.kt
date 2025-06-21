package com.odensecurewave

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule

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
                    promise.reject("ERROR", "Cannot disable device admin programmatically. Please go to Settings > Security > Device admin apps and disable it manually.")
                }
                
            } catch (securityException: SecurityException) {
                Log.e("DeviceAdminModule", "Security exception when disabling device admin", securityException)
                promise.reject("ERROR", "Cannot disable device admin programmatically. Please go to Settings > Security > Device admin apps and disable it manually.")
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
