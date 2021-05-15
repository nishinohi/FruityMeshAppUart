package com.example.fruitymeshappuart.fruity.types

import android.bluetooth.le.ScanRecord
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MeshAccessTypes {
    companion object {
        const val MESH_ACCESS_MIC_LENGTH: Int = 4
    }
}

class AdvStructureMeshAccessServiceData {
    val data: AdvStructureServiceDataAndType
    val networkId: Short
    val isEnrolled: Boolean // Flag if this beacon is enrolled
    val isSink: Boolean
    val isZeroKeyConnectable: Boolean
    val isConnectable: Boolean
    val interestedInConnection: Byte
    val reserved: Byte
    val serialIndex: Int
    val moduleIds: ByteArray //Additional subServices offered with their data
    val potentiallySlicedOff: PotentiallySlicedOff

    class PotentiallySlicedOff(
        val deviceType: Byte, val reserved: ByteArray,
        val reservedForEncryption: ByteArray,
    ) {
    }

    constructor(
        data: AdvStructureServiceDataAndType,
        networkId: Short,
        isEnrolled: Boolean,
        isSink: Boolean,
        isZeroKeyConnectable: Boolean,
        isConnectable: Boolean,
        interestedInConnection: Byte,
        reserved: Byte,
        serialIndex: Int,
        moduleIds: ByteArray,
        potentiallySlicedOff: PotentiallySlicedOff,
    ) {
        this.data = data
        this.networkId = networkId
        this.isEnrolled = isEnrolled
        this.isSink = isSink
        this.isZeroKeyConnectable = isZeroKeyConnectable
        this.isConnectable = isConnectable
        this.interestedInConnection = interestedInConnection
        this.reserved = reserved
        this.serialIndex = serialIndex
        this.moduleIds = moduleIds
        this.potentiallySlicedOff = potentiallySlicedOff
    }

    constructor(packet: ByteArray) {
        val byteBuffer = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)
        val data = ByteArray(AdvStructureServiceDataAndType.SIZE_OF_PACKET)
        byteBuffer.get(data, 0, data.size)
        this.data = AdvStructureServiceDataAndType(data)
        this.networkId = byteBuffer.short
        val temp = byteBuffer.get().toUByte().toInt()
        this.isEnrolled = (temp and 1) == 1
        this.isSink = ((temp shr 1) and 1) == 1
        this.isZeroKeyConnectable = ((temp shr 2) and 1) == 1
        this.isConnectable = ((temp shr 3) and 1) == 1
        this.interestedInConnection = ((temp shr 4) and 1).toByte()
        this.reserved = ((temp shr 5) and 7).toByte()
        this.serialIndex = byteBuffer.int
        this.moduleIds = byteArrayOf(byteBuffer.get(), byteBuffer.get(), byteBuffer.get())
        val deviceType = byteBuffer.get()
        val reserved = ByteArray(3)
        byteBuffer.get(reserved, 0, reserved.size)
        val reservedForEncryption = ByteArray(4)
        byteBuffer.get(reservedForEncryption, 0, reservedForEncryption.size)
        this.potentiallySlicedOff =
            PotentiallySlicedOff(deviceType, reserved, reservedForEncryption)
    }

    companion object {
        const val SIZE_OF_PACKET = 24

        fun isMeshAccessServiceAdvertise(scanRecord: ScanRecord): Boolean {
            val meshAdv = scanRecord.bytes ?: return false
            if (meshAdv.size < AdvertisingMessageTypes.ADV_PACKET_MAX_SIZE) return false

            // check adv data sequence
            if (meshAdv[0] != (AdvStructureFlags.SIZE_OF_PACKET - 1).toByte()) return false
            if (meshAdv[AdvStructureFlags.SIZE_OF_PACKET] !=
                (AdvStructureUUID16.SIZE_OF_PACKET - 1).toByte()
            ) return false
            if (meshAdv[AdvStructureFlags.SIZE_OF_PACKET + AdvStructureUUID16.SIZE_OF_PACKET]
                != (SIZE_OF_PACKET - 1).toByte()
            ) return false

            val advStructureMeshAccessServiceData =
                AdvStructureMeshAccessServiceData(meshAdv.copyOfRange(
                    AdvStructureFlags.SIZE_OF_PACKET
                        + AdvStructureUUID16.SIZE_OF_PACKET,
                    AdvStructureFlags.SIZE_OF_PACKET + AdvStructureUUID16.SIZE_OF_PACKET +
                            SIZE_OF_PACKET
                ))
            return advStructureMeshAccessServiceData.data.messageType == ServiceDataMessageType.MESH_ACCESS.type
        }
    }
}