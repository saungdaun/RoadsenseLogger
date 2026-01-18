package com.roadsense.logger.core.bluetooth

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * BluetoothHandler for ESP32 v2.4 Roadsense Logger
 * Compatible with ESP32_Simpel_FIXED.txt and Mega2560_Simpel_FIXED.txt
 */

// Interface untuk callback
interface BluetoothCallback {
    fun onConnectionStateChanged(state: Int)
    fun onDataReceived(data: RoadsenseData)
    fun onDeviceConnected(deviceName: String)
    fun onMessageReceived(message: String)
    fun onError(errorMessage: String)
    fun onTimeUpdated(hour: Int, minute: Int)  // New for time display
    fun onBatteryUpdated(voltage: Float)       // New for battery display
}

// Data class sesuai ESP32 v2.4 EnhancedData struct
data class RoadsenseData(
    val speedKmh: Float = 0f,
    val odometerM: Float = 0f,
    val tripDistanceM: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val avgSpeedKmh: Float = 0f,
    val accelerationZ: Float = 0f,
    val systemState: Int = 0,          // 0=READY, 1=RUNNING, 2=STOPPED, 3=PAUSED
    val packetCount: Int = 0,
    val timestamp: String = "",
    val batteryVoltage: Float = 0f,
    val hour: Int = 0,
    val minute: Int = 0,
    val timeValid: Boolean = false,
    val viewMode: Int = 0,             // 0-5 sesuai ESP32
    val btConnected: Boolean = false,
    val dataStreaming: Boolean = false,
    val lastUpdateTime: Long = 0,
    val timeSyncPending: Boolean = false
)

class BluetoothHandler(
    private val context: Context
) {
    // Bluetooth components
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var _isConnected = false
    private var executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var callback: BluetoothCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Data tracking
    private var packetCount = 0
    private var lastDataTime = 0L
    private var connectionAttempts = 0
    private var autoReconnect = false

    // ESP32 Configuration
    companion object {
        // Connection states
        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
        const val STATE_CONNECTION_FAILED = 3

        // ESP32 Device Info (sesuai ESP32_Simpel_FIXED.txt)
        const val ESP32_DEVICE_NAME = "RoadsenseLogger-v2.4"
        const val ESP32_DEVICE_NAME_ALT = "RoadsenseLogger"  // Fallback
        const val UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB"

        // System States (sesuai ESP32 code)
        const val STATE_READY = 0
        const val STATE_RUNNING = 1
        const val STATE_STOPPED = 2
        const val STATE_PAUSED = 3

        // View Modes (sesuai ESP32 code)
        const val VIEW_DASHBOARD = 0
        const val VIEW_SPEED = 1
        const val VIEW_ODOMETER = 2
        const val VIEW_LOG_STATUS = 3
        const val VIEW_STATISTICS = 4
        const val VIEW_DISTANCE_DETAIL = 5
    }

    // Set callback
    fun setCallback(callback: BluetoothCallback) {
        this.callback = callback
    }

    // Enable/disable auto reconnect
    fun setAutoReconnect(enabled: Boolean) {
        autoReconnect = enabled
    }

    // ==================== CONNECTION FUNCTIONS ====================

    fun connectToESP32() {
        executor.execute {
            try {
                mainHandler.post {
                    callback?.onConnectionStateChanged(STATE_CONNECTING)
                    callback?.onMessageReceived("Connecting to ESP32...")
                }

                // Reset attempts
                connectionAttempts = 0

                // 1. Get BluetoothAdapter with proper handling
                val bluetoothAdapter = getBluetoothAdapter()
                if (bluetoothAdapter == null) {
                    mainHandler.post {
                        callback?.onError("Bluetooth not available on this device")
                    }
                    return@execute
                }

                // 2. Check Bluetooth permissions
                if (!checkBluetoothPermissions()) {
                    mainHandler.post {
                        callback?.onError("Bluetooth permissions not granted")
                    }
                    return@execute
                }

                // 3. Check if Bluetooth is enabled
                if (!isBluetoothEnabled(bluetoothAdapter)) {
                    mainHandler.post {
                        callback?.onError("Please enable Bluetooth")
                    }
                    return@execute
                }

                // 4. Find ESP32 device (paired)
                val esp32Device = findESP32Device(bluetoothAdapter)
                if (esp32Device == null) {
                    mainHandler.post {
                        callback?.onError("ESP32 not found. Please pair device first")
                    }
                    return@execute
                }

                // 5. Connect to device
                Timber.d("Connecting to: ${esp32Device.name ?: "Unknown"}")
                socket = esp32Device.createRfcommSocketToServiceRecord(UUID.fromString(UUID_STRING))

                // Set connection timeout
                socket?.connect()

                // 6. Setup streams
                inputStream = socket?.inputStream
                outputStream = socket?.outputStream
                _isConnected = true

                // Reset packet count on new connection
                packetCount = 0

                // 7. Connection successful
                mainHandler.post {
                    callback?.onConnectionStateChanged(STATE_CONNECTED)
                    callback?.onDeviceConnected(esp32Device.name ?: "ESP32")
                    callback?.onMessageReceived("Connected to ${esp32Device.name}")

                    // Auto sync time on first connection (sesuai ESP32 code)
                    syncTime()
                }

                // 8. Start reading data
                startReadingData()

                // 9. Start connection monitor
                startConnectionMonitor()

            } catch (e: IOException) {
                Timber.e("Connection failed: ${e.message}")
                mainHandler.post {
                    callback?.onError("Connection failed: ${e.message}")
                    callback?.onConnectionStateChanged(STATE_CONNECTION_FAILED)
                }
                disconnect()
            } catch (e: SecurityException) {
                Timber.e("Security exception: ${e.message}")
                mainHandler.post {
                    callback?.onError("Security permission error")
                }
                disconnect()
            }
        }
    }

    private fun getBluetoothAdapter(): BluetoothAdapter? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                bluetoothManager.adapter
            } else {
                @Suppress("DEPRECATION")
                BluetoothAdapter.getDefaultAdapter()
            }
        } catch (e: Exception) {
            Timber.e("Error getting BluetoothAdapter: ${e.message}")
            null
        }
    }

    private fun isBluetoothEnabled(adapter: BluetoothAdapter?): Boolean {
        if (adapter == null) return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            adapter.isEnabled
        } catch (e: SecurityException) {
            Timber.e("Security exception: ${e.message}")
            false
        }
    }

    private fun findESP32Device(adapter: BluetoothAdapter): BluetoothDevice? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                    return null
                }
            }

            val pairedDevices = adapter.bondedDevices
            pairedDevices.find { device ->
                device.name?.let { name ->
                    name.contains(ESP32_DEVICE_NAME, ignoreCase = true) ||
                            name.contains(ESP32_DEVICE_NAME_ALT, ignoreCase = true) ||
                            name.contains("Roadsense", ignoreCase = true)
                } ?: false
            }
        } catch (e: SecurityException) {
            Timber.e("Security exception: ${e.message}")
            null
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }

        return requiredPermissions.all { permission ->
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    // ==================== DATA PROCESSING ====================

    private fun startReadingData() {
        executor.execute {
            val buffer = ByteArray(1024)

            while (_isConnected) {
                try {
                    val bytes = inputStream?.read(buffer)
                    if (bytes != null && bytes > 0) {
                        val message = String(buffer, 0, bytes).trim()

                        // Handle multiple messages in one packet
                        val messages = message.split("\n", "\r")

                        messages.forEach { msg ->
                            if (msg.isNotBlank()) {
                                processIncomingData(msg.trim())
                            }
                        }

                        lastDataTime = System.currentTimeMillis()
                    }
                } catch (e: IOException) {
                    Timber.e("Read error: ${e.message}")
                    if (_isConnected) {
                        disconnect()
                    }
                    break
                } catch (e: Exception) {
                    Timber.e("Unexpected error: ${e.message}")
                }

                // Small delay to reduce CPU usage
                try {
                    Thread.sleep(10)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }

    private fun processIncomingData(data: String) {
        packetCount++

        Timber.d("<< $data")

        // Handle ACK/ERROR messages (from ESP32 or Mega)
        if (data.startsWith("ACK:") || data.startsWith("ERR:")) {
            mainHandler.post {
                callback?.onMessageReceived(data)
                handleSystemMessage(data)
            }
            return
        }

        // Handle GET_TIME request from ESP32 (auto sync)
        if (data == "GET_TIME" || data == "REQUEST_TIME_SYNC") {
            Timber.d("Time sync requested by ESP32")
            syncTime()
            return
        }

        // Handle RTC time response
        if (data.startsWith("TIME:")) {
            val timeStr = data.substring(5)
            mainHandler.post {
                callback?.onMessageReceived("RTC Time: $timeStr")
            }
            return
        }

        // Handle status response
        if (data.startsWith("STATUS:")) {
            // Parse status jika diperlukan
            return
        }

        // Handle battery info
        if (data.startsWith("BAT=")) {
            try {
                val voltage = data.substring(4).toFloatOrNull() ?: 0f
                mainHandler.post {
                    callback?.onBatteryUpdated(voltage)
                }
            } catch (e: Exception) {
                Timber.e("Error parsing battery: ${e.message}")
            }
            return
        }

        // Handle data format: RS2,ODO=...,TRIP=...,SPD=... (from Mega via ESP32)
        if (data.contains("ODO=") || data.startsWith("RS2")) {
            val roadsenseData = parseRoadsenseData(data).copy(packetCount = packetCount)

            mainHandler.post {
                callback?.onDataReceived(roadsenseData)

                // Update time display if valid
                if (roadsenseData.timeValid) {
                    callback?.onTimeUpdated(roadsenseData.hour, roadsenseData.minute)
                }

                // Update battery if available
                if (roadsenseData.batteryVoltage > 0) {
                    callback?.onBatteryUpdated(roadsenseData.batteryVoltage)
                }
            }
        }

        // Handle simple messages
        if (data.contains("SYNC") || data.contains("CONNECT") || data.contains("DISCONNECT")) {
            mainHandler.post {
                callback?.onMessageReceived(data)
            }
        }
    }

    private fun handleSystemMessage(message: String) {
        when {
            message.contains("STARTED", ignoreCase = true) -> {
                callback?.onMessageReceived("Logging STARTED")
            }
            message.contains("STOPPED", ignoreCase = true) -> {
                callback?.onMessageReceived("Logging STOPPED")
            }
            message.contains("PAUSED", ignoreCase = true) -> {
                callback?.onMessageReceived("Logging PAUSED")
            }
            message.contains("RESET_COMPLETE", ignoreCase = true) -> {
                callback?.onMessageReceived("Trip RESET complete")
            }
            message.contains("TIME_SYNC_COMPLETE", ignoreCase = true) -> {
                callback?.onMessageReceived("Time SYNC complete")
            }
            message.contains("WHEEL_SET", ignoreCase = true) -> {
                callback?.onMessageReceived("Wheel circumference updated")
            }
            message.contains("MPU_CALIBRATED", ignoreCase = true) -> {
                callback?.onMessageReceived("MPU6050 calibrated")
            }
        }
    }

    private fun parseRoadsenseData(data: String): RoadsenseData {
        var speedKmh = 0f
        var odometerM = 0f
        var tripDistanceM = 0f
        var maxSpeedKmh = 0f
        var avgSpeedKmh = 0f
        var accelerationZ = 0f
        var systemState = 0
        var timestamp = ""
        var batteryVoltage = 0f
        var hour = 0
        var minute = 0
        var timeValid = false
        var viewMode = 0
        var dataStreaming = true
        var timeSyncPending = false

        try {
            // Remove RS2, prefix if present
            val cleanData = if (data.startsWith("RS2,")) data.substring(4) else data

            // Split by comma
            val parts = cleanData.split(",")

            for (part in parts) {
                when {
                    // Basic measurements
                    part.startsWith("ODO=") -> {
                        odometerM = part.substring(4).toFloatOrNull() ?: 0f
                    }
                    part.startsWith("TRIP=") -> {
                        tripDistanceM = part.substring(5).toFloatOrNull() ?: 0f
                    }
                    part.startsWith("SPD=") -> {
                        speedKmh = part.substring(4).toFloatOrNull() ?: 0f
                    }
                    part.startsWith("MAX=") -> {
                        maxSpeedKmh = part.substring(4).toFloatOrNull() ?: 0f
                    }
                    part.startsWith("AVG=") -> {
                        avgSpeedKmh = part.substring(4).toFloatOrNull() ?: 0f
                    }
                    part.startsWith("Z=") -> {
                        accelerationZ = part.substring(2).toFloatOrNull() ?: 0f
                    }
                    part.startsWith("X=") -> {
                        // X acceleration (ignore for now)
                    }
                    part.startsWith("Y=") -> {
                        // Y acceleration (ignore for now)
                    }

                    // System state
                    part.startsWith("STATE=") -> {
                        systemState = part.substring(6).toIntOrNull() ?: 0
                    }

                    // Time from RTC (via Mega)
                    part.startsWith("TIME=") -> {
                        timestamp = part.substring(5)
                        if (timestamp.length >= 19) {
                            try {
                                // Format: YYYY-MM-DD HH:MM:SS
                                hour = timestamp.substring(11, 13).toInt()
                                minute = timestamp.substring(14, 16).toInt()
                                timeValid = true
                                timeSyncPending = false  // Sync completed
                            } catch (e: Exception) {
                                timeValid = false
                            }
                        }
                    }

                    // Battery voltage (from ESP32)
                    part.startsWith("BAT=") -> {
                        batteryVoltage = part.substring(4).toFloatOrNull() ?: 0f
                    }

                    // View mode (from ESP32 display)
                    part.startsWith("VIEW=") -> {
                        viewMode = part.substring(5).toIntOrNull() ?: 0
                    }

                    // Data streaming status
                    part.startsWith("STREAM=") -> {
                        dataStreaming = part.substring(7).toBoolean()
                    }

                    // Sync status
                    part.contains("SYNC") -> {
                        timeSyncPending = part.contains("PENDING", ignoreCase = true)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("Parse error: ${e.message}")
        }

        return RoadsenseData(
            speedKmh = speedKmh,
            odometerM = odometerM,
            tripDistanceM = tripDistanceM,
            maxSpeedKmh = maxSpeedKmh,
            avgSpeedKmh = avgSpeedKmh,
            accelerationZ = accelerationZ,
            systemState = systemState,
            packetCount = packetCount,
            timestamp = timestamp,
            batteryVoltage = batteryVoltage,
            hour = hour,
            minute = minute,
            timeValid = timeValid,
            viewMode = viewMode,
            btConnected = _isConnected,
            dataStreaming = dataStreaming,
            lastUpdateTime = System.currentTimeMillis(),
            timeSyncPending = timeSyncPending
        )
    }

    // ==================== PUBLIC COMMANDS ====================

    fun sendCommand(command: String) {
        executor.execute {
            try {
                if (_isConnected && outputStream != null) {
                    // Add newline as expected by ESP32
                    val commandWithNewline = "$command\n"
                    outputStream?.write(commandWithNewline.toByteArray())
                    outputStream?.flush()

                    Timber.d(">> $command")

                    mainHandler.post {
                        callback?.onMessageReceived("Sent: $command")
                    }
                } else {
                    mainHandler.post {
                        callback?.onMessageReceived("Not connected")
                    }
                }
            } catch (e: IOException) {
                Timber.e("Send failed: ${e.message}")
                mainHandler.post {
                    callback?.onError("Send failed: ${e.message}")
                }
                disconnect()
            }
        }
    }

    // Logging control commands
    fun startLogging() {
        sendCommand("START")
    }

    fun stopLogging() {
        sendCommand("STOP")
    }

    fun pauseLogging() {
        sendCommand("PAUSE")
    }

    fun resumeLogging() {
        sendCommand("RESUME")
    }

    fun resetTrip() {
        sendCommand("RESETTRIP")
    }

    fun resetOdometer() {
        sendCommand("RESET_ODO")
    }

    fun resetMaxSpeed() {
        sendCommand("RESETMAXSPEED")
    }

    // Time sync (from phone to ESP32 to Mega)
    fun syncTime() {
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())
        sendCommand("TIME=$currentTime")

        mainHandler.post {
            callback?.onMessageReceived("Syncing time: $currentTime")
        }
    }

    fun manualSync() {
        sendCommand("SYNC")
    }

    // View control
    fun nextView() {
        sendCommand("NEXTVIEW")
    }

    fun prevView() {
        sendCommand("PREVVIEW")
    }

    fun setView(viewNumber: Int) {
        if (viewNumber in 0..5) {
            sendCommand("VIEW=$viewNumber")
        }
    }

    // Device configuration
    fun setWheelCircumference(circumference: Float) {
        sendCommand("SETWHEEL=$circumference")
    }

    fun calibrateMPU() {
        sendCommand("CALIBRATE_MPU")
    }

    fun getStatus() {
        sendCommand("STATUS")
    }

    fun getData() {
        sendCommand("GETDATA")
    }

    fun getTime() {
        sendCommand("GET_TIME")
    }

    // ==================== CONNECTION MANAGEMENT ====================

    fun disconnect() {
        executor.execute {
            try {
                _isConnected = false
                autoReconnect = false

                inputStream?.close()
                outputStream?.close()
                socket?.close()

                mainHandler.post {
                    callback?.onConnectionStateChanged(STATE_DISCONNECTED)
                    callback?.onMessageReceived("Disconnected")
                    Timber.d("Bluetooth disconnected")
                }
            } catch (e: IOException) {
                Timber.e("Disconnect error: ${e.message}")
            }
        }
    }

    private fun startConnectionMonitor() {
        executor.execute {
            while (_isConnected) {
                try {
                    // Check if we haven't received data for a while
                    val now = System.currentTimeMillis()
                    if (lastDataTime > 0 && now - lastDataTime > 10000) { // 10 seconds timeout
                        Timber.w("No data received for 10 seconds, checking connection...")

                        // Try to send a ping
                        sendCommand("PING")

                        // Wait for response
                        Thread.sleep(2000)

                        if (now - lastDataTime > 15000) { // 15 seconds total
                            Timber.e("Connection timeout")
                            disconnect()
                            break
                        }
                    }

                    Thread.sleep(5000) // Check every 5 seconds
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }

    fun cleanup() {
        disconnect()
        executor.shutdown()
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
        Timber.d("BluetoothHandler cleanup complete")
    }

    // ==================== PUBLIC GETTERS ====================

    fun isConnected(): Boolean = _isConnected

    fun getPacketCount(): Int = packetCount

    fun isESP32Available(): Boolean {
        return try {
            val bluetoothAdapter = getBluetoothAdapter()
            if (bluetoothAdapter == null) return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }

            if (!isBluetoothEnabled(bluetoothAdapter)) return false

            val pairedDevices = bluetoothAdapter.bondedDevices
            pairedDevices.any { device ->
                device.name?.let { name ->
                    name.contains(ESP32_DEVICE_NAME, ignoreCase = true) ||
                            name.contains(ESP32_DEVICE_NAME_ALT, ignoreCase = true)
                } ?: false
            }
        } catch (e: SecurityException) {
            false
        }
    }

    fun getAvailableDevices(): List<String> {
        return try {
            val bluetoothAdapter = getBluetoothAdapter()
            if (bluetoothAdapter == null) return emptyList()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                    return emptyList()
                }
            }

            if (!isBluetoothEnabled(bluetoothAdapter)) return emptyList()

            bluetoothAdapter.bondedDevices.mapNotNull { it.name }
        } catch (e: SecurityException) {
            emptyList()
        }
    }
}