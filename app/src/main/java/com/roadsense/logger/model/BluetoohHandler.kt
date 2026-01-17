package com.roadsense.logger

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * BLUETOOTH HANDLER - PERMISSION SAFE VERSION
 * Proper permission handling with PermissionHelper
 */
class BluetoothHandler(
    private val context: Context,
    private val permissionHelper: PermissionHelper
) {

    companion object {
        // System States
        const val SYSTEM_READY = 0
        const val SYSTEM_RUNNING = 1
        const val SYSTEM_STOPPED = 2
        const val SYSTEM_PAUSED = 3

        // Connection States
        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
        const val STATE_ERROR = 3

        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val ESP32_NAME = "RoadsenseLogger-v2.3"
    }

    // Data structure for Roadsense
    data class RoadsenseData(
        var speedKmh: Float = 0f,
        var odometerM: Float = 0f,
        var tripDistanceM: Float = 0f,
        var accelerationZ: Float = 0f,
        var maxSpeedKmh: Float = 0f,
        var avgSpeedKmh: Float = 0f,
        var systemState: Int = SYSTEM_READY,
        var timeValid: Boolean = false,
        var timeHour: Int = 0,
        var timeMinute: Int = 0,
        var packetCount: Int = 0,
        var currentViewMode: Int = 0,
        var batteryVoltage: Float = 0f
    )

    interface BluetoothCallback {
        fun onConnectionStateChanged(state: Int)
        fun onDataReceived(data: RoadsenseData)
        fun onDeviceConnected(deviceName: String)
        fun onMessageReceived(message: String)
        fun onError(errorMessage: String)
    }

    // Core components
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val btThread = HandlerThread("BluetoothIO").apply { start() }
    private val uiHandler = Handler(Looper.getMainLooper())

    // Bluetooth objects
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    // State management
    private var currentState = STATE_DISCONNECTED
    private var callback: BluetoothCallback? = null
    private var currentDevice: BluetoothDevice? = null
    private val commandChannel = Channel<String>(Channel.UNLIMITED)

    // Data parsing
    private val dataBuffer = StringBuilder()
    private var currentData = RoadsenseData()

    init {
        Timber.d("🔧 BluetoothHandler initialized")
        initializeBluetooth()
        startCommandProcessor()
    }

    @Suppress("DEPRECATION")
    private fun initializeBluetooth() {
        if (!permissionHelper.hasBluetoothPermissions()) {
            Timber.w("🔒 No Bluetooth permissions, skipping initialization")
            return
        }

        try {
            bluetoothAdapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
                bluetoothManager?.adapter
            } else {
                BluetoothAdapter.getDefaultAdapter()
            }

            if (bluetoothAdapter == null) {
                Timber.e("❌ Bluetooth not available on this device")
                notifyError("Bluetooth tidak tersedia di device ini")
            } else {
                Timber.i("✅ Bluetooth adapter initialized")
            }
        } catch (e: SecurityException) {
            Timber.e("🔒 Security exception initializing Bluetooth: %s", e.message)
            notifyError("Bluetooth permission denied")
        }
    }

    // === PUBLIC METHODS ===

    fun setCallback(callback: BluetoothCallback) {
        Timber.d("🔗 Callback set")
        this.callback = callback
    }

    fun getPairedDevices(): Set<BluetoothDevice> {
        return try {
            if (!permissionHelper.hasBluetoothPermissions()) {
                Timber.w("🔒 Bluetooth permissions not granted")
                return emptySet()
            }

            val devices = bluetoothAdapter?.bondedDevices ?: emptySet()
            Timber.d("📋 Found %d paired devices", devices.size)
            devices
        } catch (e: SecurityException) {
            Timber.e("🔒 Security exception getting paired devices: %s", e.message)
            emptySet()
        } catch (e: Exception) {
            Timber.e("❌ Error getting paired devices: %s", e.message)
            emptySet()
        }
    }

    fun connectToESP32(): Boolean {
        if (!permissionHelper.hasBluetoothPermissions()) {
            Timber.e("🔒 Bluetooth permissions not granted")
            notifyError("Bluetooth permissions required")
            return false
        }

        val devices = getPairedDevices()
        devices.forEach { device ->
            try {
                val deviceName = getDeviceNameSafely(device)
                if (deviceName.contains(ESP32_NAME, ignoreCase = true)) {
                    Timber.i("🎯 ESP32 found: %s", deviceName)
                    connectToDevice(device)
                    return true
                }
            } catch (e: SecurityException) {
                Timber.e("🔒 Permission denied accessing device name: %s", e.message)
            } catch (e: Exception) {
                Timber.e("❌ Error checking device name: %s", e.message)
            }
        }
        Timber.w("⚠️ ESP32 not found in paired devices")
        return false
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        if (currentState == STATE_CONNECTING || currentState == STATE_CONNECTED) {
            Timber.w("⚠️ Already connecting/connected")
            return
        }

        // Check permissions before connecting
        if (!permissionHelper.hasBluetoothPermissions()) {
            Timber.e("🔒 Bluetooth permissions not granted")
            notifyError("Bluetooth permissions required")
            return
        }

        currentState = STATE_CONNECTING
        currentDevice = device
        notifyConnectionState()

        val deviceName = getDeviceNameSafely(device)
        Timber.i("🔗 Connecting to %s...", deviceName)

        scope.launch {
            try {
                // Connect in background thread with timeout
                val socket = withTimeout(8000) {
                    Timber.d("🔄 Creating RFCOMM socket...")
                    device.createRfcommSocketToServiceRecord(SPP_UUID).also {
                        Timber.d("🔄 Connecting socket...")
                        it.connect()
                    }
                }

                bluetoothSocket = socket
                inputStream = socket.inputStream
                outputStream = socket.outputStream

                currentState = STATE_CONNECTED
                notifyConnectionState()
                notifyDeviceConnected(deviceName)

                Timber.i("✅ Connected to %s", deviceName)

                // Start reading data
                startReading()

            } catch (e: TimeoutCancellationException) {
                Timber.e("⏱️ Connection timeout: %s", e.message)
                currentState = STATE_ERROR
                notifyConnectionState()
                notifyError("Connection timeout: ${e.message}")
            } catch (e: SecurityException) {
                Timber.e("🔒 Security exception during connection: %s", e.message)
                currentState = STATE_ERROR
                notifyConnectionState()
                notifyError("Bluetooth permission denied")
            } catch (e: Exception) {
                Timber.e("❌ Connection failed: %s", e.message)
                currentState = STATE_ERROR
                notifyConnectionState()
                notifyError("Connection failed: ${e.message}")
            }
        }
    }

    fun disconnect() {
        Timber.i("🔌 Disconnecting...")
        scope.launch {
            try {
                inputStream?.close()
                outputStream?.close()
                bluetoothSocket?.close()
                Timber.d("🔌 Streams and socket closed")
            } catch (e: IOException) {
                Timber.w("⚠️ Error during disconnect: %s", e.message)
            } finally {
                bluetoothSocket = null
                inputStream = null
                outputStream = null
                currentState = STATE_DISCONNECTED
                notifyConnectionState()
                Timber.i("🔌 Disconnected")
            }
        }
    }

    // Command methods
    fun startLogging() = sendCommand("START")
    fun stopLogging() = sendCommand("STOP")
    fun pauseLogging() = sendCommand("PAUSE")
    fun resetTrip() = sendCommand("RESETTRIP")
    fun nextViewMode() = sendCommand("NEXTVIEW")

    // === PRIVATE METHODS ===

    private fun sendCommand(command: String) {
        if (currentState != STATE_CONNECTED) {
            Timber.w("⚠️ Cannot send command: not connected")
            notifyError("Not connected")
            return
        }

        scope.launch {
            try {
                commandChannel.send(command)
                Timber.d("📤 Command queued: %s", command)
            } catch (e: Exception) {
                Timber.e("❌ Failed to queue command: %s", e.message)
                notifyError("Failed to queue command: ${e.message}")
            }
        }
    }

    private fun startCommandProcessor() {
        scope.launch {
            Timber.d("🚀 Command processor started")
            commandChannel.consumeEach { command ->
                try {
                    outputStream?.write("$command\n".toByteArray())
                    outputStream?.flush()
                    Timber.i("📤 Sent: %s", command)
                    delay(100) // Allow MCU to process
                } catch (e: Exception) {
                    Timber.e("❌ Command failed %s: %s", command, e.message)
                    notifyError("Command failed: ${e.message}")
                }
            }
        }
    }

    private fun startReading() {
        scope.launch {
            Timber.d("📖 Starting read loop...")
            val buffer = ByteArray(1024)

            try {
                while (currentState == STATE_CONNECTED) {
                    val bytes = inputStream?.read(buffer) ?: break
                    if (bytes > 0) {
                        val data = String(buffer, 0, bytes)
                        Timber.v("📥 Raw data (%d bytes): %s", bytes, data)
                        processIncomingData(data)
                    } else if (bytes == -1) {
                        Timber.w("⚠️ Connection lost: read return -1")
                        disconnectInternal()
                        break
                    }
                    delay(10)
                }
            } catch (e: IOException) {
                if (currentState == STATE_CONNECTED) {
                    currentState = STATE_DISCONNECTED
                    notifyConnectionState()
                    Timber.e("❌ Connection lost: %s", e.message)
                    notifyError("Connection lost: ${e.message}")
                }
            } catch (e: Exception) {
                Timber.e("❌ Read loop error: %s", e.message)
            }
        }
    }

    private fun processIncomingData(data: String) {
        dataBuffer.append(data)

        val lines = dataBuffer.toString().split("\n")
        if (lines.size > 1) {
            dataBuffer.clear()
            dataBuffer.append(lines.last())

            lines.dropLast(1).forEach { line ->
                if (line.isNotBlank()) {
                    parseDataLine(line.trim())
                }
            }
        }
    }

    private fun parseDataLine(line: String) {
        Timber.v("📝 Parsing line: %s", line)

        when {
            line.startsWith("RS2") -> {
                Timber.d("📊 RS2 data received")
                parseRS2Data(line)
                notifyDataReceived()
            }
            line.startsWith("ACK:") -> {
                val ackMessage = line.substring(4)
                Timber.i("✅ ACK: %s", ackMessage)

                // Handle TRIP_RESET secara khusus
                if (ackMessage == "TRIP_RESET_COMPLETE") {
                    currentData.tripDistanceM = 0f
                    Timber.i("✅ Trip reset confirmed on device")
                }

                notifyMessageReceived(ackMessage)
            }
            line.startsWith("ERR:") -> {
                Timber.e("❌ ERROR: %s", line.substring(4))
                notifyError(line.substring(4))
            }
            line.startsWith("DATA:") -> {
                Timber.d("📦 DATA response")
                parseDataResponse(line.substring(5))
                notifyDataReceived()
            }
            line.startsWith("VIEW=") -> {
                Timber.d("👁️ View changed: %s", line)
                try {
                    val viewNum = line.substring(5).toIntOrNull()
                    if (viewNum != null) {
                        currentData.currentViewMode = viewNum - 1
                        notifyDataReceived()
                    }
                } catch (e: Exception) {
                    Timber.e("❌ Error parsing VIEW: %s", e.message)
                }
            }
            else -> {
                Timber.v("📄 Raw: %s", line)
            }
        }
    }

    private fun parseRS2Data(data: String) {
        try {
            val parts = data.split(",")

            parts.forEach { part ->
                when {
                    part.startsWith("ODO=") -> currentData.odometerM = part.substring(4).toFloatOrNull() ?: 0f
                    part.startsWith("TRIP=") -> currentData.tripDistanceM = part.substring(5).toFloatOrNull() ?: 0f
                    part.startsWith("SPD=") -> currentData.speedKmh = part.substring(4).toFloatOrNull() ?: 0f
                    part.startsWith("Z=") -> currentData.accelerationZ = part.substring(2).toFloatOrNull() ?: 0f
                    part.startsWith("MAX=") -> currentData.maxSpeedKmh = part.substring(4).toFloatOrNull() ?: 0f
                    part.startsWith("AVG=") -> currentData.avgSpeedKmh = part.substring(4).toFloatOrNull() ?: 0f
                    part.startsWith("STATE=") -> currentData.systemState = part.substring(6).toIntOrNull() ?: SYSTEM_READY
                    part.contains("TIME=") -> parseTime(part)
                }
            }

            currentData.packetCount++
            Timber.v("📊 Parsed: SPD=%.1f, ODO=%.1f", currentData.speedKmh, currentData.odometerM)

        } catch (e: Exception) {
            Timber.e("❌ Parse error: %s", e.message)
        }
    }

    private fun parseDataResponse(data: String) {
        try {
            val parts = data.split(",")
            parts.forEach { part ->
                when {
                    part.startsWith("ODO=") -> currentData.odometerM = part.substring(4).toFloatOrNull() ?: 0f
                    part.startsWith("TRIP=") -> currentData.tripDistanceM = part.substring(5).toFloatOrNull() ?: 0f
                    part.startsWith("SPD=") -> currentData.speedKmh = part.substring(4).toFloatOrNull() ?: 0f
                    part.startsWith("STATE=") -> currentData.systemState = part.substring(6).toIntOrNull() ?: SYSTEM_READY
                }
            }
        } catch (e: Exception) {
            Timber.e("❌ Parse DATA error: %s", e.message)
        }
    }

    private fun parseTime(timePart: String) {
        try {
            val timeStr = timePart.substringAfter("TIME=")
            if (timeStr.length >= 19) {
                val hour = timeStr.substring(11, 13).toIntOrNull()
                val minute = timeStr.substring(14, 16).toIntOrNull()

                if (hour != null && minute != null) {
                    currentData.timeHour = hour
                    currentData.timeMinute = minute
                    currentData.timeValid = true
                    Timber.d("⏰ Time parsed: %02d:%02d", hour, minute)
                }
            }
        } catch (e: Exception) {
            currentData.timeValid = false
            Timber.e("❌ Time parse error: %s", e.message)
        }
    }

    // === NOTIFICATION METHODS ===

    private fun notifyConnectionState() {
        uiHandler.post {
            callback?.onConnectionStateChanged(currentState)
        }
    }

    private fun notifyDeviceConnected(deviceName: String) {
        uiHandler.post {
            callback?.onDeviceConnected(deviceName)
        }
    }

    private fun notifyDataReceived() {
        uiHandler.post {
            callback?.onDataReceived(currentData)
        }
    }

    private fun notifyMessageReceived(message: String) {
        uiHandler.post {
            callback?.onMessageReceived(message)
        }
    }

    private fun notifyError(error: String) {
        uiHandler.post {
            callback?.onError(error)
        }
    }

    // Cleanup
    fun cleanup() {
        Timber.d("🧹 Cleaning up BluetoothHandler")
        disconnect()
        scope.cancel()
        btThread.quitSafely()
    }

    private fun disconnectInternal() {
        scope.launch {
            try {
                inputStream?.close()
                outputStream?.close()
                bluetoothSocket?.close()
            } catch (e: IOException) {
                // Ignore
            } finally {
                bluetoothSocket = null
                inputStream = null
                outputStream = null
                currentState = STATE_DISCONNECTED
                notifyConnectionState()
            }
        }
    }

    // Safe method to get device name with permission checking
    @SuppressLint("MissingPermission")
    private fun getDeviceNameSafely(device: BluetoothDevice): String {
        return try {
            if (permissionHelper.hasBluetoothPermissions()) {
                device.name ?: "Unknown"
            } else {
                "Unknown"
            }
        } catch (e: SecurityException) {
            Timber.e("🔒 Security exception getting device name: %s", e.message)
            "Unknown"
        } catch (e: Exception) {
            Timber.e("❌ Error getting device name: %s", e.message)
            "Unknown"
        }
    }
}