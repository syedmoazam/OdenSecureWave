package com.odensecurewave

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.preference.PreferenceManager
import android.util.Log

class AppDeviceAdminReceiver : DeviceAdminReceiver() {
    companion object {
        private const val TAG = "AppDeviceAdminReceiver"
        fun log(message: String) = Log.d("dpc::", message)

        private const val KEY_IS_FROM_BOOT_COMPLETED = "is_from_boot_completed"

        fun setIsFromBootCompleted(context: Context, value: Boolean) {
            try {
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                sharedPreferences.edit().putBoolean(KEY_IS_FROM_BOOT_COMPLETED, value).apply()
                ErrorLogger.logInfo(context, TAG, "setIsFromBootCompleted", "Boot completed flag set to: $value")
            } catch (e: Exception) {
                ErrorLogger.logError(context, TAG, "setIsFromBootCompleted", e, mapOf("value" to value))
            }
        }

        fun isFromBootCompleted(context: Context): Boolean {
            return try {
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
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
            log("onProfileProvisioningComplete")
            ErrorLogger.logInfo(context, TAG, "onProfileProvisioningComplete", "Profile provisioning completed")
            
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
            log("onReceive: action: $action, extras: $extras")
            
            ErrorLogger.logInfo(context, TAG, "onReceive", "Received broadcast", 
                mapOf(
                    "action" to action,
                    "extras" to extras?.toString(),
                    "scheme" to intent.scheme,
                    "data" to intent.dataString
                ))
            
            if (DevicePolicyManager.ACTION_MANAGED_PROFILE_PROVISIONED == action || Intent.ACTION_MANAGED_PROFILE_ADDED == action) {
                try {
                    PreferenceManager.getDefaultSharedPreferences(context)
                        .edit().putBoolean("is_provisioned", true).apply()
                    ErrorLogger.logInfo(context, TAG, "onReceive", "Marked profile as provisioned for action: $action")
                } catch (e: Exception) {
                    ErrorLogger.logError(context, TAG, "onReceive-setProvisioned", e, mapOf("action" to action))
                }
            }
            
            if (action == Intent.ACTION_BOOT_COMPLETED) {
                try {
                    setIsFromBootCompleted(context, true)
                    log("Boot completed - device admin receiver activated")
                    ErrorLogger.logInfo(context, TAG, "onReceive", "Boot completed - device admin receiver activated")
                } catch (e: Exception) {
                    ErrorLogger.logError(context, TAG, "onReceive-bootCompleted", e)
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
                    "userHandle" to userHandle?.toString(),
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
                    "userHandle" to userHandle?.toString(),
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
                    "userHandle" to userHandle?.toString(),
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
                    "userHandle" to userHandle?.toString(),
                    "intentAction" to intent.action
                ))
        }
    }
}
