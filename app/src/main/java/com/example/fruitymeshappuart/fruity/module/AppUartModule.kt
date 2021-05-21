package com.example.fruitymeshappuart.fruity.module

import com.example.fruitymeshappuart.customexception.MessagePacketSizeException
import com.example.fruitymeshappuart.fruity.Config
import com.example.fruitymeshappuart.fruity.types.*

class AppUartModule : Module(
    "appuart", ModuleIdWrapper.generateVendorModuleIdWrapper(
        VendorModuleId.APP_UART_MODULE.id, 1
    )
) {
    enum class AppUartModuleTriggerActionMessages(val type: Byte) {
        TERMINAL_COMMAND(0),
        SEND_LOG(1),
    }

    enum class AppUartModuleActionResponseMessages(val type: Byte) {
        TERMINAL_RETURN_TYPE(0),
        RECEIVE_LOG(1),
    }

    class AppUartModuleDataMessage : ConnectionMessageTypes {
        val splitHeader: MessageType
        val splitCount: Byte
        val partLen: Byte
        val data: ByteArray

        constructor(splitHeader: MessageType, splitCount: Byte, partLen: Byte, data: ByteArray) {
            this.splitHeader = splitHeader
            this.splitCount = splitCount
            this.partLen = partLen
            this.data = data
        }

        constructor(packet: ByteArray) {
            if (packet.size < MIN_SIZEOF_PACKET) throw MessagePacketSizeException(
                this::class.java.toString(),
                MIN_SIZEOF_PACKET
            )
            getByteBufferWrap(packet).apply {
                splitHeader = MessageType.getMessageType(get())
                splitCount = get()
                partLen = get()
                data = ByteArray(partLen.toUByte().toInt())
                get(data, 0, partLen.toUByte().toInt())
            }
        }

        override fun createBytePacket(): ByteArray {
            return getByteBufferAllocate(
                SIZEOF_APP_UART_MODULE_DATA_MESSAGE_STATIC + data.size
            ).put(splitHeader.typeValue).put(splitCount).put(partLen).put(data).array()
        }

        companion object {
            const val SIZEOF_APP_UART_MODULE_DATA_MESSAGE_STATIC = 3
            const val MAX_SIZEOF_PACKET =
                Config.MAX_MESH_PACKET_SIZE - ConnPacketVendorModule.SIZEOF_PACKET
            const val MIN_SIZEOF_PACKET = SIZEOF_APP_UART_MODULE_DATA_MESSAGE_STATIC + 1
            const val DATA_MAX_LEN = MAX_SIZEOF_PACKET - SIZEOF_APP_UART_MODULE_DATA_MESSAGE_STATIC
        }

    }

    override fun actionResponseMessageReceivedHandler(packet: ByteArray) {
        val vendorModulePacket = ConnPacketVendorModule(packet)
        when (vendorModulePacket.actionType) {
            AppUartModuleActionResponseMessages.RECEIVE_LOG.type -> {
                AppUartModuleDataMessage(
                    packet.copyOfRange(ConnPacketVendorModule.SIZEOF_PACKET, packet.size)
                ).let {
                    notifyResponseObserver(AppUartModuleActionResponseMessages.RECEIVE_LOG.type, it)
                }
            }
        }

    }
}
