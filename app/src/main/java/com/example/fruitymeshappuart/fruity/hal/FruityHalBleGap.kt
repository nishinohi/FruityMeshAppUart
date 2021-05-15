package com.example.fruitymeshappuart.fruity.hal

import com.example.fruitymeshappuart.customexception.MessagePacketSizeException
import com.example.fruitymeshappuart.fruity.types.ConnectionMessageTypes

class FruityHalBleGap {
    companion object {
        const val FH_BLE_GAP_ADDR_LEN: Int = 6
    }
}

class BleGapAddr : ConnectionMessageTypes {
    val bleGapAddrType: BleGapAddrType
    val addr: ByteArray

    constructor(packet: ByteArray) {
        if (packet.size < SIZEOF_PACKET) throw MessagePacketSizeException(this::class.java.toString(),
            SIZEOF_PACKET
        )
        getByteBufferWrap(packet).let {
            bleGapAddrType = BleGapAddrType.getBleGapAddrType(it.get())
            addr = ByteArray(FruityHalBleGap.FH_BLE_GAP_ADDR_LEN)
            it.get(addr, 0, FruityHalBleGap.FH_BLE_GAP_ADDR_LEN)
        }
    }

    companion object {
        const val SIZEOF_PACKET = 1 + FruityHalBleGap.FH_BLE_GAP_ADDR_LEN
    }

    override fun createBytePacket(): ByteArray {
        TODO("Not yet implemented")
    }
}

enum class BleGapAddrType(val type: Byte) {
    PUBLIC(0x00),
    RANDOM_STATIC(0x01),
    RANDOM_PRIVATE_RESOLVABLE(0x02),
    RANDOM_PRIVATE_NON_RESOLVABLE(0x03),
    INVALID(0xFF.toByte());

    companion object {
        fun getBleGapAddrType(type: Byte): BleGapAddrType {
            return values().find { it.type == type }
                ?: INVALID
        }
    }
}

enum class BleGapAdType(val type: Byte) {
    TYPE_FLAGS(0x01),
    TYPE_16BIT_SERVICE_UUID_MORE_AVAILABLE(0x02),
    TYPE_16BIT_SERVICE_UUID_COMPLETE(0x03),
    TYPE_32BIT_SERVICE_UUID_MORE_AVAILABLE(0x04),
    TYPE_32BIT_SERVICE_UUID_COMPLETE(0x05),
    TYPE_128BIT_SERVICE_UUID_MORE_AVAILABLE(0x06),
    TYPE_128BIT_SERVICE_UUID_COMPLETE(0x07),
    TYPE_SHORT_LOCAL_NAME(0x08),
    TYPE_COMPLETE_LOCAL_NAME(0x09),
    TYPE_TX_POWER_LEVEL(0x0A),
    TYPE_CLASS_OF_DEVICE(0x0D),
    TYPE_SIMPLE_PAIRING_HASH_C(0x0E),
    TYPE_SIMPLE_PAIRING_RANDOMIZER_R(0x0F),
    TYPE_SECURITY_MANAGER_TK_VALUE(0x10),
    TYPE_SECURITY_MANAGER_OOB_FLAGS(0x11),
    TYPE_SLAVE_CONNECTION_INTERVAL_RANGE(0x12),
    TYPE_SOLICITED_SERVICE_UUIDS_16BIT(0x14),
    TYPE_SOLICITED_SERVICE_UUIDS_128BIT(0x15),
    TYPE_SERVICE_DATA(0x16),
    TYPE_PUBLIC_TARGET_ADDRESS(0x17),
    TYPE_RANDOM_TARGET_ADDRESS(0x18),
    TYPE_APPEARANCE(0x19),
    TYPE_ADVERTISING_INTERVAL(0x1A),
    TYPE_LE_BLUETOOTH_DEVICE_ADDRESS(0x1B),
    TYPE_LE_ROLE(0x1C),
    TYPE_SIMPLE_PAIRING_HASH_C256(0x1D),
    TYPE_SIMPLE_PAIRING_RANDOMIZER_R256(0x1E),
    TYPE_SERVICE_DATA_32BIT_UUID(0x20),
    TYPE_SERVICE_DATA_128BIT_UUID(0x21),
    TYPE_LESC_CONFIRMATION_VALUE(0x22),
    TYPE_LESC_RANDOM_VALUE(0x23),
    TYPE_URI(0x24),
    TYPE_3D_INFORMATION_DATA(0x3D),
    TYPE_MANUFACTURER_SPECIFIC_DATA(0xFF.toByte()),

}