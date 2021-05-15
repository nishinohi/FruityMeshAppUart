package com.example.fruitymeshappuart.fruity.types

import com.example.fruitymeshappuart.customexception.MessagePacketSizeException
import java.nio.ByteBuffer
import java.nio.ByteOrder

abstract class ConnectionMessageTypes {
    abstract fun createBytePacket(): ByteArray
    fun getByteBufferAllocate(size: Int): ByteBuffer {
        return ByteBuffer.allocate(size).order(
            ByteOrder.LITTLE_ENDIAN)
    }

    fun getByteBufferWrap(packet: ByteArray): ByteBuffer {
        return ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)
    }
}

class ConnPacketHeader : ConnectionMessageTypes {
    val messageType: MessageType
    val sender: Short
    val receiver: Short

    constructor(messageType: MessageType, sender: Short, receiver: Short) {
        this.messageType = messageType
        this.sender = sender
        this.receiver = receiver
    }

    constructor(packet: ByteArray) {
        if (packet.size < SIZEOF_PACKET) throw MessagePacketSizeException(this::class.java.toString(),
            SIZEOF_PACKET)
        val byteBuffer = getByteBufferWrap(packet)
        this.messageType = MessageType.getMessageType(byteBuffer.get())
        this.sender = byteBuffer.short
        this.receiver = byteBuffer.short
    }

    companion object {
        const val SIZEOF_PACKET = 5
    }

    override fun createBytePacket(): ByteArray {
        return getByteBufferAllocate(SIZEOF_PACKET).put(messageType.typeValue).putShort(sender)
            .putShort(receiver).array()
    }

}

class ConnPacketModule : ConnectionMessageTypes {
    val moduleId: Byte
    val requestHandle: Byte
    val actionType: Byte
    val header: ConnPacketHeader

    constructor(
        messageType: MessageType, sender: Short, receiver: Short, moduleId: Byte,
        requestHandle: Byte, actionType: Byte,
    ) {
        header = ConnPacketHeader(messageType, sender, receiver)
        this.moduleId = moduleId
        this.requestHandle = requestHandle
        this.actionType = actionType
    }

    constructor(packet: ByteArray) {
        if (packet.size < SIZEOF_PACKET) throw MessagePacketSizeException(this::class.java.toString(),
            SIZEOF_PACKET)
        val byteBuffer = getByteBufferWrap(packet)
        // skip header
        byteBuffer.get(ByteArray(ConnPacketHeader.SIZEOF_PACKET), 0, ConnPacketHeader.SIZEOF_PACKET)
        header = ConnPacketHeader(packet)
        this.moduleId = byteBuffer.get()
        this.requestHandle = byteBuffer.get()
        this.actionType = byteBuffer.get()
    }

    override fun createBytePacket(): ByteArray {
        return getByteBufferAllocate(SIZEOF_PACKET).put(header.createBytePacket()).put(moduleId)
            .put(requestHandle).put(actionType).array()
    }

    companion object {
        var SIZEOF_PACKET = ConnPacketHeader.SIZEOF_PACKET + 3
    }
}

class ConnPacketVendorModule : ConnectionMessageTypes {
    val header: ConnPacketHeader
    val vendorModuleId: Int
    val requestHandle: Byte
    val actionType: Byte

    constructor(
        messageType: MessageType, sender: Short, receiver: Short, vendorModuleId: Int,
        requestHandle: Byte, actionType: Byte,
    ) {
        this.header = ConnPacketHeader(messageType, sender, receiver)
        this.vendorModuleId = vendorModuleId
        this.requestHandle = requestHandle
        this.actionType = actionType
    }

    constructor(packet: ByteArray) {
        if (packet.size < SIZEOF_PACKET) throw MessagePacketSizeException(this::class.java.toString(),
            SIZEOF_PACKET)
        val byteBuffer = getByteBufferWrap(packet)
        val headerPacket = ByteArray(ConnPacketHeader.SIZEOF_PACKET)
        byteBuffer.get(headerPacket, 0, ConnPacketHeader.SIZEOF_PACKET)
        this.header = ConnPacketHeader(headerPacket)
        this.vendorModuleId = byteBuffer.int
        this.requestHandle = byteBuffer.get()
        this.actionType = byteBuffer.get()
    }

    override fun createBytePacket(): ByteArray {
        return getByteBufferAllocate(SIZEOF_PACKET).put(header.createBytePacket())
            .putInt(vendorModuleId).put(requestHandle).put(actionType).array()
    }

    companion object {
        const val SIZEOF_PACKET = ConnPacketHeader.SIZEOF_PACKET + 6
    }

}

class PacketSplitHeader : ConnectionMessageTypes {
    val splitMessageType: MessageType
    val splitCounter: Byte

    constructor(packet: ByteArray) {
        if (packet.size < SIZEOF_PACKET) throw MessagePacketSizeException(this::class.java.toString(),
            SIZEOF_PACKET)
        val byteBuffer = getByteBufferWrap(packet)
        splitMessageType = MessageType.getMessageType(byteBuffer.get())
        splitCounter = byteBuffer.get()
    }

    companion object {
        var SIZEOF_PACKET = 2
    }

    override fun createBytePacket(): ByteArray {
        TODO("Not yet implemented")
    }
}

class ConnPacketEncryptCustomStart : ConnectionMessageTypes {
    val version: Byte
    private val fmKeyId: FmKeyId
    private val tunnelType: Byte
    val reserved: Byte
    private val header: ConnPacketHeader

    constructor(
        sender: Short, receiver: Short, version: Byte, fmKeyId: FmKeyId, tunnelType: Byte,
        reserved: Byte,
    ) {
        header = ConnPacketHeader(MessageType.ENCRYPT_CUSTOM_START, sender, receiver)
        this.version = version
        this.fmKeyId = fmKeyId
        this.tunnelType = tunnelType
        this.reserved = reserved
    }

    companion object {
        const val SIZEOF_PACKET = ConnPacketHeader.SIZEOF_PACKET + 6
    }

    override fun createBytePacket(): ByteArray {
        val concatByte: Byte = (((reserved.toInt()) shl 2) or tunnelType.toInt()).toByte()
        return getByteBufferAllocate(SIZEOF_PACKET).put(header.createBytePacket()).put(version)
            .putInt(fmKeyId.keyId).put(concatByte).array()
    }

}

class ConnPacketEncryptCustomSNonce : ConnectionMessageTypes {
    val header: ConnPacketHeader
    val sNonceFirst: Int
    val sNonceSecond: Int
    private val sNonce: Array<Int>

    constructor(sender: Short, receiver: Short, sNonceFirst: Int, sNonceSecond: Int) {
        header = ConnPacketHeader(MessageType.ENCRYPT_CUSTOM_SNONCE, sender, receiver)
        this.sNonceFirst = sNonceFirst
        this.sNonceSecond = sNonceSecond
        sNonce = arrayOf(sNonceFirst, sNonceSecond)
    }

    companion object {
        const val SIZEOF_PACKET =
            ConnPacketHeader.SIZEOF_PACKET + 8
    }

    override fun createBytePacket(): ByteArray {
        return getByteBufferAllocate(SIZEOF_PACKET).put(header.createBytePacket()).putInt(sNonce[0])
            .putInt(sNonce[1]).array()
    }
}

class ConnPacketEncryptCustomANonce : ConnectionMessageTypes {
    val header: ConnPacketHeader
    val aNonceFirst: Int
    val aNonceSecond: Int
    private val aNonce: Array<Int>

    constructor(sender: Short, receiver: Short, aNonceFirst: Int, aNonceSecond: Int) {
        header = ConnPacketHeader(MessageType.ENCRYPT_CUSTOM_ANONCE, sender, receiver)
        this.aNonceFirst = aNonceFirst
        this.aNonceSecond = aNonceSecond
        aNonce = arrayOf(aNonceFirst, aNonceSecond)
    }

    constructor(packet: ByteArray) {
        if (packet.size < SIZEOF_PACKET) throw MessagePacketSizeException(this::class.java.toString(),
            SIZEOF_PACKET)
        val byteBuffer = getByteBufferWrap(packet)
        // skip header
        byteBuffer.get(ByteArray(ConnPacketHeader.SIZEOF_PACKET), 0, ConnPacketHeader.SIZEOF_PACKET)
        header = ConnPacketHeader(packet)
        aNonceFirst = byteBuffer.int
        aNonceSecond = byteBuffer.int
        aNonce = arrayOf(aNonceFirst, aNonceSecond)
    }

    companion object {
        const val SIZEOF_PACKET = ConnPacketHeader.SIZEOF_PACKET + 8
    }

    override fun createBytePacket(): ByteArray {
        return getByteBufferAllocate(SIZEOF_PACKET).put(header.createBytePacket()).putInt(aNonce[0])
            .putInt(aNonce[1]).array()
    }
}

class ConnPacketClusterInfoUpdate : ConnectionMessageTypes {
    val header: ConnPacketHeader
    val deprecated: Int
    val clusterSizeChange: Short
    val hopsToSink: Short
    val connectionMasterBitHandover: Byte

    constructor(
        sender: Short, receiver: Short, deprecated: Int, clusterSizeChange: Short,
        hopsToSink: Short, connectionMasterBitHandover: Byte,
    ) {
        header = ConnPacketHeader(MessageType.CLUSTER_INFO_UPDATE, sender, receiver)
        this.deprecated = deprecated
        this.clusterSizeChange = clusterSizeChange
        this.hopsToSink = hopsToSink
        this.connectionMasterBitHandover = connectionMasterBitHandover
    }

    constructor(packet: ByteArray) {
        if (packet.size < SIZEOF_PACKET) throw MessagePacketSizeException(this::class.java.toString(),
            SIZEOF_PACKET)
        val byteBuffer = getByteBufferWrap(packet)
        header = ConnPacketHeader(packet)
        // skip header
        byteBuffer.get(ByteArray(ConnPacketHeader.SIZEOF_PACKET), 0, ConnPacketHeader.SIZEOF_PACKET)
        this.deprecated = byteBuffer.int
        this.clusterSizeChange = byteBuffer.short
        this.hopsToSink = byteBuffer.short
        this.connectionMasterBitHandover = byteBuffer.get()
    }

    override fun createBytePacket(): ByteArray {
        TODO("Not yet implemented")
    }

    // TODO create Unit test
    companion object {
        const val SIZEOF_PACKET = ConnPacketHeader.SIZEOF_PACKET + 9
    }

}


enum class MessageType(val typeValue: Byte) {
    INVALID(0), SPLIT_WRITE_CMD(16),  //Used if a WRITE_CMD message is split
    SPLIT_WRITE_CMD_END(17),  //Used if a WRITE_CMD message is split
    CLUSTER_WELCOME(20),  //The initial message after a connection setup ((byte)Sent between two nodes)
    CLUSTER_ACK_1(21),  //Both sides must acknowledge the handshake ((byte)Sent between two nodes)
    CLUSTER_ACK_2(22),  //Second ack ((byte)Sent between two nodes)
    CLUSTER_INFO_UPDATE(23),  //When the cluster size changes), this message is used ((byte)Sent to all nodes)
    RECONNECT(24),  //Sent while trying to reestablish a connection
    ENCRYPT_CUSTOM_START(25), ENCRYPT_CUSTOM_ANONCE(26), ENCRYPT_CUSTOM_SNONCE(27), ENCRYPT_CUSTOM_DONE(
        28),
    UPDATE_TIMESTAMP(30),  //Used to enable timestamp distribution over the mesh
    UPDATE_CONNECTION_INTERVAL(31),  //Instructs a node to use a different connection interval
    ASSET_LEGACY(32), CAPABILITY(33), ASSET_GENERIC(34), SIG_MESH_SIMPLE(
        35),  //A lightweight wrapper for SIG mesh access layer messages
    MODULE_MESSAGES_START(50), MODULE_CONFIG(50),  //Used for many different messages that set and get the module config
    MODULE_TRIGGER_ACTION(51),  //Trigger some custom module action
    MODULE_ACTION_RESPONSE(52),  //Response on a triggered action
    MODULE_GENERAL(53),  //A message), generated by the module not as a response to an action), e.g. an event
    MODULE_RAW_DATA(54), MODULE_RAW_DATA_LIGHT(55), COMPONENT_ACT(58),  //Actuator messages
    COMPONENT_SENSE(59),  //Sensor messages
    MODULE_MESSAGES_END(59), TIME_SYNC(60), DEAD_DATA(61),  //Used by the MeshAccessConnection when malformed data was received.
    DATA_1(80), DATA_1_VITAL(81), CLC_DATA(83), RESERVED_BIT_START(128.toByte()), RESERVED_BIT_END(
        255.toByte());

    companion object {
        fun getMessageType(typeValue: Byte): MessageType {
            return values().find { it.typeValue == typeValue }
                ?: INVALID
        }
    }
}

// TODO change INT to UINT
enum class FmKeyId(val keyId: Int) {
    ZERO(0), NODE(1), NETWORK(2), BASE_USER(3), ORGANIZATION(4), RESTRAINED(5), USER_DERIVED_START(
        10),
    USER_DERIVED_END(Int.MAX_VALUE);

    companion object {
        fun getFmKeyId(keyId: Int): FmKeyId {
            return values().find { it.keyId == keyId } ?: USER_DERIVED_END
        }
    }
}
