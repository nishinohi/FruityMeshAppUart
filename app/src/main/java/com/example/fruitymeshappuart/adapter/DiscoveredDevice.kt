package com.example.fruitymeshappuart.adapter

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.os.Parcel
import android.os.Parcelable
import com.example.fruitymeshappuart.fruity.types.AdvStructureFlags
import com.example.fruitymeshappuart.fruity.types.AdvStructureMeshAccessServiceData
import com.example.fruitymeshappuart.fruity.types.AdvStructureUUID16
import com.example.fruitymeshappuart.fruity.types.ServiceDataMessageType

class DiscoveredDevice(var lastScanResult: ScanResult) : Parcelable {
    val device: BluetoothDevice get() = lastScanResult.device
    var name = lastScanResult.scanRecord?.deviceName ?: ""
    var enrolled: Boolean = false
    val rssi get() = lastScanResult.rssi
    var previousRssi = 0
    var highestRssi = -128

    constructor(parcel: Parcel) : this(parcel.readParcelable(ScanResult::class.java.classLoader)!!) {
        previousRssi = parcel.readInt()
        highestRssi = parcel.readInt()
    }

    init {
        update(lastScanResult)
    }

    fun update(scanResult: ScanResult) {
        lastScanResult = scanResult
        previousRssi = rssi
        highestRssi = if (highestRssi > rssi) highestRssi else rssi
        scanResult.scanRecord?.let { checkEnrolledFromMeshAccessAdvertise(it) }
    }

    private fun checkEnrolledFromMeshAccessAdvertise(scanRecord: ScanRecord) {
        if (!AdvStructureMeshAccessServiceData.isMeshAccessServiceAdvertise(scanRecord)) return

        val meshAdv = scanRecord.bytes ?: return
        val advStructureMeshAccessServiceData =
            AdvStructureMeshAccessServiceData(meshAdv.copyOfRange(
                AdvStructureFlags.SIZE_OF_PACKET
                    + AdvStructureUUID16.SIZE_OF_PACKET,
                AdvStructureFlags.SIZE_OF_PACKET + AdvStructureUUID16.SIZE_OF_PACKET +
                        AdvStructureMeshAccessServiceData.SIZE_OF_PACKET))
        if (advStructureMeshAccessServiceData.data.messageType == ServiceDataMessageType.MESH_ACCESS.type) {
            this.enrolled = advStructureMeshAccessServiceData.isEnrolled
        }
    }

    /* package */
    fun hasRssiLevelChanged(): Boolean {
        val newLevel =
            if (rssi <= 10) 0 else if (rssi <= 28) 1 else if (rssi <= 45) 2 else if (rssi <= 65) 3 else 4
        val oldLevel =
            if (previousRssi <= 10) 0 else if (previousRssi <= 28) 1 else if (previousRssi <= 45) 2 else if (previousRssi <= 65) 3 else 4
        return newLevel != oldLevel
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(lastScanResult, flags)
        parcel.writeInt(previousRssi)
        parcel.writeInt(highestRssi)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DiscoveredDevice> {
        override fun createFromParcel(parcel: Parcel): DiscoveredDevice {
            return DiscoveredDevice(parcel)
        }

        override fun newArray(size: Int): Array<DiscoveredDevice?> {
            return arrayOfNulls(size)
        }

    }


}