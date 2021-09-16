package com.example.fruitymeshappuart.viewmodels

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.location.LocationManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val bleAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val manager = application.getSystemService(BluetoothManager::class.java)
        manager.adapter
    }
    private var scanner: BluetoothLeScanner? = bleAdapter.bluetoothLeScanner
    val scannerLiveData: ScannerStateLiveData = ScannerStateLiveData()
    val bluetoothState: MutableLiveData<Boolean> = MutableLiveData()
    val deniedPermissionState: MutableLiveData<List<String>> = MutableLiveData()
    val gpsState: MutableLiveData<Boolean> = MutableLiveData()
    val devicesLiveData: DevicesLiveData = DevicesLiveData(false)
    private val locationManager = application.getSystemService(LocationManager::class.java)
    val sortRssiState: MutableLiveData<Boolean> = MutableLiveData(false)
    val nearByState: MutableLiveData<Boolean> = MutableLiveData(false)

    private fun updateDeviceLiveData() {
        devicesLiveData.applyFilter(nearByState.value ?: false, sortRssiState.value ?: false)
    }

    fun refresh() {
        scannerLiveData.refresh()
        updateDeviceLiveData()
    }

    fun startScan() {
        if (scannerLiveData.isScanning || scanner == null) return
        ScanSettings.Builder().apply {
            setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            setReportDelay(500)
        }.build()

        GlobalScope.async {
            delay(5000)
            stopScan()
            scannerLiveData.scanningStopped()
        }

//        handler.postDelayed({
//            scannerLiveData.scanningStopped()
//            scanner?.stopScan(onScanResult)
//        }, SCAN_PERIOD)

        scanner!!.startScan(onScanResult)
        scannerLiveData.scanningStarted()
    }

    fun stopScan() {
        if (scannerLiveData.isScanning && BluetoothAdapter.getDefaultAdapter().isEnabled) {
            scanner?.stopScan(onScanResult)
        }
    }

    fun enableBle() {
        bluetoothState.postValue(true)
        if (scanner == null) scanner = bleAdapter.bluetoothLeScanner
    }

    fun disableBle() {
        bluetoothState.postValue(false)
    }

    fun isBleEnable(): Boolean {
        return bluetoothState.value == true
    }

    fun updateBleState() {
        if (bleAdapter.isEnabled) enableBle() else disableBle()
    }

    fun enableGps() {
        gpsState.postValue(true)
    }

    fun disableGps() {
        gpsState.postValue(false)
    }

    fun isGpsEnable(): Boolean {
        return gpsState.value == true
    }

    fun updateGpsState() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) enableGps()
        else disableGps()
    }

    fun disableSortByRssi() {
        sortRssiState.postValue(false)
    }

    fun enableSortByRssi() {
        sortRssiState.postValue(true)
    }

    fun isSortByRssi(): Boolean {
        return sortRssiState.value == true
    }

    fun disableShowOnlyNearby() {
        nearByState.postValue(false)
    }

    fun enableShowOnlyNearby() {
        nearByState.postValue(true)
    }

    fun isShowOnlyNearBy(): Boolean {
        return nearByState.value == true
    }

    fun updateDeniedPermission(deniedPermissions: List<String>) {
        deniedPermissionState.postValue(deniedPermissions)
    }

    /**
     * null -> permission has not been checked
     */
    fun getDeniedPermission(): List<String>? {
        return deniedPermissionState.value
    }

    private val onScanResult: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result == null) return
            devicesLiveData.deviceDiscovered(result)
            updateDeviceLiveData()
            scannerLiveData.recordFound()
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d("SCAN", "onScanFailed: ")
            scannerLiveData.scanningStopped()
        }
    }

    companion object {
        private const val SCAN_PERIOD = 10000L
    }
}