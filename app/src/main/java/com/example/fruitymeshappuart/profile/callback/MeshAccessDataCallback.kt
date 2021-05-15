package com.example.fruitymeshappuart.profile.callback

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.fruitymeshappuart.fruity.types.*
import com.example.fruitymeshappuart.manager.MeshAccessManager
import com.example.fruitymeshappuart.util.Util
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.callback.DataSentCallback
import no.nordicsemi.android.ble.callback.SuccessCallback
import no.nordicsemi.android.ble.data.Data
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

abstract class MeshAccessDataCallback :
    DataReceivedCallback, DataSentCallback {
    lateinit var maRxCharacteristic: BluetoothGattCharacteristic
    lateinit var maTxCharacteristic: BluetoothGattCharacteristic
    val encryptionState: MutableLiveData<EncryptionState> = MutableLiveData()
    var partnerId: Short = 0
    lateinit var encryptionNonce: Array<Int>
    lateinit var encryptionKey: SecretKey
    lateinit var decryptionNonce: Array<Int>
    lateinit var decryptionKey: SecretKey
    var networkKey: SecretKeySpec = SecretKeySpec(
        byteArrayOf(0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22,
            0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22), "AES"
    )

    abstract fun sendPacket(
        data: ByteArray,
        encryptionNonce: Array<Int>?,
        encryptionKey: SecretKey?,
        callback: SuccessCallback? = null
    )

    abstract fun initialize()
    abstract fun parsePacket(packet: ByteArray)

    override fun onDataSent(device: BluetoothDevice, data: Data) {
        Log.d("MATAG", "onDataSent: ${data.value}")
    }

    override fun onDataReceived(device: BluetoothDevice, data: Data) {
        var packet = data.value ?: return
        if (encryptionState.value == EncryptionState.ENCRYPTED) {
            packet = decryptPacket(packet, data.size(), decryptionNonce, decryptionKey) ?: return
            decryptionNonce[1] += 2
        }
        parsePacket(packet)
        Log.d("MATAG", "onDataReceived: $data")
    }

    fun onANonceReceived(aNoncePacket: ConnPacketEncryptCustomANonce) {
        partnerId = aNoncePacket.header.sender
        encryptionNonce = arrayOf(aNoncePacket.aNonceFirst, aNoncePacket.aNonceSecond)
        val plainTextForEncryptionKey = createPlainTextForSecretKey(
            MeshAccessManager.NODE_ID,
            encryptionNonce)
        Log.d("MATAG", "encryptionNonce[0]: ${encryptionNonce[0]}")
        Log.d("MATAG", "encryptionNonce[1]: ${encryptionNonce[1]}")
        Log.d("MATAG", "networkKey: ${Data(networkKey.encoded)}")
        encryptionKey = generateSecretKey(plainTextForEncryptionKey, networkKey)
        val secureRandom = SecureRandom.getInstance("SHA1PRNG")
        decryptionNonce = arrayOf(secureRandom.nextInt(), secureRandom.nextInt())
        Log.d("MATAG", "decryptionNonce[0]: ${decryptionNonce[0]}")
        Log.d("MATAG", "decryptionNonce[1]: ${decryptionNonce[1]}")
        val plainTextForDecryptionKey = createPlainTextForSecretKey(
            MeshAccessManager.NODE_ID,
            decryptionNonce)
        decryptionKey = generateSecretKey(plainTextForDecryptionKey, networkKey)
        val sNoncePacket = ConnPacketEncryptCustomSNonce(
            MeshAccessManager.NODE_ID,
            partnerId, decryptionNonce[0], decryptionNonce[1])
        encryptionState.postValue(EncryptionState.ENCRYPTED)
        sendPacket(sNoncePacket.createBytePacket(), encryptionNonce, encryptionKey)
    }

    fun startEncryptionHandshake() {
        encryptionState.postValue(EncryptionState.ENCRYPTING)
        val connPacketEncryptCustomStart =
            ConnPacketEncryptCustomStart(MeshAccessManager.NODE_ID, 0, 1, FmKeyId.NETWORK, 1, 0)
        sendPacket(connPacketEncryptCustomStart.createBytePacket(), null, null)
    }

    private fun createPlainTextForSecretKey(nodeId: Short, nonce: Array<Int>): ByteArray {
        val byteBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.putShort(nodeId)
        byteBuffer.putInt(nonce[0])
        byteBuffer.putInt(nonce[1])
        return byteBuffer.array()
    }

    private fun generateSecretKey(plainText: ByteArray, secretKey: SecretKey): SecretKey {
        val cipher = Cipher.getInstance("AES_128/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val temp = cipher.doFinal(plainText)
//        Log.d("MATAG", "generateSecretKey: ${Data(temp)}")
        return SecretKeySpec(cipher.doFinal(plainText), "AES")
    }

    fun decryptPacket(
        encryptPacket: ByteArray, packetLen: Int,
        decryptionNonce: Array<Int>, sessionDecryptionKey: SecretKey,
    ): ByteArray? {
        if (decryptionNonce.size != 2) return null
        val packetRawLen: Int = packetLen - MeshAccessTypes.MESH_ACCESS_MIC_LENGTH
        // check MIC
        if (!this.checkMicValidation(encryptPacket, decryptionNonce, sessionDecryptionKey)) {
//            Log.d("MATAG", "MIC is invalid")
            return null
        }
//        Log.d("MATAG",
//            "Decrypting: " + encryptPacket + "(" + packetLen + ")" + " with nonce: " + decryptionNonce[1])
        val byteBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.putInt(decryptionNonce[0])
        byteBuffer.putInt(decryptionNonce[1])
        val decryptNonceClearTextForKeyStream = byteBuffer.array()
        val cipher = Cipher.getInstance("AES_128/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, sessionDecryptionKey)
        val decryptKeyStream: ByteArray =
            cipher.doFinal(decryptNonceClearTextForKeyStream) ?: return null

        val decryptPacket: ByteArray =
            Util.xorBytes(encryptPacket, 0, decryptKeyStream, 0, packetRawLen)
//        Log.d("MATAG",
//            "Decrypted: " + decryptPacket + "(" + packetLen + ")" + " with nonce: " + decryptionNonce[1])
        return decryptPacket
    }


    fun checkMicValidation(
        encryptedPacket: ByteArray, _decryptionNonce: Array<Int>, secretKey: SecretKey,
    ): Boolean {
        if (_decryptionNonce.size != 2) return false

        // check MIC
        val decryptionNonce = _decryptionNonce.toList().toMutableList()
        ++decryptionNonce[1]
        var byteBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.putInt(decryptionNonce[0])
        byteBuffer.putInt(decryptionNonce[1])
        val decryptNonceClearTextForKeyStream = byteBuffer.array()
        val cipher = Cipher.getInstance("AES_128/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val micKeyStream = cipher.doFinal(decryptNonceClearTextForKeyStream) ?: return false
        byteBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.put(encryptedPacket, 0,
            encryptedPacket.size - MeshAccessTypes.MESH_ACCESS_MIC_LENGTH)
        val encryptedPacketWithoutMic = byteBuffer.array()
        val micTemp = Util.xorBytes(encryptedPacketWithoutMic, 0,
            micKeyStream, 0, encryptedPacketWithoutMic.size)
        val mic = cipher.doFinal(micTemp) ?: return false
        return mic.copyOfRange(0, MeshAccessTypes.MESH_ACCESS_MIC_LENGTH).contentEquals(
            encryptedPacket.copyOfRange(encryptedPacket.size - MeshAccessTypes.MESH_ACCESS_MIC_LENGTH,
                encryptedPacket.size))
    }


}

enum class EncryptionState(private val state: Int) {
    NOT_ENCRYPTED(0),
    ENCRYPTING(1),
    ENCRYPTED(2)
}
