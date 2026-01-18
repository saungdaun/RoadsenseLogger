package com.roadsense.logger.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadsense.logger.core.bluetooth.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class SharedViewModel : ViewModel() {

    // ============ BLUETOOTH STATES ============
    private val _connectionStatus = MutableLiveData<String>("Disconnected")
    val connectionStatus: LiveData<String> = _connectionStatus

    private val _connectionState = MutableLiveData<Int>(BluetoothHandler.STATE_DISCONNECTED)
    val connectionState: LiveData<Int> = _connectionState

    private val _deviceName = MutableLiveData<String>("No Device")
    val deviceName: LiveData<String> = _deviceName

    private val _isConnected = MutableLiveData<Boolean>(false)
    val isConnected: LiveData<Boolean> = _isConnected

    private val _isLogging = MutableLiveData<Boolean>(false)
    val isLogging: LiveData<Boolean> = _isLogging

    // ============ ROADSENSE DATA ============
    private val _speed = MutableLiveData<Float>(0f)
    val speed: LiveData<Float> = _speed

    private val _odometer = MutableLiveData<Float>(0f)
    val odometer: LiveData<Float> = _odometer

    private val _tripDistance = MutableLiveData<Float>(0f)
    val tripDistance: LiveData<Float> = _tripDistance

    private val _maxSpeed = MutableLiveData<Float>(0f)
    val maxSpeed: LiveData<Float> = _maxSpeed

    private val _avgSpeed = MutableLiveData<Float>(0f)
    val avgSpeed: LiveData<Float> = _avgSpeed

    private val _accelZ = MutableLiveData<Float>(0f)
    val accelZ: LiveData<Float> = _accelZ

    private val _systemState = MutableLiveData<Int>(0)
    val systemState: LiveData<Int> = _systemState

    private val _packetCount = MutableLiveData<Int>(0)
    val packetCount: LiveData<Int> = _packetCount

    private val _batteryVoltage = MutableLiveData<Float>(0f)
    val batteryVoltage: LiveData<Float> = _batteryVoltage

    private val _timestamp = MutableLiveData<String>("--:--")
    val timestamp: LiveData<String> = _timestamp

    private val _batteryPercent = MutableLiveData<Int>(0)
    val batteryPercent: LiveData<Int> = _batteryPercent

    // ============ VIEW MODES ============
    private val _currentView = MutableLiveData<Int>(0)
    val currentView: LiveData<Int> = _currentView

    private val _tripTime = MutableLiveData<String>("00:00:00")
    val tripTime: LiveData<String> = _tripTime

    private val _dataStreaming = MutableLiveData<Boolean>(false)
    val dataStreaming: LiveData<Boolean> = _dataStreaming

    private val _timeSyncPending = MutableLiveData<Boolean>(false)
    val timeSyncPending: LiveData<Boolean> = _timeSyncPending

    // ============ STATISTICS ============
    private var tripStartTime: Long = 0
    private var isTripActive = false
    private var totalSpeedSum = 0f
    private var totalSpeedSamples = 0
    private var lastUpdateTime: Long = 0

    // Bluetooth handler
    private var bluetoothHandler: BluetoothHandler? = null

    fun initializeBluetoothHandler(handler: BluetoothHandler) {
        bluetoothHandler = handler

        // Setup callback
        handler.setCallback(object : BluetoothCallback {
            override fun onConnectionStateChanged(state: Int) {
                viewModelScope.launch {
                    _connectionState.value = state

                    when (state) {
                        BluetoothHandler.STATE_CONNECTED -> {
                            _isConnected.value = true
                            _connectionStatus.value = "Connected"
                            Timber.d("Bluetooth CONNECTED")
                        }
                        BluetoothHandler.STATE_DISCONNECTED -> {
                            _isConnected.value = false
                            _connectionStatus.value = "Disconnected"
                            _isLogging.value = false
                            stopTripTimer()
                            Timber.d("Bluetooth DISCONNECTED")
                        }
                        BluetoothHandler.STATE_CONNECTING -> {
                            _connectionStatus.value = "Connecting..."
                            Timber.d("Bluetooth CONNECTING")
                        }
                        BluetoothHandler.STATE_CONNECTION_FAILED -> {
                            _connectionStatus.value = "Connection Failed"
                            Timber.d("Bluetooth CONNECTION FAILED")
                        }
                    }
                }
            }

            override fun onDataReceived(data: RoadsenseData) {
                viewModelScope.launch {
                    updateRoadsenseData(data)
                }
            }

            override fun onDeviceConnected(deviceName: String) {
                viewModelScope.launch {
                    _deviceName.value = deviceName
                    Timber.d("Device connected: $deviceName")
                }
            }

            override fun onMessageReceived(message: String) {
                viewModelScope.launch {
                    _connectionStatus.value = message
                    Timber.d("Message: $message")

                    // Auto update system state based on messages
                    when {
                        message.contains("STARTED", ignoreCase = true) -> {
                            _isLogging.value = true
                            _systemState.value = BluetoothHandler.STATE_RUNNING
                            startTripTimer()
                        }
                        message.contains("STOPPED", ignoreCase = true) -> {
                            _isLogging.value = false
                            _systemState.value = BluetoothHandler.STATE_STOPPED
                            stopTripTimer()
                        }
                        message.contains("PAUSED", ignoreCase = true) -> {
                            _isLogging.value = false
                            _systemState.value = BluetoothHandler.STATE_PAUSED
                            pauseTripTimer()
                        }
                        message.contains("READY", ignoreCase = true) -> {
                            _systemState.value = BluetoothHandler.STATE_READY
                        }
                        message.contains("SYNC", ignoreCase = true) -> {
                            _timeSyncPending.value = message.contains("PENDING", ignoreCase = true)
                        }
                    }
                }
            }

            override fun onError(errorMessage: String) {
                viewModelScope.launch {
                    _connectionStatus.value = "Error: $errorMessage"
                    Timber.e("Bluetooth error: $errorMessage")
                }
            }

            override fun onTimeUpdated(hour: Int, minute: Int) {
                viewModelScope.launch {
                    val timeStr = String.format("%02d:%02d", hour, minute)
                    _timestamp.value = timeStr
                    _timeSyncPending.value = false  // Sync completed
                }
            }

            override fun onBatteryUpdated(voltage: Float) {
                viewModelScope.launch {
                    _batteryVoltage.value = voltage
                    // Convert to percentage (assuming 3.7V nominal, 4.2V max, 3.3V min)
                    val percent = when {
                        voltage >= 4.1f -> 100
                        voltage >= 3.9f -> 80
                        voltage >= 3.7f -> 60
                        voltage >= 3.5f -> 40
                        voltage >= 3.3f -> 20
                        else -> 10
                    }
                    _batteryPercent.value = percent
                }
            }
        })

        // Enable auto reconnect
        handler.setAutoReconnect(true)
    }

    private fun updateRoadsenseData(data: RoadsenseData) {
        lastUpdateTime = System.currentTimeMillis()

        // Update basic data
        _speed.value = data.speedKmh
        _odometer.value = data.odometerM
        _tripDistance.value = data.tripDistanceM
        _accelZ.value = data.accelerationZ
        _packetCount.value = data.packetCount

        // Update system state (priority: from data > from message)
        if (data.systemState in 0..3) {
            _systemState.value = data.systemState

            // Update logging state based on system state
            when (data.systemState) {
                BluetoothHandler.STATE_RUNNING -> {
                    _isLogging.value = true
                    if (!isTripActive) startTripTimer()
                }
                BluetoothHandler.STATE_STOPPED,
                BluetoothHandler.STATE_PAUSED,
                BluetoothHandler.STATE_READY -> {
                    _isLogging.value = false
                }
            }
        }

        // Update view mode from ESP32
        if (data.viewMode in 0..5) {
            _currentView.value = data.viewMode
        }

        // Update max speed
        val currentSpeed = data.speedKmh
        if (currentSpeed > (_maxSpeed.value ?: 0f)) {
            _maxSpeed.value = currentSpeed
        }

        // Update average speed (rolling average)
        if (currentSpeed > 0.5f) { // Ignore very low speeds
            totalSpeedSum += currentSpeed
            totalSpeedSamples++
            _avgSpeed.value = totalSpeedSum / totalSpeedSamples
        }

        // Update timestamp
        if (data.timeValid) {
            val timeStr = String.format("%02d:%02d", data.hour, data.minute)
            _timestamp.value = timeStr
        }

        // Update battery
        if (data.batteryVoltage > 0) {
            _batteryVoltage.value = data.batteryVoltage
        }

        // Update streaming status
        _dataStreaming.value = data.dataStreaming

        // Update sync status
        _timeSyncPending.value = data.timeSyncPending

        // If not receiving data for 3 seconds, set streaming to false
        viewModelScope.launch {
            delay(3000)
            if (System.currentTimeMillis() - lastUpdateTime > 3000) {
                _dataStreaming.value = false
            }
        }
    }

    // ============ BLUETOOTH CONNECTION ============

    fun connectToESP32() {
        _connectionStatus.value = "Connecting..."
        bluetoothHandler?.connectToESP32()
        Timber.d("Connecting to ESP32...")
    }

    fun disconnect() {
        bluetoothHandler?.disconnect()
        _connectionStatus.value = "Disconnecting..."
        _isConnected.value = false
        _deviceName.value = "No Device"
        _isLogging.value = false
        stopTripTimer()
        Timber.d("Disconnecting...")
    }

    fun scanDevices() {
        _connectionStatus.value = "Scanning for devices..."
        Timber.d("Scanning for devices...")

        viewModelScope.launch {
            delay(2000)

            val devices = bluetoothHandler?.getAvailableDevices() ?: emptyList()
            if (devices.isNotEmpty()) {
                _connectionStatus.value = "Found ${devices.size} devices"
            } else {
                _connectionStatus.value = "No devices found"
            }
            Timber.d("Scan complete: $devices")
        }
    }

    // ============ VIEW CONTROL ============

    fun switchToNextView() {
        val current = _currentView.value ?: 0
        val next = (current + 1) % 6  // 6 view modes sesuai ESP32
        _currentView.value = next

        bluetoothHandler?.nextView()
        Timber.d("View switched to: $next (${getCurrentViewName()})")
    }

    fun switchToPrevView() {
        val current = _currentView.value ?: 0
        val prev = if (current == 0) 5 else current - 1
        _currentView.value = prev

        bluetoothHandler?.prevView()
        Timber.d("View switched to: $prev (${getCurrentViewName()})")
    }

    fun setView(viewNumber: Int) {
        if (viewNumber in 0..5) {
            _currentView.value = viewNumber
            bluetoothHandler?.setView(viewNumber)
            Timber.d("View set to: $viewNumber (${getCurrentViewName()})")
        }
    }

    fun getCurrentViewName(): String {
        return when (_currentView.value) {
            BluetoothHandler.VIEW_DASHBOARD -> "Dashboard"
            BluetoothHandler.VIEW_SPEED -> "Speed"
            BluetoothHandler.VIEW_ODOMETER -> "Odometer"
            BluetoothHandler.VIEW_LOG_STATUS -> "Log Status"
            BluetoothHandler.VIEW_STATISTICS -> "Statistics"
            BluetoothHandler.VIEW_DISTANCE_DETAIL -> "Distance Detail"
            else -> "Dashboard"
        }
    }

    // ============ BLUETOOTH COMMANDS ============

    fun startLogging() {
        if (_isConnected.value == true) {
            bluetoothHandler?.startLogging()
            _isLogging.value = true
            startTripTimer()
            Timber.d("START logging command")
        } else {
            _connectionStatus.value = "Not connected!"
            Timber.w("Cannot start: not connected")
        }
    }

    fun stopLogging() {
        bluetoothHandler?.stopLogging()
        _isLogging.value = false
        stopTripTimer()
        Timber.d("STOP logging command")
    }

    fun pauseLogging() {
        bluetoothHandler?.pauseLogging()
        _isLogging.value = false
        pauseTripTimer()
        Timber.d("PAUSE logging command")
    }

    fun resumeLogging() {
        bluetoothHandler?.resumeLogging()
        _isLogging.value = true
        resumeTripTimer()
        Timber.d("RESUME logging command")
    }

    fun resetTrip() {
        bluetoothHandler?.resetTrip()

        // Reset trip statistics
        _tripDistance.value = 0f
        _maxSpeed.value = 0f
        _avgSpeed.value = 0f
        totalSpeedSum = 0f
        totalSpeedSamples = 0

        // Reset trip timer
        tripStartTime = System.currentTimeMillis()
        _tripTime.value = "00:00:00"
        Timber.d("RESET trip command")
    }

    fun resetOdometer() {
        bluetoothHandler?.resetOdometer()
        _odometer.value = 0f
        Timber.d("RESET odometer command")
    }

    fun resetMaxSpeed() {
        bluetoothHandler?.resetMaxSpeed()
        _maxSpeed.value = 0f
        Timber.d("RESET max speed command")
    }

    fun syncTime() {
        if (_isConnected.value == true) {
            bluetoothHandler?.syncTime()
            _timeSyncPending.value = true
            _connectionStatus.value = "Syncing time..."
            Timber.d("SYNC time command")
        } else {
            _connectionStatus.value = "Cannot sync: not connected"
            Timber.w("Cannot sync: not connected")
        }
    }

    fun manualSync() {
        if (_isConnected.value == true) {
            bluetoothHandler?.manualSync()
            _timeSyncPending.value = true
            _connectionStatus.value = "Manual sync requested..."
            Timber.d("MANUAL SYNC command")
        }
    }

    fun calibrateMPU() {
        bluetoothHandler?.calibrateMPU()
        Timber.d("CALIBRATE MPU command")
    }

    fun setWheelCircumference(circumference: Float) {
        bluetoothHandler?.setWheelCircumference(circumference)
        Timber.d("SET wheel circumference: $circumference")
    }

    fun getStatus() {
        bluetoothHandler?.getStatus()
        Timber.d("GET STATUS command")
    }

    fun getData() {
        bluetoothHandler?.getData()
        Timber.d("GET DATA command")
    }

    // ============ TRIP TIMER ============

    private fun startTripTimer() {
        if (!isTripActive) {
            tripStartTime = System.currentTimeMillis()
            isTripActive = true

            viewModelScope.launch {
                while (isTripActive) {
                    updateTripTime()
                    delay(1000)
                }
            }
            Timber.d("Trip timer STARTED")
        }
    }

    private fun stopTripTimer() {
        isTripActive = false
        updateTripTime()
        Timber.d("Trip timer STOPPED")
    }

    private fun pauseTripTimer() {
        isTripActive = false
        Timber.d("Trip timer PAUSED")
    }

    private fun resumeTripTimer() {
        if (!isTripActive) {
            startTripTimer()
        }
        Timber.d("Trip timer RESUMED")
    }

    private fun updateTripTime() {
        if (tripStartTime > 0) {
            val elapsed = System.currentTimeMillis() - tripStartTime
            val hours = elapsed / 3600000
            val minutes = (elapsed % 3600000) / 60000
            val seconds = (elapsed % 60000) / 1000

            _tripTime.value = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    // ============ UTILITY FUNCTIONS ============

    fun getSystemStateName(): String {
        return when (_systemState.value) {
            BluetoothHandler.STATE_READY -> "READY"
            BluetoothHandler.STATE_RUNNING -> "RUNNING"
            BluetoothHandler.STATE_STOPPED -> "STOPPED"
            BluetoothHandler.STATE_PAUSED -> "PAUSED"
            else -> "UNKNOWN"
        }
    }

    fun getBatteryStatus(): String {
        return when (_batteryPercent.value ?: 0) {
            in 80..100 -> "High"
            in 50..79 -> "Medium"
            in 20..49 -> "Low"
            else -> "Critical"
        }
    }

    fun getConnectionQuality(): String {
        return if (_dataStreaming.value == true) {
            "Good"
        } else if (_isConnected.value == true) {
            "Connected (No Data)"
        } else {
            "Disconnected"
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothHandler?.cleanup()
        Timber.d("SharedViewModel cleared")
    }
}