package com.odensecurewave

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
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
     * Get current device location using Device Owner privileges
     * Returns null if location cannot be obtained
     */
    fun getCurrentLocation(context: Context): LocationData? {
        return try {
            Log.d(TAG, "Attempting to get current device location")
            ErrorLogger.logInfo(context, TAG, "getCurrentLocation", "Starting location retrieval")
            
            // Check if we have location permissions (should be granted for Device Owner)
            if (!hasLocationPermissions(context)) {
                Log.w(TAG, "Location permissions not granted")
                ErrorLogger.logWarning(context, TAG, "getCurrentLocation", "Location permissions not granted")
                return null
            }
            
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            
            // Check if location services are enabled
            if (!isLocationEnabled(locationManager)) {
                Log.w(TAG, "Location services are disabled")
                ErrorLogger.logWarning(context, TAG, "getCurrentLocation", "Location services are disabled")
                return null
            }
            
            // Try to get last known location from different providers
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
                ErrorLogger.logInfo(context, TAG, "getCurrentLocation", "Location retrieved successfully", 
                    mapOf(
                        "latitude" to location.latitude,
                        "longitude" to location.longitude,
                        "accuracy" to location.accuracy,
                        "provider" to location.provider
                    ))
                
                return locationData
            } else {
                Log.w(TAG, "No location available from any provider")
                ErrorLogger.logWarning(context, TAG, "getCurrentLocation", "No location available from any provider")
                return null
            }
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting location (should not happen for Device Owner)", e)
            ErrorLogger.logError(context, TAG, "getCurrentLocation", e, mapOf("errorType" to "SecurityException"))
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current location", e)
            ErrorLogger.logError(context, TAG, "getCurrentLocation", e)
            null
        }
    }
    
    /**
     * Check if the app has location permissions
     */
    private fun hasLocationPermissions(context: Context): Boolean {
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
        
        Log.d(TAG, "Location permissions - Fine: $fineLocationGranted, Coarse: $coarseLocationGranted, Background: $backgroundLocationGranted")
        
        return fineLocationGranted || coarseLocationGranted
    }
    
    /**
     * Check if location services are enabled on the device
     */
    private fun isLocationEnabled(locationManager: LocationManager): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager.isLocationEnabled
            } else {
                @Suppress("DEPRECATION")
                val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                @Suppress("DEPRECATION")
                val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                gpsEnabled || networkEnabled
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking if location is enabled", e)
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
