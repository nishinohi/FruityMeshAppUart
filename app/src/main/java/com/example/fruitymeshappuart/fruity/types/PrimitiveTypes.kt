package com.example.fruitymeshappuart.fruity.types

import java.nio.ByteBuffer
import java.nio.ByteOrder

class PrimitiveTypes {
    companion object {
        const val NODE_ID_BROADCAST: Short = 0
        fun getVendorModuleId(vendorId: Short, subId: Byte): Int {
            return ModuleIdWrapper(ModuleId.VENDOR_MODULE_ID_PREFIX.id, subId, vendorId)
                .wrappedModuleId
        }

        fun isVendorModuleId(moduleId: Byte): Boolean {
            return moduleId == ModuleId.VENDOR_MODULE_ID_PREFIX.id
        }

        fun isVendorModuleId(wrappedModuleId: Int): Boolean {
            return ModuleIdWrapper(wrappedModuleId).prefix == ModuleId.VENDOR_MODULE_ID_PREFIX.id
        }
    }
}

/**
 * vendorId is not equal vendorModuleId
 * vendorId is an element of vendorModuleId
 */
class ModuleIdWrapper {
    val prefix: Byte
    val subId: Byte
    val vendorId: Short
    val wrappedModuleId: Int
    val isVendorModuleId
        get() = prefix == ModuleId.VENDOR_MODULE_ID_PREFIX.id
    val primaryModuleId: Byte
        get() {
            if (isVendorModuleId) return ModuleId.INVALID_MODULE.id
            return prefix
        }

    constructor(prefix: Byte, subId: Byte, vendorId: Short) {
        this.prefix = prefix
        this.subId = subId
        this.vendorId = vendorId
        val byteBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.put(prefix).put(subId).putShort(vendorId).clear()
        wrappedModuleId = byteBuffer.int
    }

    // Generate ModuleIdWrapper from Primary ModuleId
    // moduleId is never VENDOR_MODULE_ID_PREFIX
    constructor(moduleId: Byte) {
        prefix = moduleId
        subId = SUB_ID_FOR_PRIMARY_MODULE_ID
        vendorId = VENDOR_ID_FOR_PRIMARY_MODULE_ID
        val byteBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.put(prefix).put(subId).putShort(vendorId).clear()
        wrappedModuleId = byteBuffer.int
    }

    constructor(wrappedModuleId: Int) {
        this.wrappedModuleId = wrappedModuleId
        val byteBuffer = ByteBuffer.allocate(4).putInt(wrappedModuleId)
        byteBuffer.clear()
        vendorId = byteBuffer.short
        subId = byteBuffer.get()
        prefix = byteBuffer.get()
    }

    companion object {
        fun generateVendorModuleIdWrapper(vendorId: Short, subId: Byte): ModuleIdWrapper {
            return ModuleIdWrapper(ModuleId.VENDOR_MODULE_ID_PREFIX.id, subId, vendorId)
        }

        const val SUB_ID_FOR_PRIMARY_MODULE_ID: Byte = 0xFF.toByte()
        const val VENDOR_ID_FOR_PRIMARY_MODULE_ID: Short = 0xFFFF.toShort()
    }
}

enum class ModuleId(val id: Byte) {
    //Standard modules
    NODE(0), // Not a module per se), but why not let it send module messages
    BEACONING_MODULE(1),
    SCANNING_MODULE(2),
    STATUS_REPORTER_MODULE(3),
    DFU_MODULE(4),
    ENROLLMENT_MODULE(5),
    IO_MODULE(6),
    DEBUG_MODULE(7),
    CONFIG(8),

    //BOARD_CONFIG(9), //deprecated as of 20.01.2020 (boardconfig is not a module anymore)
    MESH_ACCESS_MODULE(10),

    //MANAGEMENT_MODULE=11), //deprecated as of 22.05.2019
    TESTING_MODULE(12),
    BULK_MODULE(13),
    ENVIRONMENT_SENSING_MODULE(14), //Placeholder for environmental sensing module

    //M-Way Modules
    CLC_MODULE(150.toByte()),
    VS_MODULE(151.toByte()),
    ENOCEAN_MODULE(152.toByte()),
    ASSET_MODULE(153.toByte()),
    EINK_MODULE(154.toByte()),
    WM_MODULE(155.toByte()),
    ET_MODULE(156.toByte()), //Placeholder for Partner
    MODBUS_MODULE(157.toByte()),
    BP_MODULE(158.toByte()),

    //Other Modules), this range can be used for experimenting but must not be used if FruityMesh
    //nodes are to be used in a network with nodes of different vendors as their moduleIds will clash
    MY_CUSTOM_MODULE(200.toByte()),

    //PING_MODULE(201), Deprecated as of 26.08.2020, now uses a VendorModuleId
    //VENDOR_TEMPLATE_MODULE(202), Deprecated as of 20.08.2020, now uses a VendorModuleId
    SIG_EXAMPLE_MODULE(203.toByte()),
    MATAGEEK_MODULE(204.toByte()),

    //The VendorModuleId was introduced to have a range of moduleIds that do not clash
    //between different vendors
    VENDOR_MODULE_ID_PREFIX(240.toByte()), //0xF0

    //The in between space is reserved for later extensions, e.g. of the vendor module ids
    //Invalid Module: 0xFF is the flash memory default and is therefore invalid
    INVALID_MODULE(255.toByte()),
}

enum class VendorModuleId(val id: Short) {
    MATAGEEK_MODULE(0xAB24.toShort()),
    APP_UART_MODULE(0x024D.toShort())
}

enum class DeviceType(val type: Byte) {
    INVALID(0),
    STATIC(1), // A normal node that remains static at one position
    ROAMING(2), // A node that is moving constantly or often (not implemented)
    SINK(3), // A static node that wants to acquire data, e.g. a MeshGateway
    ASSET(4), // A roaming node that is sporadically or never connected but broadcasts data
    LEAF(5),  // A node that will never act as a slave but will only connect as a master (useful for roaming nodes, but no relaying possible)
}