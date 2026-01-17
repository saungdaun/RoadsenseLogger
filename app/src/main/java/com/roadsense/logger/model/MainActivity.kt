package com.roadsense.logger

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import timber.log.Timber

class MainActivity : AppCompatActivity(), BluetoothHandler.BluetoothCallback {

    // --- UI Components ---
    private lateinit var tvConnectionStatus: TextView
    private lateinit var tvDeviceName: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvOdometer: TextView
    private lateinit var tvTrip: TextView
    private lateinit var tvSystemState: TextView
    private lateinit var tvMaxSpeed: TextView
    private lateinit var tvAvgSpeed: TextView
    private lateinit var tvAccelZ: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvPackets: TextView
    private lateinit var tvViewMode: TextView

    private lateinit var btnConnect: Button
    private lateinit var btnDisconnect: Button
    private lateinit var btnScan: Button
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnPause: Button
    private lateinit var btnResetTrip: Button
    private lateinit var btnNextView: Button

    private lateinit var deviceListView: ListView

    // --- Bluetooth Logic ---
    private lateinit var bluetoothHandler: BluetoothHandler
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private val deviceItems = mutableListOf<String>()
    private val deviceMap = mutableMapOf<String, BluetoothDevice>()

    // Connection state
    private var isConnected = false

    // Permission helper
    private lateinit var permissionHelper: PermissionHelper

    companion object {
        private const val REQUEST_BLUETOOTH_PERM = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Timber.d("🎬 MainActivity created")

        // Initialize permission helper
        permissionHelper = PermissionHelper(this)

        // 1. Inisialisasi Bluetooth Handler
        bluetoothHandler = BluetoothHandler(this, permissionHelper)
        bluetoothHandler.setCallback(this)

        // 2. Setup UI
        initializeViews()
        setupDeviceList()
        setupListeners()

        // 3. Cek Izin
        if (!checkPermissions()) {
            Timber.i("🔒 Requesting permissions")
            requestPermissions()
        } else {
            Timber.i("✅ Permissions granted")
        }
    }

    private fun initializeViews() {
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus)
        tvDeviceName       = findViewById(R.id.tvDeviceName)
        tvSpeed            = findViewById(R.id.tvSpeedValue)
        tvOdometer         = findViewById(R.id.tvOdoValue)
        tvTrip             = findViewById(R.id.tvTripValue)
        tvSystemState      = findViewById(R.id.tvSystemState)
        tvMaxSpeed         = findViewById(R.id.tvMaxSpeedValue)
        tvAvgSpeed         = findViewById(R.id.tvAvgSpeedValue)
        tvAccelZ           = findViewById(R.id.tvAccelZValue)
        tvTime             = findViewById(R.id.tvTimeValue)
        tvPackets          = findViewById(R.id.tvPacketCount)
        tvViewMode         = findViewById(R.id.tvViewMode)

        btnConnect         = findViewById(R.id.btnConnect)
        btnDisconnect      = findViewById(R.id.btnDisconnect)
        btnScan            = findViewById(R.id.btnScan)
        btnStart           = findViewById(R.id.btnStart)
        btnStop            = findViewById(R.id.btnStop)
        btnPause           = findViewById(R.id.btnPause)
        btnResetTrip       = findViewById(R.id.btnResetTrip)
        btnNextView        = findViewById(R.id.btnNextView)
        deviceListView     = findViewById(R.id.deviceListView)

        updateConnectionUI(false)
        updateControlButtons(false)

        Timber.d("✅ UI components initialized")
    }

    private fun setupDeviceList() {
        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceItems)
        deviceListView.adapter = deviceListAdapter
        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val item = deviceItems[position]
            val device = deviceMap[item]
            device?.let {
                Timber.i("📱 Selected device")
                connectToDevice(it)
            }
        }
    }

    private fun setupListeners() {
        btnConnect.setOnClickListener {
            Timber.d("🔗 Connect button clicked")
            connectToESP32()
        }

        btnDisconnect.setOnClickListener {
            Timber.d("🔌 Disconnect button clicked")
            bluetoothHandler.disconnect()
        }

        btnScan.setOnClickListener {
            Timber.d("🔍 Scan button clicked")
            scanPairedDevices()
        }

        btnStart.setOnClickListener {
            Timber.d("▶️ Start button clicked")
            bluetoothHandler.startLogging()
        }

        btnStop.setOnClickListener {
            Timber.d("⏹️ Stop button clicked")
            bluetoothHandler.stopLogging()
        }

        btnPause.setOnClickListener {
            Timber.d("⏸️ Pause button clicked")
            bluetoothHandler.pauseLogging()
        }

        btnResetTrip.setOnClickListener {
            Timber.d("🔄 Reset Trip button clicked")
            showResetTripDialog()
        }

        btnNextView.setOnClickListener {
            Timber.d("👁️ Next View button clicked")
            bluetoothHandler.nextViewMode()
        }
    }

    private fun showResetTripDialog() {
        if (!isConnected) {
            showToast("Not connected to device")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Reset Trip")
            .setMessage("Are you sure you want to reset trip distance to 0?")
            .setPositiveButton("RESET") { _, _ ->
                bluetoothHandler.resetTrip()
                showToast("Trip reset command sent")
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun connectToESP32() {
        if (!checkPermissions()) {
            requestPermissions()
            return
        }
        if (!bluetoothHandler.connectToESP32()) {
            showToast("ESP32 not found in paired devices.")
            Timber.w("⚠️ ESP32 not found in paired devices")
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Timber.i("🔗 Connecting to device")
        bluetoothHandler.connectToDevice(device)
    }

    private fun scanPairedDevices() {
        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        deviceItems.clear()
        deviceMap.clear()

        val devices = bluetoothHandler.getPairedDevices()
        if (devices.isEmpty()) {
            deviceItems.add("No paired devices found.")
            Timber.i("📋 No paired devices found")
        } else {
            devices.forEach { device ->
                // Gunakan helper untuk mendapatkan device name dengan aman
                val name = getDeviceNameSafely(device)
                val address = device.address
                val item = "$name\n$address"
                deviceItems.add(item)
                deviceMap[item] = device
            }
            Timber.i("📋 Found ${devices.size} paired devices")
        }
        deviceListAdapter.notifyDataSetChanged()
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceNameSafely(device: BluetoothDevice): String {
        return try {
            if (checkPermissions()) {
                device.name ?: "Unknown"
            } else {
                "Unknown"
            }
        } catch (e: SecurityException) {
            Timber.e("🔒 Permission denied getting device name: %s", e.message)
            "Unknown"
        } catch (e: Exception) {
            Timber.e("❌ Error getting device name: %s", e.message)
            "Unknown"
        }
    }

    // --- Implementasi BluetoothCallback ---

    override fun onConnectionStateChanged(state: Int) {
        isConnected = (state == BluetoothHandler.STATE_CONNECTED)
        Timber.i("🔗 Connection state: %d (connected: %b)", state, isConnected)
        updateConnectionUI(isConnected)
        updateControlButtons(isConnected)
    }

    override fun onDataReceived(data: BluetoothHandler.RoadsenseData) {
        runOnUiThread {
            tvSpeed.text    = String.format("%.1f km/h", data.speedKmh)
            tvOdometer.text = String.format("%.1f m", data.odometerM)
            tvTrip.text     = String.format("%.1f m", data.tripDistanceM)
            tvMaxSpeed.text = String.format("%.1f km/h", data.maxSpeedKmh)
            tvAvgSpeed.text = String.format("%.1f km/h", data.avgSpeedKmh)
            tvAccelZ.text   = String.format("%.2f g", data.accelerationZ)
            tvPackets.text  = data.packetCount.toString()
            tvViewMode.text = "View ${data.currentViewMode + 1}/6"

            tvSystemState.text = when(data.systemState) {
                BluetoothHandler.SYSTEM_READY -> "READY"
                BluetoothHandler.SYSTEM_RUNNING -> "RUNNING"
                BluetoothHandler.SYSTEM_PAUSED -> "PAUSED"
                else -> "STOPPED"
            }

            if (data.timeValid) {
                tvTime.text = String.format("%02d:%02d", data.timeHour, data.timeMinute)
            } else {
                tvTime.text = "--:--"
            }

            Timber.v("📊 Data updated: SPD=%.1f, ODO=%.1f", data.speedKmh, data.odometerM)
        }
    }

    override fun onDeviceConnected(deviceName: String) {
        runOnUiThread {
            tvDeviceName.text = deviceName
            showToast("Connected to $deviceName")
            Timber.i("✅ Connected to: %s", deviceName)
        }
    }

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            if (message.startsWith("STARTED") ||
                message.startsWith("STOPPED") ||
                message.startsWith("PAUSED") ||
                message.startsWith("TRIP_RESET")) {
                showToast(message)
                Timber.i("📨 Message: %s", message)
            }
        }
    }

    override fun onError(errorMessage: String) {
        runOnUiThread {
            showToast("Error: $errorMessage")
            Timber.e("❌ Error: %s", errorMessage)
        }
    }

    // --- Permission Handling ---
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_BLUETOOTH_PERM) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                showToast("Permissions granted")
                Timber.i("✅ All permissions granted")
                // Refresh device list if permissions just granted
                scanPairedDevices()
            } else {
                showToast("Permissions denied")
                Timber.w("⚠️ Some permissions denied")
            }
        }
    }

    // --- Helper UI ---

    private fun updateConnectionUI(connected: Boolean) {
        runOnUiThread {
            tvConnectionStatus.text = if (connected) "CONNECTED" else "DISCONNECTED"
            tvConnectionStatus.setTextColor(ContextCompat.getColor(this,
                if (connected) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
            btnDisconnect.isEnabled = connected
            btnConnect.isEnabled = !connected
            Timber.d("🔗 UI updated: connected=%b", connected)
        }
    }

    private fun updateControlButtons(connected: Boolean) {
        runOnUiThread {
            btnStart.isEnabled = connected
            btnStop.isEnabled = connected
            btnPause.isEnabled = connected
            btnResetTrip.isEnabled = connected
            btnNextView.isEnabled = connected
        }
    }

    private fun checkPermissions(): Boolean = permissionHelper.hasBluetoothPermissions()

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, permissionHelper.getRequiredPermissions(), REQUEST_BLUETOOTH_PERM)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        Timber.d("🍞 Toast: %s", msg)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("👋 MainActivity destroyed")
        bluetoothHandler.cleanup()
    }
}