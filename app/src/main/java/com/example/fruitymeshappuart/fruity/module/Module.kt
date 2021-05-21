package com.example.fruitymeshappuart.fruity.module

import com.example.fruitymeshappuart.fruity.types.*
import com.example.fruitymeshappuart.manager.MeshAccessManager
import com.example.fruitymeshappuart.manager.ModuleMessageObserver
import java.nio.ByteBuffer
import java.nio.ByteOrder

abstract class Module(
    protected val moduleName: String,
    val moduleIdWrapper: ModuleIdWrapper,
) {
    private var observer: ModuleMessageObserver? = null

    fun notifyResponseObserver(type: Byte, message: ConnectionMessageTypes) {
        observer?.updateResponseMessageData(type, message)
    }


    fun addModuleMessageObserver(observer: ModuleMessageObserver) {
        this.observer = observer
    }

    //TODO: reliable is currently not supported and by default false. The input is ignored
    fun createSendModuleActionMessagePacket(
        messageType: MessageType, receiver: Short, requestHandle: Byte, actionType: Byte,
        additionalData: ByteArray?, additionalDataSize: Int, reliable: Boolean,
    ): ByteArray {
        val headerSize =
            if (moduleIdWrapper.isVendorModuleId) ConnPacketVendorModule.SIZEOF_PACKET else ConnPacketModule.SIZEOF_PACKET
        val packetBuf =
            ByteBuffer.allocate(headerSize + additionalDataSize).order(ByteOrder.LITTLE_ENDIAN)

        if (moduleIdWrapper.isVendorModuleId) {
            val connPacketVendorModule = ConnPacketVendorModule(
                messageType, MeshAccessManager.NODE_ID, receiver, moduleIdWrapper.wrappedModuleId,
                requestHandle, actionType
            )
            packetBuf.put(connPacketVendorModule.createBytePacket())
        } else {
            val connPacketModule = ConnPacketModule(
                messageType, MeshAccessManager.NODE_ID, receiver, moduleIdWrapper.primaryModuleId,
                requestHandle, actionType
            )
            packetBuf.put(connPacketModule.createBytePacket())
        }
        if (additionalData != null) packetBuf.put(additionalData)
        return packetBuf.array()
    }

    abstract fun actionResponseMessageReceivedHandler(packet: ByteArray)

}
