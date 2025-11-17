package com.odensecurewave

import android.content.Context
import android.util.Log
import com.google.firebase.database.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * FirebaseUtils - Utility functions for Firebase Realtime Database operations
 * Provides simple methods to get and set data in Firebase database
 */
object FirebaseUtils {
    private const val TAG = "FirebaseUtils"
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    
    /**
     * Get data from Firebase database using reference path
     * @param context Application context for logging
     * @param referencePath The path to the data in Firebase database (e.g., "users/userId/profile")
     * @return The data retrieved from Firebase, or null if not found or error occurred
     */
    suspend fun getData(context: Context, referencePath: String): Any? {
        return try {
            Log.d(TAG, "Getting data from Firebase path: $referencePath")
            ErrorLogger.logInfo(context, TAG, "getData", "Starting Firebase data retrieval", mapOf("path" to referencePath))
            
            val reference = database.child(referencePath)
            
            val result = suspendCancellableCoroutine<Any?> { continuation ->
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val data = snapshot.value
                            Log.d(TAG, "Data retrieved successfully from path: $referencePath")
                            ErrorLogger.logInfo(context, TAG, "getData", "Data retrieved successfully", 
                                mapOf("path" to referencePath, "hasData" to (data != null)))
                            continuation.resume(data)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing data from Firebase", e)
                            ErrorLogger.logError(context, TAG, "getData", e, mapOf("path" to referencePath, "operation" to "onDataChange"))
                            continuation.resumeWithException(e)
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Firebase data retrieval cancelled: ${error.message}")
                        ErrorLogger.logError(context, TAG, "getData", Exception("Firebase error: ${error.message}"), 
                            mapOf("path" to referencePath, "errorCode" to error.code, "errorMessage" to error.message))
                        continuation.resumeWithException(Exception("Firebase error: ${error.message}"))
                    }
                }
                
                reference.addListenerForSingleValueEvent(listener)
                
                // Handle cancellation
                continuation.invokeOnCancellation {
                    Log.d(TAG, "Firebase getData operation cancelled for path: $referencePath")
                    reference.removeEventListener(listener)
                }
            }
            
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting data from Firebase path: $referencePath", e)
            ErrorLogger.logError(context, TAG, "getData", e, mapOf("path" to referencePath))
            null
        }
    }
    
    /**
     * Set data at Firebase database using reference path and value
     * @param context Application context for logging
     * @param referencePath The path where data should be stored (e.g., "users/userId/profile")
     * @param value The value to store at the specified path
     * @return true if data was set successfully, false otherwise
     */
    suspend fun setData(context: Context, referencePath: String, value: Any?): Boolean {
        return try {
            Log.d(TAG, "Setting data at Firebase path: $referencePath")
            ErrorLogger.logInfo(context, TAG, "setData", "Starting Firebase data storage", 
                mapOf("path" to referencePath, "valueType" to (value?.javaClass?.simpleName ?: "null")))
            
            val reference = database.child(referencePath)
            
            val result = suspendCancellableCoroutine<Boolean> { continuation ->
                reference.setValue(value) { error, ref ->
                    if (error != null) {
                        Log.e(TAG, "Failed to set data at Firebase path: $referencePath", error.toException())
                        ErrorLogger.logError(context, TAG, "setData", error.toException(), 
                            mapOf("path" to referencePath, "errorMessage" to error.message))
                        continuation.resume(false)
                    } else {
                        Log.d(TAG, "Data set successfully at Firebase path: $referencePath")
                        ErrorLogger.logInfo(context, TAG, "setData", "Data set successfully", 
                            mapOf("path" to referencePath, "reference" to ref.toString()))
                        continuation.resume(true)
                    }
                }
            }
            
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting data at Firebase path: $referencePath", e)
            ErrorLogger.logError(context, TAG, "setData", e, mapOf("path" to referencePath))
            false
        }
    }
    
    /**
     * Update specific fields at Firebase database using reference path and map of values
     * @param context Application context for logging
     * @param referencePath The path where data should be updated
     * @param updates Map of field names to values to update
     * @return true if data was updated successfully, false otherwise
     */
    suspend fun updateData(context: Context, referencePath: String, updates: Map<String, Any?>): Boolean {
        return try {
            Log.d(TAG, "Updating data at Firebase path: $referencePath")
            ErrorLogger.logInfo(context, TAG, "updateData", "Starting Firebase data update", 
                mapOf("path" to referencePath, "fieldsCount" to updates.size))
            
            val reference = database.child(referencePath)
            
            val result = suspendCancellableCoroutine<Boolean> { continuation ->
                reference.updateChildren(updates) { error, ref ->
                    if (error != null) {
                        Log.e(TAG, "Failed to update data at Firebase path: $referencePath", error.toException())
                        ErrorLogger.logError(context, TAG, "updateData", error.toException(), 
                            mapOf("path" to referencePath, "errorMessage" to error.message))
                        continuation.resume(false)
                    } else {
                        Log.d(TAG, "Data updated successfully at Firebase path: $referencePath")
                        ErrorLogger.logInfo(context, TAG, "updateData", "Data updated successfully", 
                            mapOf("path" to referencePath, "reference" to ref.toString()))
                        continuation.resume(true)
                    }
                }
            }
            
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating data at Firebase path: $referencePath", e)
            ErrorLogger.logError(context, TAG, "updateData", e, mapOf("path" to referencePath))
            false
        }
    }
    
    /**
     * Delete data at Firebase database using reference path
     * @param context Application context for logging
     * @param referencePath The path of data to delete
     * @return true if data was deleted successfully, false otherwise
     */
    suspend fun deleteData(context: Context, referencePath: String): Boolean {
        return try {
            Log.d(TAG, "Deleting data at Firebase path: $referencePath")
            ErrorLogger.logInfo(context, TAG, "deleteData", "Starting Firebase data deletion", mapOf("path" to referencePath))
            
            val reference = database.child(referencePath)
            
            val result = suspendCancellableCoroutine<Boolean> { continuation ->
                reference.removeValue { error, ref ->
                    if (error != null) {
                        Log.e(TAG, "Failed to delete data at Firebase path: $referencePath", error.toException())
                        ErrorLogger.logError(context, TAG, "deleteData", error.toException(), 
                            mapOf("path" to referencePath, "errorMessage" to error.message))
                        continuation.resume(false)
                    } else {
                        Log.d(TAG, "Data deleted successfully at Firebase path: $referencePath")
                        ErrorLogger.logInfo(context, TAG, "deleteData", "Data deleted successfully", 
                            mapOf("path" to referencePath, "reference" to ref.toString()))
                        continuation.resume(true)
                    }
                }
            }
            
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting data at Firebase path: $referencePath", e)
            ErrorLogger.logError(context, TAG, "deleteData", e, mapOf("path" to referencePath))
            false
        }
    }
    
    /**
     * Check if data exists at Firebase database path
     * @param context Application context for logging
     * @param referencePath The path to check for data existence
     * @return true if data exists, false otherwise
     */
    suspend fun dataExists(context: Context, referencePath: String): Boolean {
        return try {
            Log.d(TAG, "Checking if data exists at Firebase path: $referencePath")
            ErrorLogger.logInfo(context, TAG, "dataExists", "Checking data existence", mapOf("path" to referencePath))
            
            val reference = database.child(referencePath)
            
            val result = suspendCancellableCoroutine<Boolean> { continuation ->
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val exists = snapshot.exists()
                        Log.d(TAG, "Data existence check completed for path: $referencePath, exists: $exists")
                        ErrorLogger.logInfo(context, TAG, "dataExists", "Data existence check completed", 
                            mapOf("path" to referencePath, "exists" to exists))
                        continuation.resume(exists)
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Firebase data existence check cancelled: ${error.message}")
                        ErrorLogger.logError(context, TAG, "dataExists", Exception("Firebase error: ${error.message}"), 
                            mapOf("path" to referencePath, "errorCode" to error.code))
                        continuation.resume(false)
                    }
                }
                
                reference.addListenerForSingleValueEvent(listener)
                
                continuation.invokeOnCancellation {
                    reference.removeEventListener(listener)
                }
            }
            
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking data existence at Firebase path: $referencePath", e)
            ErrorLogger.logError(context, TAG, "dataExists", e, mapOf("path" to referencePath))
            false
        }
    }
}
