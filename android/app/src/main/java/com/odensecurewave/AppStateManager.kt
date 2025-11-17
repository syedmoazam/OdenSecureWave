package com.odensecurewave

import android.app.ActivityManager
import android.content.Context
import android.util.Log

/**
 * AppStateManager - Utility to check if the app is currently running
 * Used by PeriodicWorker to determine if it should execute
 */
object AppStateManager {
    private const val TAG = "AppStateManager"
    
    /**
     * Check if the app is currently running (foreground or background with visible activities)
     * Returns true if the main app UI is running, false if only background services are running
     */
    fun isAppRunning(context: Context): Boolean {
        return try {
            Log.d(TAG, "Checking if app is currently running")
            
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val packageName = context.packageName
            
            // Method 1: Check running app processes for FOREGROUND or VISIBLE apps only
            val runningProcesses = activityManager.runningAppProcesses
            if (runningProcesses != null) {
                for (processInfo in runningProcesses) {
                    if (processInfo.processName == packageName) {
                        // Only consider the app "running" if it's in foreground or visible state
                        // This excludes background services like PeriodicWorker
                        val isAppActive = when (processInfo.importance) {
                            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> true
                            ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> true
                            ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING -> true
                            else -> false
                        }
                        
                        if (isAppActive) {
                            Log.d(TAG, "App is running - found in active state (importance: ${processInfo.importance})")
                            ErrorLogger.logInfo(context, TAG, "isAppRunning", "App is running - found in active state",
                                mapOf("processName" to processInfo.processName, "importance" to processInfo.importance))
                            return true
                        } else {
                            Log.d(TAG, "App process found but only in background (importance: ${processInfo.importance})")
                            ErrorLogger.logInfo(context, TAG, "isAppRunning", "App process found but only in background",
                                mapOf("processName" to processInfo.processName, "importance" to processInfo.importance))
                        }
                    }
                }
            }
            
            // Method 2: Check if any activities are running (as fallback)
            val runningTasks = try {
                @Suppress("DEPRECATION")
                activityManager.getRunningTasks(1)
            } catch (e: Exception) {
                Log.w(TAG, "Could not get running tasks", e)
                null
            }
            
            if (runningTasks != null && runningTasks.isNotEmpty()) {
                val topActivity = runningTasks[0].topActivity
                if (topActivity?.packageName == packageName) {
                    Log.d(TAG, "App is running - found in running tasks")
                    ErrorLogger.logInfo(context, TAG, "isAppRunning", "App is running - found in running tasks",
                        mapOf("topActivity" to (topActivity?.className ?: "unknown")))
                    return true
                }
            }
            
            Log.d(TAG, "App is not currently running (only background services may be active)")
            ErrorLogger.logInfo(context, TAG, "isAppRunning", "App is not currently running")
            return false
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if app is running", e)
            ErrorLogger.logError(context, TAG, "isAppRunning", e)
            // If we can't determine, assume it's not running to be safe
            return false
        }
    }
    
    /**
     * Check if the app is in the foreground
     * Returns true if the app is in foreground, false otherwise
     */
    fun isAppInForeground(context: Context): Boolean {
        return try {
            Log.d(TAG, "Checking if app is in foreground")
            
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val packageName = context.packageName
            
            // Check running app processes and their importance
            val runningProcesses = activityManager.runningAppProcesses
            if (runningProcesses != null) {
                for (processInfo in runningProcesses) {
                    if (processInfo.processName == packageName) {
                        val isInForeground = processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                        Log.d(TAG, "App foreground status: $isInForeground (importance: ${processInfo.importance})")
                        ErrorLogger.logInfo(context, TAG, "isAppInForeground", "App foreground status checked",
                            mapOf("isInForeground" to isInForeground, "importance" to processInfo.importance))
                        return isInForeground
                    }
                }
            }
            
            Log.d(TAG, "App is not in foreground")
            ErrorLogger.logInfo(context, TAG, "isAppInForeground", "App is not in foreground")
            return false
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if app is in foreground", e)
            ErrorLogger.logError(context, TAG, "isAppInForeground", e)
            return false
        }
    }
    
    /**
     * Launch the app
     * Returns true if successful, false otherwise
     */
    fun launchApp(context: Context): Boolean {
        return try {
            Log.d(TAG, "Launching app")
            ErrorLogger.logInfo(context, TAG, "launchApp", "Attempting to launch app")
            
            val packageManager = context.packageManager
            val launchIntent = packageManager.getLaunchIntentForPackage(context.packageName)
            
            if (launchIntent != null) {
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                context.startActivity(launchIntent)
                
                Log.d(TAG, "App launched successfully")
                ErrorLogger.logInfo(context, TAG, "launchApp", "App launched successfully")
                return true
            } else {
                Log.e(TAG, "Could not find launch intent for app")
                ErrorLogger.logError(context, TAG, "launchApp", Exception("No launch intent found"))
                return false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app", e)
            ErrorLogger.logError(context, TAG, "launchApp", e)
            return false
        }
    }
    
    /**
     * Close/kill the app
     * Returns true if successful, false otherwise
     */
    fun closeApp(context: Context): Boolean {
        return try {
            Log.d(TAG, "Attempting to close app")
            ErrorLogger.logInfo(context, TAG, "closeApp", "Attempting to close app")
            
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val packageName = context.packageName
            
            // Method 1: Kill background processes
            activityManager.killBackgroundProcesses(packageName)
            Log.d(TAG, "Killed background processes for: $packageName")
            
            // Method 2: Finish all activities if we have access to them
            // Note: This might require the app to be running and have proper lifecycle management
            try {
                // Force stop the application (requires FORCE_STOP_PACKAGES permission)
                // This is more aggressive but may not work without system permissions
                val runtime = Runtime.getRuntime()
                runtime.exec("am force-stop $packageName")
                Log.d(TAG, "Executed force-stop command for: $packageName")
            } catch (e: Exception) {
                Log.w(TAG, "Could not execute force-stop command", e)
                // This is expected for non-system apps
            }
            
            // Method 3: System exit as last resort (only affects current process)
            try {
                android.os.Process.killProcess(android.os.Process.myPid())
            } catch (e: Exception) {
                Log.w(TAG, "Could not kill current process", e)
            }
            
            Log.d(TAG, "App close attempt completed")
            ErrorLogger.logInfo(context, TAG, "closeApp", "App close attempt completed")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error closing app", e)
            ErrorLogger.logError(context, TAG, "closeApp", e)
            return false
        }
    }
}
