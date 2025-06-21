package com.odensecurewave

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object ErrorLogger {
    private const val TAG = "ErrorLogger"
    private const val LOG_FILE_NAME = "device_admin_errors.log"
    private const val MAX_LOG_SIZE = 5 * 1024 * 1024 // 5MB
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    /**
     * Initialize the error logger and ensure log file exists
     */
    fun initialize(context: Context) {
        try {
            val logFile = getLogFile(context)
            ensureLogFileExists(context, logFile)
            logInfo(context, "ErrorLogger", "initialize", "Error logger initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ErrorLogger", e)
        }
    }
    
    /**
     * Log an error with detailed context information
     */
    fun logError(
        context: Context,
        source: String,
        operation: String,
        exception: Exception,
        additionalInfo: Map<String, Any?> = emptyMap()
    ) {
        try {
            val timestamp = dateFormat.format(Date())
            val errorDetails = buildErrorDetails(timestamp, source, operation, exception, additionalInfo)
            
            // Log to Android system log
            Log.e(TAG, errorDetails)
            
            // Write to file
            writeToFile(context, errorDetails)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log error", e)
        }
    }
    
    /**
     * Log a warning with context
     */
    fun logWarning(
        context: Context,
        source: String,
        operation: String,
        message: String,
        additionalInfo: Map<String, Any?> = emptyMap()
    ) {
        try {
            val timestamp = dateFormat.format(Date())
            val warningDetails = buildWarningDetails(timestamp, source, operation, message, additionalInfo)
            
            // Log to Android system log
            Log.w(TAG, warningDetails)
            
            // Write to file
            writeToFile(context, warningDetails)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log warning", e)
        }
    }
    
    /**
     * Log an info message with context
     */
    fun logInfo(
        context: Context,
        source: String,
        operation: String,
        message: String,
        additionalInfo: Map<String, Any?> = emptyMap()
    ) {
        try {
            val timestamp = dateFormat.format(Date())
            val infoDetails = buildInfoDetails(timestamp, source, operation, message, additionalInfo)
            
            // Log to Android system log
            Log.i(TAG, infoDetails)
            
            // Write to file
            writeToFile(context, infoDetails)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log info", e)
        }
    }
    
    private fun buildErrorDetails(
        timestamp: String,
        source: String,
        operation: String,
        exception: Exception,
        additionalInfo: Map<String, Any?>
    ): String {
        val sb = StringBuilder()
        sb.append("\\n=== ERROR ===\\n")
        sb.append("Timestamp: $timestamp\\n")
        sb.append("Source: $source\\n")
        sb.append("Operation: $operation\\n")
        sb.append("Exception: ${exception.javaClass.simpleName}\\n")
        sb.append("Message: ${exception.message}\\n")
        sb.append("Stack Trace:\\n")
        exception.stackTrace.forEach { stackElement ->
            sb.append("  at $stackElement\\n")
        }
        
        if (additionalInfo.isNotEmpty()) {
            sb.append("Additional Info:\\n")
            additionalInfo.forEach { (key, value) ->
                sb.append("  $key: $value\\n")
            }
        }
        
        // Add device/app context
        sb.append("Android Version: ${android.os.Build.VERSION.RELEASE}\\n")
        sb.append("SDK Level: ${android.os.Build.VERSION.SDK_INT}\\n")
        sb.append("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\\n")
        sb.append("=============\\n")
        
        return sb.toString()
    }
    
    private fun buildWarningDetails(
        timestamp: String,
        source: String,
        operation: String,
        message: String,
        additionalInfo: Map<String, Any?>
    ): String {
        val sb = StringBuilder()
        sb.append("\\n=== WARNING ===\\n")
        sb.append("Timestamp: $timestamp\\n")
        sb.append("Source: $source\\n")
        sb.append("Operation: $operation\\n")
        sb.append("Message: $message\\n")
        
        if (additionalInfo.isNotEmpty()) {
            sb.append("Additional Info:\\n")
            additionalInfo.forEach { (key, value) ->
                sb.append("  $key: $value\\n")
            }
        }
        sb.append("===============\\n")
        
        return sb.toString()
    }
    
    private fun buildInfoDetails(
        timestamp: String,
        source: String,
        operation: String,
        message: String,
        additionalInfo: Map<String, Any?>
    ): String {
        val sb = StringBuilder()
        sb.append("\\n=== INFO ===\\n")
        sb.append("Timestamp: $timestamp\\n")
        sb.append("Source: $source\\n")
        sb.append("Operation: $operation\\n")
        sb.append("Message: $message\\n")
        
        if (additionalInfo.isNotEmpty()) {
            sb.append("Additional Info:\\n")
            additionalInfo.forEach { (key, value) ->
                sb.append("  $key: $value\\n")
            }
        }
        sb.append("=============\\n")
        
        return sb.toString()
    }
    
    private fun writeToFile(context: Context, content: String) {
        try {
            val logFile = getLogFile(context)
            
            // Ensure the log file exists, create if it doesn't
            ensureLogFileExists(context, logFile)
            
            // Check file size and rotate if needed
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                rotateLogFile(context)
            }
            
            FileWriter(logFile, true).use { writer ->
                writer.append(content)
                writer.flush()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }
    
    private fun getLogFile(context: Context): File {
        return File(context.filesDir, LOG_FILE_NAME)
    }
    
    /**
     * Ensure the log file exists, create it if it doesn't
     */
    private fun ensureLogFileExists(context: Context, logFile: File) {
        try {
            // Ensure the parent directory exists
            val parentDir = logFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                val created = parentDir.mkdirs()
                Log.i(TAG, "Created log directory: $created")
            }
            
            // Create the log file if it doesn't exist
            if (!logFile.exists()) {
                val created = logFile.createNewFile()
                if (created) {
                    Log.i(TAG, "Created new log file: ${logFile.absolutePath}")
                    
                    // Write initial header to the new file
                    val timestamp = dateFormat.format(Date())
                    val header = """
                        |===============================================
                        |    Device Admin Error Log
                        |    Created: $timestamp
                        |    Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                        |    Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})
                        |    App Package: ${context.packageName}
                        |===============================================
                        |
                    """.trimMargin()
                    
                    FileWriter(logFile, false).use { writer ->
                        writer.write(header)
                        writer.flush()
                    }
                } else {
                    Log.w(TAG, "Failed to create log file: ${logFile.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ensure log file exists", e)
        }
    }
    
    private fun rotateLogFile(context: Context) {
        try {
            val currentFile = getLogFile(context)
            val backupFile = File(context.filesDir, "${LOG_FILE_NAME}.backup")
            
            // Delete old backup
            if (backupFile.exists()) {
                backupFile.delete()
            }
            
            // Move current to backup
            currentFile.renameTo(backupFile)
            
            Log.i(TAG, "Log file rotated")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate log file", e)
        }
    }
    
    /**
     * Get the current log file content
     */
    fun getLogContent(context: Context): String {
        return try {
            val logFile = getLogFile(context)
            if (logFile.exists()) {
                logFile.readText()
            } else {
                "No log file found"
            }
        } catch (e: Exception) {
            "Error reading log file: ${e.message}"
        }
    }
    
    /**
     * Clear the log file
     */
    fun clearLogs(context: Context): Boolean {
        return try {
            val logFile = getLogFile(context)
            val backupFile = File(context.filesDir, "${LOG_FILE_NAME}.backup")
            
            logFile.delete()
            backupFile.delete()
            
            logInfo(context, "ErrorLogger", "clearLogs", "Log files cleared")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear log files", e)
            false
        }
    }
}
