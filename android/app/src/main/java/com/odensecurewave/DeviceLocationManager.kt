package com.odensecurewave

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * DeviceLocationManager - Handles location retrieval for Device Owner apps
 * Since the app has Device Owner privileges, it can access location without user prompts
 */
object DeviceLocationManager {
    private const val TAG = "DeviceLocationManager"
    private const val LOCATION_TIMEOUT = 30_000L // 30 seconds timeout
    
    /**
     * Check if the app has location permissions
     * @param context Application context
     * @return true if location permissions are granted, false otherwise
     */
    fun hasLocationPermission(context: Context): Boolean {
        return try {
            Log.d(TAG, "Checking location permissions")
            ErrorLogger.logInfo(context, TAG, "hasLocationPermission", "Checking location permissions")
            
            val fineLocationGranted = ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            val coarseLocationGranted = ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            val backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Not required for older Android versions
            }
            
            val hasPermissions = fineLocationGranted || coarseLocationGranted
            
            Log.d(TAG, "Location permissions - Fine: $fineLocationGranted, Coarse: $coarseLocationGranted, Background: $backgroundLocationGranted")
            ErrorLogger.logInfo(context, TAG, "hasLocationPermission", "Location permission check completed", 
                mapOf("fine" to fineLocationGranted, "coarse" to coarseLocationGranted, "background" to backgroundLocationGranted, "hasPermissions" to hasPermissions))
            
            return hasPermissions
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking location permissions", e)
            ErrorLogger.logError(context, TAG, "hasLocationPermission", e)
            false
        }
    }
    
    /**
     * Get location permissions using Device Owner privileges
     * @param context Application context
     * @return true if permissions were granted successfully, false otherwise
     */
    fun getLocationPermission(context: Context): Boolean {
        return try {
            Log.d(TAG, "Attempting to get location permissions using Device Owner privileges")
            ErrorLogger.logInfo(context, TAG, "getLocationPermission", "Attempting to get location permissions")
            
            // Check if we already have permissions
            if (hasLocationPermission(context)) {
                Log.d(TAG, "Location permissions already granted")
                ErrorLogger.logInfo(context, TAG, "getLocationPermission", "Location permissions already granted")
                return true
            }
            
            var allGranted = true
            
            // Grant fine location permission
            val fineLocationGranted = DeviceOwnerPrivilegeManager.grantPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            if (!fineLocationGranted) {
                Log.w(TAG, "Failed to grant fine location permission")
                allGranted = false
            }
            
            // Grant coarse location permission as fallback
            val coarseLocationGranted = DeviceOwnerPrivilegeManager.grantPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (!coarseLocationGranted) {
                Log.w(TAG, "Failed to grant coarse location permission")
                if (!fineLocationGranted) allGranted = false
            }
            
            // Grant background location permission for Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val backgroundLocationGranted = DeviceOwnerPrivilegeManager.grantPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                if (!backgroundLocationGranted) {
                    Log.w(TAG, "Failed to grant background location permission")
                    // Don't mark as failure since background location is not always critical
                }
            }
            
            if (allGranted) {
                Log.d(TAG, "Location permissions granted successfully")
                ErrorLogger.logInfo(context, TAG, "getLocationPermission", "Location permissions granted successfully")
            } else {
                Log.w(TAG, "Some location permissions could not be granted")
                ErrorLogger.logWarning(context, TAG, "getLocationPermission", "Some location permissions could not be granted")
            }
            
            return allGranted
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location permissions", e)
            ErrorLogger.logError(context, TAG, "getLocationPermission", e)
            false
        }
    }
    
    /**
     * Check if location services are enabled on the device
     * @param context Application context
     * @return true if location is enabled, false otherwise
     */
    fun isLocationEnabled(context: Context): Boolean {
        return try {
            Log.d(TAG, "Checking if location services are enabled")
            ErrorLogger.logInfo(context, TAG, "isLocationEnabled", "Checking location services status")
            
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            
            val isEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager.isLocationEnabled
            } else {
                @Suppress("DEPRECATION")
                val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                @Suppress("DEPRECATION")
                val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                gpsEnabled || networkEnabled
            }
            
            Log.d(TAG, "Location services enabled: $isEnabled")
            ErrorLogger.logInfo(context, TAG, "isLocationEnabled", "Location services status checked", mapOf("enabled" to isEnabled))
            
            return isEnabled
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if location is enabled", e)
            ErrorLogger.logError(context, TAG, "isLocationEnabled", e)
            false
        }
    }
    
    /**
     * Get current device location. If location is not enabled, it will programmatically turn on location and return location data.
     * @param context Application context
     * @return LocationData if successful, null otherwise
     */
    fun getLocation(context: Context): LocationData? {
        return try {
            Log.d(TAG, "Getting device location with auto-enable functionality")
            ErrorLogger.logInfo(context, TAG, "getLocation", "Starting location retrieval with auto-enable")
            
            // Step 1: Ensure we have location permissions
            if (!hasLocationPermission(context)) {
                Log.d(TAG, "Location permissions not available, attempting to get them")
                if (!getLocationPermission(context)) {
                    Log.e(TAG, "Could not obtain location permissions")
                    ErrorLogger.logError(context, TAG, "getLocation", Exception("Could not obtain location permissions"))
                    return null
                }
            }
            
            // Step 2: Check if location is enabled, if not try to enable it
            if (!isLocationEnabled(context)) {
                Log.d(TAG, "Location services disabled, attempting to enable programmatically")
                if (!enableLocationServices(context)) {
                    Log.e(TAG, "Could not enable location services")
                    ErrorLogger.logError(context, TAG, "getLocation", Exception("Could not enable location services"))
                    return null
                }
            }
            
            // Step 3: Get the actual location
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = getBestLastKnownLocation(locationManager)
            
            if (location != null) {
                val locationData = LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    timestamp = location.time,
                    provider = location.provider ?: "unknown"
                )
                
                Log.d(TAG, "Location retrieved successfully: $locationData")
                ErrorLogger.logInfo(context, TAG, "getLocation", "Location retrieved successfully", 
                    mapOf(
                        "latitude" to location.latitude,
                        "longitude" to location.longitude,
                        "accuracy" to location.accuracy,
                        "provider" to location.provider
                    ))
                
                return locationData
            } else {
                Log.w(TAG, "No location available from any provider")
                ErrorLogger.logWarning(context, TAG, "getLocation", "No location available from any provider")
                return null
            }
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting location (should not happen for Device Owner)", e)
            ErrorLogger.logError(context, TAG, "getLocation", e, mapOf("errorType" to "SecurityException"))
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            ErrorLogger.logError(context, TAG, "getLocation", e)
            null
        }
    }
    
    /**
     * Enable location services programmatically using Device Owner privileges
     */
    private fun enableLocationServices(context: Context): Boolean {
        return try {
            Log.d(TAG, "Attempting to enable location services using Device Owner privileges")
            ErrorLogger.logInfo(context, TAG, "enableLocationServices", "Attempting to enable location services")
            
            // For Device Owner apps, we can enable location services programmatically
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // Android 9.0+ - Location mode is managed differently
                    Log.d(TAG, "Android 9.0+ detected - location enabling may require user interaction")
                    ErrorLogger.logInfo(context, TAG, "enableLocationServices", "Android 9.0+ - limited programmatic control")
                } else {
                    // Pre-Android 9.0 approach
                    @Suppress("DEPRECATION")
                    Settings.Secure.putInt(context.contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_HIGH_ACCURACY)
                    Log.d(TAG, "Location services enabled successfully (Pre-Android 9.0)")
                    ErrorLogger.logInfo(context, TAG, "enableLocationServices", "Location services enabled successfully (Pre-Android 9.0)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not enable location services via Settings", e)
            }
            
            // Fallback: Check if location is now enabled after our attempts
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager.isLocationEnabled
            } else {
                @Suppress("DEPRECATION")
                val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                @Suppress("DEPRECATION")
                val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                gpsEnabled || networkEnabled
            }
            
            if (isEnabled) {
                Log.d(TAG, "Location services are now enabled")
                ErrorLogger.logInfo(context, TAG, "enableLocationServices", "Location services are now enabled")
                return true
            } else {
                Log.w(TAG, "Location services could not be enabled programmatically")
                ErrorLogger.logWarning(context, TAG, "enableLocationServices", "Location services could not be enabled programmatically")
                return false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling location services", e)
            ErrorLogger.logError(context, TAG, "enableLocationServices", e)
            false
        }
    }
    
    /**
     * Get the best available last known location from all providers
     */
    private fun getBestLastKnownLocation(locationManager: LocationManager): Location? {
        try {
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            
            var bestLocation: Location? = null
            
            for (provider in providers) {
                try {
                    if (locationManager.isProviderEnabled(provider)) {
                        @Suppress("MissingPermission") // We checked permissions earlier
                        val location = locationManager.getLastKnownLocation(provider)
                        
                        if (location != null && isBetterLocation(location, bestLocation)) {
                            bestLocation = location
                            Log.d(TAG, "Found location from provider $provider: ${location.latitude}, ${location.longitude}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error getting location from provider $provider", e)
                }
            }
            
            return bestLocation
        } catch (e: Exception) {
            Log.e(TAG, "Error getting best known location", e)
            return null
        }
    }
    
    /**
     * Determine if one location reading is better than the current location fix
     */
    private fun isBetterLocation(location: Location, currentBestLocation: Location?): Boolean {
        if (currentBestLocation == null) {
            return true
        }
        
        // Check whether the new location fix is newer or older
        val timeDelta = location.time - currentBestLocation.time
        val isSignificantlyNewer = timeDelta > 2 * 60 * 1000 // 2 minutes
        val isSignificantlyOlder = timeDelta < -2 * 60 * 1000
        val isNewer = timeDelta > 0
        
        // If it's been more than two minutes since the current location, use the new location
        if (isSignificantlyNewer) {
            return true
        } else if (isSignificantlyOlder) {
            return false
        }
        
        // Check whether the new location fix is more or less accurate
        val accuracyDelta = (location.accuracy - currentBestLocation.accuracy).toInt()
        val isLessAccurate = accuracyDelta > 0
        val isMoreAccurate = accuracyDelta < 0
        val isSignificantlyLessAccurate = accuracyDelta > 200
        
        // Check if the old and new location are from the same provider
        val isFromSameProvider = location.provider == currentBestLocation.provider
        
        // Determine location quality using a combination of timeliness and accuracy
        return when {
            isMoreAccurate -> true
            isNewer && !isLessAccurate -> true
            isNewer && !isSignificantlyLessAccurate && isFromSameProvider -> true
            else -> false
        }
    }
}

/**
 * Data class to represent location information
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val provider: String
) {
    override fun toString(): String {
        return "Location(lat=$latitude, lng=$longitude, accuracy=${accuracy}m, provider=$provider)"
    }
    
    /**
     * Convert to map for Firebase storage
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "accuracy" to accuracy,
            "timestamp" to timestamp,
            "provider" to provider,
            "retrievedAt" to System.currentTimeMillis()
        )
    }
}
