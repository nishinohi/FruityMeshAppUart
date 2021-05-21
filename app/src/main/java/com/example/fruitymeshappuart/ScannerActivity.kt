package com.example.fruitymeshappuart

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.fruitymeshappuart.databinding.ActivityScannerBinding
import com.example.fruitymeshappuart.adapter.DevicesAdapter
import com.example.fruitymeshappuart.viewmodels.ScannerViewModel

class ScannerActivity : AppCompatActivity() {
    val scannerViewModel: ScannerViewModel by viewModels()
    private lateinit var _bind: ActivityScannerBinding
    private val bind get() = _bind
    private val requestPermissions: List<String> =
        arrayListOf(Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    private lateinit var deviceNamePreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _bind = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(bind.root)
        // get preference
        deviceNamePreferences = getSharedPreferences(getString(R.string.preference_device_name_key),
            Context.MODE_PRIVATE)
        // set recycle view adapter and recycle list observer
        setRecycleViewAdapter()
        // set live data observer
        setLiveDataObserver()
        // permission check
        scannerViewModel.updateDeniedPermission(checkDeniedPermission(requestPermissions))
        // set button handler
        setButtonHandler()
        // register broadcast receiver
        registerBleStateBroadcastReceiver()
        registerGpsStateBroadcastReceiver()
    }

    override fun onResume() {
        super.onResume()
        scannerViewModel.updateGpsState()
        scannerViewModel.updateBleState()
        scannerViewModel.refresh()
    }

    private fun setButtonHandler() {
        val permissionLauncher = registerPermissionLauncher()
        val bleEnableLauncher = registerLaunchSubActivity(
            { scannerViewModel.enableBle() }, { scannerViewModel.disableBle() })
        bind.settingDisable.actionEnableBluetooth.setOnClickListener {
            bleEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
        bind.permissionDeniedLayout.actionEnableBluetoothPermission.setOnClickListener {
            scannerViewModel.getDeniedPermission()?.let {
                permissionLauncher.launch(it.toTypedArray())
            }
        }
        val gpsEnableLauncher = registerLaunchSubActivity(
            { scannerViewModel.enableGps() }, { scannerViewModel.disableGps() }
        )
        bind.settingDisable.actionEnableGps.setOnClickListener {
            gpsEnableLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        bind.permissionDeniedLayout.actionEnableGpsPermission.setOnClickListener {
            scannerViewModel.getDeniedPermission()?.let {
                permissionLauncher.launch(it.toTypedArray())
            }
        }
    }

    private fun setRecycleViewAdapter() {
        // set recycle adapter
        val adapter = DevicesAdapter {
            Intent(this, AppUartActivity::class.java).apply {
                putExtra(AppUartActivity.EXTRA_DEVICE, it)
                startActivity(this)
            }
        }
        bind.scannedDeviceList.adapter = adapter
        scannerViewModel.devicesLiveData.observe(this) {
            it?.let {
                for (discoveredDevice in it) {
                    if (discoveredDevice.name.isNotEmpty()) continue
                    deviceNamePreferences.getString(discoveredDevice.device.address, "Unknown")
                        ?.let { deviceName -> discoveredDevice.name = deviceName }
                }
                adapter.submitList(it)
            }
        }
    }

    private fun setLiveDataObserver() {
        // set live data observer
        scannerViewModel.bluetoothState.observe(this) {
            switchContentsVisibility(
                it, scannerViewModel.isGpsEnable(), scannerViewModel.getDeniedPermission())
            switchScanner(
                it, scannerViewModel.isGpsEnable(), scannerViewModel.getDeniedPermission())
        }
        scannerViewModel.gpsState.observe(this, {
            switchContentsVisibility(
                scannerViewModel.isBleEnable(), it, scannerViewModel.getDeniedPermission())
            switchScanner(
                scannerViewModel.isBleEnable(), it, scannerViewModel.getDeniedPermission())
        })
        scannerViewModel.deniedPermissionState.observe(this) {
            switchContentsVisibility(
                scannerViewModel.isBleEnable(), scannerViewModel.isGpsEnable(), it)
            switchScanner(scannerViewModel.isBleEnable(), scannerViewModel.isGpsEnable(), it)
        }
        scannerViewModel.scannerLiveData.observe(this, {
//            scannerViewModel.startScan()
        })
    }

    private fun switchContentsVisibility(
        isBleEnabled: Boolean, isGpsEnabled: Boolean, deniedPermissions: List<String>?,
    ) {
        if (!isBleEnabled || !isGpsEnabled) {
            bind.scannedDeviceList.visibility = View.GONE
            bind.permissionDeniedLayout.root.visibility = View.GONE
            bind.settingDisable.root.visibility = View.VISIBLE
            bind.settingDisable.bluetoothGroup.visibility =
                if (!isBleEnabled) View.VISIBLE else View.GONE
            // ble is priority
            bind.settingDisable.gpsGroup.visibility =
                if (!isGpsEnabled && isBleEnabled) View.VISIBLE else View.GONE
            return
        }
        if (deniedPermissions?.isNotEmpty() == true) {
            bind.scannedDeviceList.visibility = View.GONE
            bind.settingDisable.root.visibility = View.GONE
            bind.permissionDeniedLayout.root.visibility = View.VISIBLE
            if (deniedPermissions.find { it == Manifest.permission.ACCESS_FINE_LOCATION } != null) {
                bind.permissionDeniedLayout.gpsPermissionGroup.visibility = View.VISIBLE
                bind.permissionDeniedLayout.bluetoothPermissionGroup.visibility = View.GONE
                return
            }
            if (deniedPermissions.find {
                    it == Manifest.permission.BLUETOOTH || it == Manifest.permission.BLUETOOTH_ADMIN
                } != null) {
                bind.permissionDeniedLayout.gpsPermissionGroup.visibility = View.GONE
                bind.permissionDeniedLayout.bluetoothPermissionGroup.visibility = View.VISIBLE
                return
            }
            return
        }
        bind.scannedDeviceList.visibility = View.VISIBLE
        bind.settingDisable.root.visibility = View.GONE
        bind.permissionDeniedLayout.root.visibility = View.GONE
    }

    private fun registerGpsStateBroadcastReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                scannerViewModel.updateGpsState()
            }
        }
        registerReceiver(receiver, IntentFilter(LocationManager.MODE_CHANGED_ACTION))
    }

    private fun registerBleStateBroadcastReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val state =
                    intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
                val preState = intent?.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE,
                    BluetoothAdapter.STATE_OFF)
                when (state) {
                    BluetoothAdapter.STATE_ON -> {
                        scannerViewModel.enableBle()
                    }
                    BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> {
                        if (preState != BluetoothAdapter.STATE_OFF && preState != BluetoothAdapter.STATE_TURNING_OFF)
                            scannerViewModel.disableBle()
                    }
                }
            }
        }
        registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    private fun registerLaunchSubActivity(
        resultOkCallback: () -> Unit,
        resultCanceledCallback: () -> Unit,
    ): ActivityResultLauncher<Intent> {
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                resultOkCallback()
            } else {
                resultCanceledCallback()
            }
        }
    }

    private fun registerPermissionLauncher(): ActivityResultLauncher<Array<String>> {
        return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            val deniedPermission: MutableList<String> = mutableListOf()
            permission.forEach {
                if (it.value == null || !it.value) deniedPermission.add(it.key)
            }
            scannerViewModel.updateDeniedPermission(deniedPermission)
        }
    }

    private fun checkDeniedPermission(permissions: List<String>): List<String> {
        val deniedPermissions = mutableListOf<String>()
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(this,
                    it) != PackageManager.PERMISSION_GRANTED
            ) {
                deniedPermissions.add(it)
            }
        }
        return deniedPermissions.toList()
    }

    private fun switchScanner(
        isBleEnabled: Boolean, isGpsEnabled: Boolean, deniedPermissions: List<String>?,
    ) {
        if (isBleEnabled && isGpsEnabled && deniedPermissions?.isEmpty() == true) {
            scannerViewModel.startScan()
        } else {
            scannerViewModel.stopScan()
        }
    }

}