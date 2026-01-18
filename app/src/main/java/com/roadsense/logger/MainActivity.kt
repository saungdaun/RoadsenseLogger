package com.roadsense.logger

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.roadsense.logger.core.bluetooth.BluetoothHandler
import com.roadsense.logger.databinding.ActivityMainBinding
import com.roadsense.logger.ui.viewmodels.SharedViewModel
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothHandler: BluetoothHandler

    // Gunakan SharedViewModel yang sudah ada
    private val sharedViewModel: SharedViewModel by viewModels()

    // Activity Result Launchers untuk permission dan Bluetooth enable
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Timber.d("All permissions granted")
            checkBluetoothEnabled()
        } else {
            Timber.d("Some permissions denied")
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Timber.d("Bluetooth enabled by user")
            // Bluetooth sudah diaktifkan
        } else {
            Timber.d("User denied to enable Bluetooth")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Timber.d("MainActivity created with Navigation")

        // Initialize Bluetooth Handler (hanya 1 parameter sekarang)
        bluetoothHandler = BluetoothHandler(this)

        // Initialize ViewModel dengan BluetoothHandler
        sharedViewModel.initializeBluetoothHandler(bluetoothHandler)

        // Pass Bluetooth handler to application context
        (application as? RoadsenseApplication)?.bluetoothHandler = bluetoothHandler

        // Setup Bottom Navigation
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_survey,
                R.id.navigation_results,
                R.id.navigation_reports
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Setup navigation item selection listener
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    Timber.d("Home tab selected")
                    true
                }
                R.id.navigation_survey -> {
                    Timber.d("Survey tab selected")
                    true
                }
                R.id.navigation_results -> {
                    Timber.d("Results tab selected")
                    true
                }
                R.id.navigation_reports -> {
                    Timber.d("Reports tab selected")
                    true
                }
                else -> false
            }
        }

        // Check and request permissions if needed
        checkPermissions()

        Timber.d("MainActivity setup completed")
    }

    private fun checkPermissions() {
        // Daftar permission yang diperlukan
        val requiredPermissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            // Android 6-11
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            requiredPermissions.add(Manifest.permission.BLUETOOTH)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        // Cek apakah semua permission sudah granted
        val permissionsToRequest = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            Timber.d("Requesting permissions: $permissionsToRequest")
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Timber.d("All permissions already granted")
            checkBluetoothEnabled()
        }
    }

    private fun checkBluetoothEnabled() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Timber.d("Bluetooth not supported on this device")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Timber.d("Bluetooth is not enabled, requesting...")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            Timber.d("Bluetooth is already enabled")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothHandler.disconnect()  // GANTI cleanup() DENGAN disconnect()
        Timber.d("MainActivity destroyed")
    }
}