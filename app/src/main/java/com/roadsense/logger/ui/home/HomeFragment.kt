package com.roadsense.logger.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.roadsense.logger.R
import com.roadsense.logger.core.bluetooth.BluetoothHandler
import com.roadsense.logger.databinding.FragmentHomeBinding
import com.roadsense.logger.ui.viewmodels.SharedViewModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    // View mode containers
    private lateinit var viewContainers: Array<View>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.d("HomeFragment created")

        setupViewContainers()
        setupButtonListeners()
        setupObservers()
        setupTimeUpdater()
        updateButtonStates()
    }

    private fun setupViewContainers() {
        // Initialize view containers array
        viewContainers = arrayOf(
            binding.viewDashboard,
            binding.viewSpeed,
            binding.viewOdometer,
            binding.viewStatistics,
            binding.viewLogStatus,
            binding.viewDistanceDetail
        )

        // Hide all views initially
        viewContainers.forEach { it.visibility = View.GONE }
    }

    private fun setupButtonListeners() {
        // ============ BLUETOOTH CONTROLS ============
        binding.btnConnectBluetooth.setOnClickListener {
            sharedViewModel.connectToESP32()
            showToast("Connecting to ESP32...")
        }

        binding.btnDisconnectBluetooth.setOnClickListener {
            sharedViewModel.disconnect()
            showToast("Disconnecting...")
        }

        binding.btnScanDevices.setOnClickListener {
            showDeviceSelectionDialog()
        }

        // ============ LOGGING CONTROLS ============
        binding.btnStartLogging.setOnClickListener {
            sharedViewModel.startLogging()
            showToast("Starting logging...")
        }

        binding.btnStopLogging.setOnClickListener {
            sharedViewModel.stopLogging()
            showToast("Stopping logging...")
        }

        binding.btnPauseLogging.setOnClickListener {
            sharedViewModel.pauseLogging()
            showToast("Pausing logging...")
        }

        binding.btnResumeLogging.setOnClickListener {
            sharedViewModel.resumeLogging()
            showToast("Resuming logging...")
        }

        binding.btnResetTrip.setOnClickListener {
            showConfirmationDialog("Reset Trip", "Are you sure you want to reset the trip?") {
                sharedViewModel.resetTrip()
                showToast("Trip reset")
            }
        }

        binding.btnSyncTime.setOnClickListener {
            sharedViewModel.syncTime()
            showToast("Syncing time...")
        }

        // ============ VIEW NAVIGATION ============
        binding.btnNextView.setOnClickListener {
            sharedViewModel.switchToNextView()
        }

        binding.btnPrevView.setOnClickListener {
            sharedViewModel.switchToPrevView()
        }

        // ============ EXTRA CONTROLS ============
        binding.btnResetOdo.setOnClickListener {
            showConfirmationDialog("Reset Odometer", "Reset total odometer to zero?") {
                sharedViewModel.resetOdometer()
                showToast("Odometer reset")
            }
        }

        binding.btnResetMaxSpeed.setOnClickListener {
            sharedViewModel.resetMaxSpeed()
            showToast("Max speed reset")
        }

        binding.btnManualSync.setOnClickListener {
            sharedViewModel.manualSync()
            showToast("Manual sync requested")
        }

        binding.btnCalibrateMpu.setOnClickListener {
            sharedViewModel.calibrateMPU()
            showToast("Calibrating MPU...")
        }

        binding.btnGetStatus.setOnClickListener {
            sharedViewModel.getStatus()
            showToast("Requesting status...")
        }

        // ============ QUICK VIEW BUTTONS ============
        binding.btnViewDashboard.setOnClickListener {
            sharedViewModel.setView(BluetoothHandler.VIEW_DASHBOARD)
        }

        binding.btnViewSpeed.setOnClickListener {
            sharedViewModel.setView(BluetoothHandler.VIEW_SPEED)
        }

        binding.btnViewOdometer.setOnClickListener {
            sharedViewModel.setView(BluetoothHandler.VIEW_ODOMETER)
        }

        binding.btnViewStatistics.setOnClickListener {
            sharedViewModel.setView(BluetoothHandler.VIEW_STATISTICS)
        }
    }

    private fun setupObservers() {
        // ============ CONNECTION STATUS ============
        sharedViewModel.connectionStatus.observe(viewLifecycleOwner, Observer { status ->
            binding.tvBluetoothStatus.text = status

            // Update status color
            when {
                status.contains("Connected", ignoreCase = true) -> {
                    binding.tvBluetoothStatus.setTextColor(Color.GREEN)
                    binding.connectionIndicator.setBackgroundColor(Color.GREEN)
                }
                status.contains("Error", ignoreCase = true) -> {
                    binding.tvBluetoothStatus.setTextColor(Color.RED)
                    binding.connectionIndicator.setBackgroundColor(Color.RED)
                }
                status.contains("Connecting", ignoreCase = true) -> {
                    binding.tvBluetoothStatus.setTextColor(Color.YELLOW)
                    binding.connectionIndicator.setBackgroundColor(Color.YELLOW)
                }
                else -> {
                    binding.tvBluetoothStatus.setTextColor(Color.GRAY)
                    binding.connectionIndicator.setBackgroundColor(Color.GRAY)
                }
            }
        })

        sharedViewModel.isConnected.observe(viewLifecycleOwner, Observer { isConnected ->
            updateButtonStates()
            binding.tvConnectionState.text = if (isConnected) "CONNECTED" else "DISCONNECTED"
        })

        sharedViewModel.deviceName.observe(viewLifecycleOwner, Observer { deviceName ->
            binding.tvDeviceName.text = deviceName
        })

        // ============ LOGGING STATE ============
        sharedViewModel.isLogging.observe(viewLifecycleOwner, Observer { isLogging ->
            updateButtonStates()
            binding.tvLoggingState.text = if (isLogging) "LOGGING" else "STOPPED"
            binding.tvLoggingState.setTextColor(
                if (isLogging) Color.GREEN else Color.RED
            )
        })

        sharedViewModel.systemState.observe(viewLifecycleOwner, Observer { state ->
            binding.tvSystemState.text = sharedViewModel.getSystemStateName()
        })

        // ============ VIEW MODE ============
        sharedViewModel.currentView.observe(viewLifecycleOwner, Observer { viewMode ->
            updateDisplayView(viewMode)
            binding.tvCurrentView.text = sharedViewModel.getCurrentViewName()
        })

        // ============ DATA DISPLAY ============
        sharedViewModel.speed.observe(viewLifecycleOwner, Observer { speed ->
            binding.tvCurrentSpeed.text = String.format("%.1f", speed)
            binding.tvSpeedLarge.text = String.format("%.1f", speed)

            // Update speed color based on value
            when {
                speed > 80 -> binding.tvCurrentSpeed.setTextColor(Color.RED)
                speed > 60 -> binding.tvCurrentSpeed.setTextColor(Color.YELLOW)
                else -> binding.tvCurrentSpeed.setTextColor(Color.GREEN)
            }
        })

        sharedViewModel.odometer.observe(viewLifecycleOwner, Observer { odo ->
            val odoKm = odo / 1000.0f
            binding.tvTotalOdo.text = String.format("%.2f", odoKm)
            binding.tvTotalOdoLarge.text = String.format("%.2f", odoKm)
        })

        sharedViewModel.tripDistance.observe(viewLifecycleOwner, Observer { trip ->
            val tripMeters = trip
            val tripKm = trip / 1000.0f
            binding.tvTripDistance.text = String.format("%.1f", tripMeters)
            binding.tvTripDistanceLarge.text = String.format("%.2f", tripKm)
            binding.tvTripMeters.text = String.format("%.0f m", tripMeters)
        })

        sharedViewModel.maxSpeed.observe(viewLifecycleOwner, Observer { maxSpeed ->
            binding.tvMaxSpeed.text = String.format("MAX: %.1f", maxSpeed)
            binding.tvMaxSpeedLarge.text = String.format("%.1f", maxSpeed)
        })

        sharedViewModel.avgSpeed.observe(viewLifecycleOwner, Observer { avgSpeed ->
            binding.tvAvgSpeed.text = String.format("AVG: %.1f", avgSpeed)
            binding.tvAvgSpeedLarge.text = String.format("%.1f", avgSpeed)
        })

        sharedViewModel.accelZ.observe(viewLifecycleOwner, Observer { accelZ ->
            binding.tvAccelZ.text = String.format("Z: %.2f", accelZ)

            // Color code for acceleration
            when {
                accelZ > 1.5f -> binding.tvAccelZ.setTextColor(Color.RED)
                accelZ > 1.0f -> binding.tvAccelZ.setTextColor(Color.YELLOW)
                else -> binding.tvAccelZ.setTextColor(Color.GREEN)
            }
        })

        sharedViewModel.packetCount.observe(viewLifecycleOwner, Observer { packetCount ->
            binding.tvPacketCount.text = packetCount.toString()
        })

        sharedViewModel.tripTime.observe(viewLifecycleOwner, Observer { time ->
            binding.tvTripTime.text = time
        })

        sharedViewModel.timestamp.observe(viewLifecycleOwner, Observer { time ->
            binding.tvDeviceTime.text = time
        })

        sharedViewModel.batteryVoltage.observe(viewLifecycleOwner, Observer { voltage ->
            if (voltage > 0) {
                binding.tvBatteryVoltage.text = String.format("%.2f V", voltage)

                // Update battery icon
                when {
                    voltage >= 3.9f -> binding.ivBattery.setImageResource(R.drawable.ic_battery_full)
                    voltage >= 3.7f -> binding.ivBattery.setImageResource(R.drawable.ic_battery_medium)
                    voltage >= 3.5f -> binding.ivBattery.setImageResource(R.drawable.ic_battery_low)
                    else -> binding.ivBattery.setImageResource(R.drawable.ic_battery_empty)
                }
            }
        })

        sharedViewModel.dataStreaming.observe(viewLifecycleOwner, Observer { streaming ->
            binding.tvDataStreaming.text = if (streaming) "STREAMING ✓" else "NO DATA ✗"
            binding.tvDataStreaming.setTextColor(
                if (streaming) Color.GREEN else Color.RED
            )
        })

        sharedViewModel.timeSyncPending.observe(viewLifecycleOwner, Observer { pending ->
            binding.ivSyncIndicator.visibility = if (pending) View.VISIBLE else View.GONE
        })
    }

    private fun updateDisplayView(viewMode: Int) {
        // Hide all views
        viewContainers.forEach { it.visibility = View.GONE }

        // Show the selected view
        if (viewMode in viewContainers.indices) {
            viewContainers[viewMode].visibility = View.VISIBLE
        } else {
            // Fallback to dashboard
            viewContainers[0].visibility = View.VISIBLE
        }
    }

    private fun updateButtonStates() {
        val isConnected = sharedViewModel.isConnected.value ?: false
        val isLogging = sharedViewModel.isLogging.value ?: false
        val systemState = sharedViewModel.systemState.value ?: 0

        // ============ BLUETOOTH BUTTONS ============
        binding.btnConnectBluetooth.isEnabled = !isConnected
        binding.btnDisconnectBluetooth.isEnabled = isConnected
        binding.btnScanDevices.isEnabled = !isConnected

        // ============ LOGGING CONTROL BUTTONS ============
        binding.btnStartLogging.isEnabled = isConnected && !isLogging && systemState != BluetoothHandler.STATE_RUNNING
        binding.btnStopLogging.isEnabled = isConnected && isLogging
        binding.btnPauseLogging.isEnabled = isConnected && isLogging && systemState == BluetoothHandler.STATE_RUNNING
        binding.btnResumeLogging.isEnabled = isConnected && !isLogging && systemState == BluetoothHandler.STATE_PAUSED

        // ============ ACTION BUTTONS ============
        binding.btnResetTrip.isEnabled = isConnected
        binding.btnSyncTime.isEnabled = isConnected
        binding.btnResetOdo.isEnabled = isConnected && !isLogging
        binding.btnResetMaxSpeed.isEnabled = isConnected
        binding.btnManualSync.isEnabled = isConnected
        binding.btnCalibrateMpu.isEnabled = isConnected && !isLogging
        binding.btnGetStatus.isEnabled = isConnected

        // ============ VIEW BUTTONS (always enabled) ============
        binding.btnNextView.isEnabled = true
        binding.btnPrevView.isEnabled = true
        binding.btnViewDashboard.isEnabled = true
        binding.btnViewSpeed.isEnabled = true
        binding.btnViewOdometer.isEnabled = true
        binding.btnViewStatistics.isEnabled = true
    }

    private fun setupTimeUpdater() {
        Thread {
            while (true) {
                activity?.runOnUiThread {
                    val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        .format(Date())
                    binding.tvCurrentTime.text = currentTime
                }
                Thread.sleep(1000)
            }
        }.start()
    }

    private fun showDeviceSelectionDialog() {
        val devices = listOf("RoadsenseLogger-v2.4", "RoadsenseLogger", "ESP32-BT")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Device")
            .setItems(devices.toTypedArray()) { _, which ->
                val selectedDevice = devices[which]
                showToast("Selected: $selectedDevice")
                // In real app, you would connect to this device
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Confirm") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Timber.d("HomeFragment destroyed")
    }
}