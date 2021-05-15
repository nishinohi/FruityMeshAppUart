package com.example.fruitymeshappuart.fruity.types

import java.nio.ByteBuffer
import java.nio.ByteOrder

abstract class AdvertisingMessageTypes {
    companion object {
        const val ADV_PACKET_MAX_SIZE = 31
    }
}

enum class ServiceDataMessageType(val type: Byte) {
    INVALID(0),
    JOIN_ME_V0(0x01),
    LEGACY_ASSET(0x02),
    MESH_ACCESS(0x03),
    ASSET(0x04),
}

class AdvStructureFlags {
    val len: Byte
    val type: Byte
    val flags: Byte

    constructor(len: Byte, type: Byte, flags: Byte) {
        this.len = len
        this.type = type
        this.flags = flags
    }

    constructor(packet: ByteArray) {
        val byteBuffer = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)
        len = byteBuffer.get()
        type = byteBuffer.get()
        flags = byteBuffer.get()
    }

    companion object {
        const val SIZE_OF_PACKET = 3
    }
}

class AdvStructureUUID16 {
    val len: Byte
    val type: Byte
    val uuid: Short

    constructor(len: Byte, type: Byte, uuid: Short) {
        this.len = len
        this.type = type
        this.uuid = uuid
    }

    constructor(packet: ByteArray) {
        val byteBuffer = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)
        len = byteBuffer.get()
        type = byteBuffer.get()
        uuid = byteBuffer.short
    }

    companion object {
        const val SIZE_OF_PACKET = 4
    }
}

class AdvStructureServiceDataAndType {
    val uuid: AdvStructureUUID16
    val messageType: Byte //Message type depending on our custom service
    val reserved: Byte

    constructor(uuid: AdvStructureUUID16, messageType: Byte, reserved: Byte) {
        this.uuid = uuid
        this.messageType = messageType
        this.reserved = reserved
    }

    constructor(packet: ByteArray) {
        val byteBuffer = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)
        val uuid = ByteArray(AdvStructureUUID16.SIZE_OF_PACKET)
        byteBuffer.get(uuid, 0, AdvStructureUUID16.SIZE_OF_PACKET)
        this.uuid = AdvStructureUUID16(uuid)
        this.messageType = byteBuffer.get()
        this.reserved = byteBuffer.get()
    }

    companion object {
        const val SIZE_OF_PACKET = 6
    }
}
