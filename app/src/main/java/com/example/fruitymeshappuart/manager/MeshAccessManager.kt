package com.example.fruitymeshappuart.manager

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.fruitymeshappuart.R
import com.example.fruitymeshappuart.fruity.types.*
import com.example.fruitymeshappuart.fruity.module.Module
import com.example.fruitymeshappuart.profile.callback.MeshAccessDataCallback
import com.example.fruitymeshappuart.profile.callback.EncryptionState
import com.example.fruitymeshappuart.profile.FruityDataEncryptAndSplit
import no.nordicsemi.android.ble.callback.SuccessCallback
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.livedata.ObservableBleManager
import java.nio.ByteBuffer
import java.util.*
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.Exception

class MeshAccessManager(context: Context, private val observer: DeviceInfoObserver) :
    ObservableBleManager(context) {

    /** MeshAccessService Characteristics */
    private lateinit var meshAccessService: BluetoothGattService
    val handShakeState: MutableLiveData<HandShakeState> = MutableLiveData()
    private var modules: MutableList<Module> = mutableListOf()

    // ble timeout handler map
    val timeoutMap: MutableMap<Long, TimeOutCoroutineJobAndCounter<Boolean>> = mutableMapOf()

    fun initModules(modules: List<Module>) {
        this.modules = modules as MutableList<Module>
    }

    fun notifyObserver(commonDeviceInfo: CommonDeviceInfo) {
        observer.updateCommonDeviceInfo(commonDeviceInfo)
    }

    data class TimeOutCoroutineJobAndCounter<T>(
        var counter: Short,
        val successCallback: () -> Unit,
        val customCallBack: ((packet: ByteArray) -> Unit)?,
    )

    fun addTimeoutJob(
        moduleIdWrapper: ModuleIdWrapper, actionType: Byte, requestHandle: Byte, counter: Short,
        successCallback: () -> Unit, customCallBack: ((packet: ByteArray) -> Unit)? = null,
    ) {
        timeoutMap[generateTimeoutKey(moduleIdWrapper, actionType, requestHandle)] =
            TimeOutCoroutineJobAndCounter(counter, successCallback, customCallBack)
    }

    fun generateTimeoutKey(
        moduleIdWrapper: ModuleIdWrapper, actionType: Byte, requestHandle: Byte,
    ): Long {
        val byteBuffer =
            ByteBuffer.allocate(Long.SIZE_BYTES).putInt(moduleIdWrapper.wrappedModuleId)
                .put(actionType).put(requestHandle)
        byteBuffer.clear()
        return byteBuffer.long
    }

    // TODO not secure
    private val defaultKeyInt = 0x22222222

    // TODO not secure
    private val networkKeyPreference: SharedPreferences =
        context.getSharedPreferences(
            context.getString(R.string.preference_network_key),
            Context.MODE_PRIVATE
        )

    override fun getGattCallback(): BleManagerGattCallback {
        return MataGeekBleManagerGattCallback()
    }

    private val meshAccessDataCallback: MeshAccessDataCallback =
        object : MeshAccessDataCallback() {
            private val messageBuffer = mutableListOf<Byte>()

            override fun sendPacket(
                data: ByteArray, encryptionNonce: Array<Int>?, encryptionKey: SecretKey?,
                callback: SuccessCallback?,
            ) {
                val request = writeCharacteristic(this.maRxCharacteristic, Data(data))
                    .split(FruityDataEncryptAndSplit(encryptionNonce, encryptionKey)).with(this)
                if (callback != null) request.done(callback)
                request.enqueue()
            }

            override fun initialize() {
                encryptionState.postValue(EncryptionState.NOT_ENCRYPTED)
                setNotificationCallback(maTxCharacteristic).with(this)
                enableNotifications(maTxCharacteristic).with(this).enqueue()
            }

            override fun parsePacket(packet: ByteArray) {
                when (val messageType = MessageType.getMessageType(packet[0])) {
                    MessageType.ENCRYPT_CUSTOM_ANONCE -> {
                        if (encryptionState.value != EncryptionState.ENCRYPTING) return
                        val connPacketEncryptCustomANonce =
                            ConnPacketEncryptCustomANonce(packet)
                        onANonceReceived(connPacketEncryptCustomANonce)
                    }
                    MessageType.ENCRYPT_CUSTOM_DONE -> {
                        this@MeshAccessManager.handShakeState.postValue(HandShakeState.HANDSHAKE_DONE)
                    }
                    MessageType.SPLIT_WRITE_CMD -> {
                        val splitPacket = PacketSplitHeader(packet)
                        if (splitPacket.splitCounter == 0.toByte()) messageBuffer.clear()
                        messageBuffer.addAll(
                            packet.copyOfRange(
                                PacketSplitHeader.SIZEOF_PACKET,
                                packet.size
                            ).toList()
                        )
                    }
                    MessageType.SPLIT_WRITE_CMD_END -> {
                        messageBuffer.addAll(
                            packet.copyOfRange(
                                PacketSplitHeader.SIZEOF_PACKET,
                                packet.size
                            ).toList()
                        )
                        parsePacket(messageBuffer.toByteArray())
                    }
                    MessageType.MODULE_ACTION_RESPONSE -> {
                        moduleMessageReceivedHandler(packet)
                    }
                    MessageType.CLUSTER_INFO_UPDATE -> {
                        val clusterInfoUpdate = ConnPacketClusterInfoUpdate(packet)
                        // It can use any module to update device info
                        notifyObserver(
                            CommonDeviceInfo(
                                clusterInfoUpdate.clusterSizeChange
                            )
                        )
                    }
                    else -> {
                        Log.d("MATAG", "onDataReceived: Unknown Message $messageType")
                    }
                }
            }
        }

    fun moduleMessageReceivedHandler(packet: ByteArray) {
        val modulePacket = ConnPacketModule(packet)
        modules.find { it.moduleIdWrapper.primaryModuleId == modulePacket.moduleId }
            ?.actionResponseMessageReceivedHandler(packet)
        val vendorModulePacket = ConnPacketVendorModule(packet)
        modules.find { it.moduleIdWrapper.wrappedModuleId == vendorModulePacket.vendorModuleId }
            ?.actionResponseMessageReceivedHandler(packet)

        val isVendorModuleId = PrimitiveTypes.isVendorModuleId(modulePacket.moduleId)
        val moduleIdWrapper =
            if (isVendorModuleId) ModuleIdWrapper(vendorModulePacket.vendorModuleId) else
                ModuleIdWrapper(modulePacket.moduleId)
        val actionType =
            if (isVendorModuleId) vendorModulePacket.actionType else modulePacket.actionType
        val requestHandle =
            if (isVendorModuleId) vendorModulePacket.requestHandle else modulePacket.requestHandle
        val timeoutKey = generateTimeoutKey(moduleIdWrapper, actionType, requestHandle)
        timeoutMap[timeoutKey]?.let {
            --(it.counter)
            Log.d("MATAG", "timeout counter: ${it.counter}")
            it.customCallBack?.let { customCallback -> customCallback(packet) }
            if (it.counter == 0.toShort()) {
                Log.d("MATAG", "timeout counter: job cancel")
                it.successCallback()
                timeoutMap.remove(timeoutKey)
            }
        }
    }

    fun getPartnerId(): Short {
        return meshAccessDataCallback.partnerId
    }

    private fun <T : Module> findModuleById(primaryModuleId: Byte): T {
        return findModuleById(ModuleIdWrapper(primaryModuleId).wrappedModuleId)
    }

    private fun <T : Module> findModuleById(wrappedModuleId: Int): T {
        val module = modules.find {
            it.moduleIdWrapper.wrappedModuleId == wrappedModuleId
        }
            ?: throw Exception("Module not found")
        return module as? T
            ?: throw Exception("${module::class.java.toString()} is not cast type")
    }

    // TODO send module message by another MODEL class

    fun startEncryptionHandshake(isEnrolled: Boolean) {
        handShakeState.postValue(HandShakeState.HANDSHAKING)
        // TODO not secure
        if (isEnrolled) {
            val key =
                networkKeyPreference.getInt(
                    context.getString(R.string.network_key),
                    defaultKeyInt
                )
            val byteBuffer =
                ByteBuffer.allocate(16).putInt(key).putInt(key).putInt(key).putInt(key)
            Log.d("MATAG", "startEncryptionHandshake: ${Data(byteBuffer.array())}")
            meshAccessDataCallback.networkKey = SecretKeySpec(byteBuffer.array(), "AES")
        }
        meshAccessDataCallback.startEncryptionHandshake()
    }

    fun sendModuleActionTriggerMessage(
        moduleIdWrapper: ModuleIdWrapper, actionType: Byte,
        receiver: Short = meshAccessDataCallback.partnerId, additionalData: ByteArray? = null,
        additionalDataSize: Int = 0, callback: SuccessCallback? = null,
    ) {
        val module = findModuleById<Module>(moduleIdWrapper.wrappedModuleId)
        meshAccessDataCallback.sendPacket(
            module.createTriggerActionMessagePacket(
                receiver,
                actionType, additionalData, additionalDataSize
            ),
            meshAccessDataCallback.encryptionNonce,
            meshAccessDataCallback.encryptionKey, callback
        )
    }

    private inner class MataGeekBleManagerGattCallback : BleManagerGattCallback() {
        override fun initialize() {
            meshAccessDataCallback.initialize()
        }

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val maService: BluetoothGattService? = gatt.getService(MA_UUID_SERVICE)
            val maRxCharacteristic = maService?.getCharacteristic(MA_UUID_RX_CHAR)
            val maTxCharacteristic = maService?.getCharacteristic(MA_UUID_TX_CHAR)
            if (maRxCharacteristic == null || maTxCharacteristic == null) return false
            meshAccessService = maService

            meshAccessDataCallback.maTxCharacteristic = maTxCharacteristic
            meshAccessDataCallback.maRxCharacteristic = maRxCharacteristic
            return (maRxCharacteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) > 0
        }

        override fun onDeviceDisconnected() {
            Log.d("MATAG", "onDeviceDisconnected: ")
        }

    }

    enum class HandShakeState(val state: Byte) {
        HANDSHAKE_NONE(0),
        HANDSHAKING(1),
        HANDSHAKE_DONE(2)
    }

    companion object {
        const val NODE_ID: Short = 32000

        val MA_UUID_SERVICE: UUID = UUID.fromString("00000001-acce-423c-93fd-0c07a0051858")

        /** RX characteristic UUID (use for send packet to peripheral) */
        val MA_UUID_RX_CHAR: UUID = UUID.fromString("00000002-acce-423c-93fd-0c07a0051858")

        /** TX characteristic UUID (use for receive packet from peripheral) */
        val MA_UUID_TX_CHAR: UUID = UUID.fromString("00000003-acce-423c-93fd-0c07a0051858")
    }

}