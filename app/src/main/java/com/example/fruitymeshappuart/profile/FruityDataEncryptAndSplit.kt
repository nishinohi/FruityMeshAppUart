package com.example.fruitymeshappuart.profile

import com.example.fruitymeshappuart.fruity.types.FmTypes
import com.example.fruitymeshappuart.fruity.types.MeshAccessTypes
import com.example.fruitymeshappuart.fruity.types.MessageType
import com.example.fruitymeshappuart.fruity.types.PacketSplitHeader
import com.example.fruitymeshappuart.util.Util
import no.nordicsemi.android.ble.data.DataSplitter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.crypto.*
import kotlin.math.min

class FruityDataEncryptAndSplit(
    private val encryptionNonce: Array<Int>?, private val encryptionKey: SecretKey?,
) : DataSplitter {

    override fun chunk(message: ByteArray, index: Int, maxLength: Int): ByteArray? {
        // If packet size is lower than maxLength, you don't have to add split header
        if (index == 0 && FmTypes.MAX_DATA_SIZE_PER_WRITE >= message.size + MeshAccessTypes.MESH_ACCESS_MIC_LENGTH) {
            if (encryptionNonce == null || encryptionKey == null) return message

            val encryptedPacket =
                encryptPacketWithMIC(message, message.size, encryptionNonce, encryptionKey)
            encryptionNonce[1] += 2
            return encryptedPacket
        }

        val maxPayloadSize: Int = if (encryptionNonce != null && encryptionKey != null)
            FmTypes.MAX_DATA_SIZE_PER_WRITE - MeshAccessTypes.MESH_ACCESS_MIC_LENGTH - PacketSplitHeader.SIZEOF_PACKET else
            FmTypes.MAX_DATA_SIZE_PER_WRITE - PacketSplitHeader.SIZEOF_PACKET
        val offset = index * maxPayloadSize
        val payloadSize = min(maxPayloadSize, message.size - offset)
        if (payloadSize <= 0) return null
        val payloadSizeWithSplitHeader: Int =
            payloadSize + PacketSplitHeader.SIZEOF_PACKET

        val nonEncryptedDataBuffer = ByteBuffer.allocate(payloadSizeWithSplitHeader)
        // MessageType changes when you send last packet
        nonEncryptedDataBuffer.put(if (maxPayloadSize >= message.size - offset)
            MessageType.SPLIT_WRITE_CMD_END.typeValue else
            MessageType.SPLIT_WRITE_CMD.typeValue)
        nonEncryptedDataBuffer.put(index.toByte())
        nonEncryptedDataBuffer.put(message, offset, payloadSize)
        val nonEncryptedData = nonEncryptedDataBuffer.array()

        if (encryptionNonce == null || encryptionKey == null) return nonEncryptedData

        val encryptedPacket = encryptPacketWithMIC(nonEncryptedData,
            nonEncryptedData.size,
            encryptionNonce,
            encryptionKey)
        encryptionNonce[1] += 2 // increment

        return encryptedPacket
    }

    // TODO change method location and accessibility
    fun encryptPacketWithMIC(
        plainPacket: ByteArray, packetLen: Int, encryptionNonce: Array<Int>,
        sessionEncryptionKey: SecretKey?,
    ): ByteArray? {
        if (encryptionNonce.size != 2) return null
//        Log.d("FM",
//            "Encrypting: " + plainPacket.contentToString() + "(" + packetLen + ")" + " with nonce: " + encryptionNonce[1])
        // create clear text
        var byteBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.putInt(encryptionNonce[0])
        byteBuffer.putInt(encryptionNonce[1])
        val encryptNonceClearTextForKeyStream = byteBuffer.array()
        // generate key stream
        val cipher = Cipher.getInstance("AES_128/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, sessionEncryptionKey)
        val encryptKeyStream = cipher.doFinal(encryptNonceClearTextForKeyStream)
        byteBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.put(plainPacket, 0, packetLen)
        val packetZeroPadding = byteBuffer.array()
        val encryptPacket =
            Util.xorBytes(packetZeroPadding, 0, encryptKeyStream, 0, packetZeroPadding.size)
        val mic = generateMIC(encryptionNonce, sessionEncryptionKey, encryptPacket, packetLen)
            ?: return null

        val encryptPacketWithMicBuffer: ByteBuffer =
            ByteBuffer.allocate(packetLen + MeshAccessTypes.MESH_ACCESS_MIC_LENGTH)
        encryptPacketWithMicBuffer.put(encryptPacket, 0, packetLen)
        encryptPacketWithMicBuffer.put(mic, 0, MeshAccessTypes.MESH_ACCESS_MIC_LENGTH)
        val encryptPacketWithMic = encryptPacketWithMicBuffer.array()
//        Log.d("FM",
//            "Encrypted with MIC: " + encryptPacketWithMic.contentToString() + "(" + encryptPacketWithMic.size + ")" + " with nonce: " + encryptionNonce[1])
        return encryptPacketWithMic
    }

    fun generateMIC(
        encryptionNonceOrigin: Array<Int>,
        secretKey: SecretKey?,
        encryptedPacket: ByteArray,
        packetLen: Int,
    ): ByteArray? {
        if (encryptionNonceOrigin.size != 2 && encryptedPacket.size != 16) return null

        // create aNonce plain text to generate key stream for mic
        val encryptionNonce = encryptionNonceOrigin.toMutableList()
        var byteBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.putInt(encryptionNonce[0])
        byteBuffer.putInt(encryptionNonce[1] + 1)
        val plainTextForMicKeyStream = byteBuffer.array()
        val cipher = Cipher.getInstance("AES_128/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val micKeyStream = cipher.doFinal(plainTextForMicKeyStream)
        byteBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.put(encryptedPacket, 0, packetLen)
        val zeroPaddingEncryptPacket = byteBuffer.array()
        val xoredMic: ByteArray =
            Util.xorBytes(micKeyStream, 0, zeroPaddingEncryptPacket, 0, micKeyStream.size)
        val micBuffer = ByteBuffer.allocate(MeshAccessTypes.MESH_ACCESS_MIC_LENGTH)
        return micBuffer.put(cipher.doFinal(xoredMic), 0, MeshAccessTypes.MESH_ACCESS_MIC_LENGTH)
            .array()
    }


}
