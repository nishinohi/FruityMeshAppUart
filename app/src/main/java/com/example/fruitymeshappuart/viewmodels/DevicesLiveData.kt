package com.example.fruitymeshappuart.viewmodels

import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import androidx.lifecycle.LiveData
import com.example.fruitymeshappuart.adapter.DiscoveredDevice
import com.example.fruitymeshappuart.fruity.types.AdvStructureMeshAccessServiceData
import com.example.fruitymeshappuart.fruity.types.FmTypes

class DevicesLiveData(
    private val filterUuidRequired: Boolean,
) :
    LiveData<MutableList<DiscoveredDevice>>() {

    private var discoveredDevices: MutableList<DiscoveredDevice> = mutableListOf()
    private var filteredDevices: MutableList<DiscoveredDevice> = mutableListOf()

    fun applyFilter(nearBy: Boolean = false, sortByRssi: Boolean = false): Boolean {
        filteredDevices = discoveredDevices.filter { discoveredDevice ->
            this.filterServiceUuid(discoveredDevice.lastScanResult) &&
                    if (nearBy) this.filterNearby(
                        discoveredDevice.lastScanResult, FILTER_RSSI
                    ) else true
        }.toMutableList()
        if (sortByRssi) filteredDevices = filteredDevices.sortedByDescending { it.rssi }.toMutableList()
        postValue(filteredDevices)
        return filteredDevices.isNotEmpty()
    }

    @Synchronized
    fun deviceDiscovered(scanResult: ScanResult) {
        scanResult.scanRecord.let {
            if (it == null ||
                !AdvStructureMeshAccessServiceData.isMeshAccessServiceAdvertise(it)
            ) return
        }

        val newDevice = DiscoveredDevice(scanResult)
        val existDevice = discoveredDevices.find { it.device.address == scanResult.device.address }
        if (discoveredDevices.size == 0 || existDevice == null) {
            discoveredDevices.add(newDevice)
            return
        }
        existDevice.update(scanResult)
    }

    private fun filterServiceUuid(scanResult: ScanResult): Boolean {
        if (!this.filterUuidRequired) return true

        // sometimes scan result doesn't contain service uuid
        // so, once discovered device is excluded for filtering
        if (filteredDevices.find {
                it.device.address.equals(scanResult.device.address)
            } != null) return true

        val scanServiceUuids: MutableList<ParcelUuid> = scanResult.scanRecord?.serviceUuids
            ?: return false
        val maServiceUuid =
            scanServiceUuids.find { parcelUuid ->
                return (parcelUuid.uuid.toString().substring(0, 8)
                    .compareTo(
                        FmTypes.MESH_SERVICE_DATA_SERVICE_UUID16.toString()
                            .substring(0, 8)
                    ) == 0)
            }

        return maServiceUuid != null
    }

    private fun filterNearby(scanResult: ScanResult, minRssi: Int): Boolean {
        return scanResult.rssi > minRssi
    }

    companion object {
        private const val FILTER_RSSI = -60 // [dBm]
    }
}