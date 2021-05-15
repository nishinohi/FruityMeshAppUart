package com.example.fruitymeshappuart.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.fruitymeshappuart.R
import com.example.fruitymeshappuart.adapter.DiscoveredDevice
import com.example.fruitymeshappuart.fruity.module.AppUartModule
import com.example.fruitymeshappuart.fruity.types.*
import com.example.fruitymeshappuart.manager.*
import kotlinx.coroutines.*
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import kotlin.coroutines.resume

class DeviceConfigViewModel(application: Application) :
    AndroidViewModel(application), DeviceInfoObserver {
    private val meshAccessManager: MeshAccessManager = MeshAccessManager(application, this)

    /** Device Connection State */
    val connectionState: LiveData<ConnectionState> = meshAccessManager.state
    val handShakeState = meshAccessManager.handShakeState
    val progressState: MutableLiveData<Boolean> = MutableLiveData()

    /** Device Info */
    var displayNodeId: MutableLiveData<Short> = MutableLiveData()
    var displayBleAddr = ""
    val clusterSize: MutableLiveData<Short> = MutableLiveData()

    //    val batteryInfo: MutableLiveData<Byte> = MutableLiveData()
//    val trapState: MutableLiveData<Boolean> = MutableLiveData()
    val deviceName: MutableLiveData<String> = MutableLiveData()

    /** Mesh Graph */
//    val meshGraph = MeshGraph(meshAccessManager.getPartnerId())
//    val nodeIdList: MutableLiveData<List<Short>> = MutableLiveData()

    /** Log **/
    val log: MutableLiveData<String> = MutableLiveData()
    var tempLog: String = ""

    init {
        addModuleMessageObserver()
    }

    private lateinit var discoveredDevice: DiscoveredDevice

    val deviceNamePreferences: SharedPreferences = application.getSharedPreferences(
        application.getString(R.string.preference_device_name_key), Context.MODE_PRIVATE
    )

    fun connect(discoveredDevice: DiscoveredDevice) {
        this.discoveredDevice = discoveredDevice
        reconnect(discoveredDevice.device)
    }

    // TODO replace magic number
    private fun reconnect(device: BluetoothDevice) {
        meshAccessManager.connect(device).retry(3, 100)
            .useAutoConnect(false)
            .enqueue()
    }

    fun startHandShake() {
        if (connectionState.value == ConnectionState.Ready) {
            meshAccessManager.startEncryptionHandshake(discoveredDevice.enrolled)
        }
    }

    // TODO implement failed
    fun disconnect() {
        meshAccessManager.disconnect().enqueue()
    }

    fun inProgress() {
        progressState.postValue(true)
    }

    fun endProgress() {
        progressState.postValue(false)
    }

    private suspend fun sendModuleActionTriggerMessageAsync(
        targetNodeId: Short, moduleIdWrapper: ModuleIdWrapper, triggerActionType: Byte,
        responseActionType: Byte, counter: Short,
        customCallback: ((packet: ByteArray) -> Unit)? = null,
    ): Boolean {
        return suspendCancellableCoroutine {
            meshAccessManager.sendModuleActionTriggerMessage(
                moduleIdWrapper,
                triggerActionType, targetNodeId
            )
            meshAccessManager.addTimeoutJob(
                moduleIdWrapper, responseActionType, 0, counter,
                { it.resume(true) }, customCallback
            )
        }
    }

//    fun updateDeviceInfo2(
//        targetNodeId: Short = meshAccessManager.getPartnerId(),
//        successCallback: (() -> Unit)? = null,
//        failedCallback: (() -> Unit)? = null, timeoutMillis: Long = 5000,
//    ) {
//        viewModelScope.launch {
//            withTimeout(timeoutMillis) {
//                try {
//                    sendModuleActionTriggerMessageAsync(targetNodeId,
//                        ModuleIdWrapper(ModuleId.STATUS_REPORTER_MODULE.id),
//                        StatusReporterModule.StatusModuleTriggerActionMessages.GET_DEVICE_INFO_V2.type,
//                        StatusReporterModule.StatusModuleActionResponseMessages.DEVICE_INFO_V2.type,
//                        1) { packet ->
//                        val deviceInfo2Message =
//                            StatusReporterModule.StatusReporterModuleDeviceInfo2Message(packet.copyOfRange(
//                                ConnPacketModule.SIZEOF_PACKET,
//                                packet.size))
//                        val bleAddr = ("${"%x".format(deviceInfo2Message.gapAddress.addr[5])}:" +
//                                "${"%x".format(deviceInfo2Message.gapAddress.addr[4])}:" +
//                                "${"%x".format(deviceInfo2Message.gapAddress.addr[3])}:" +
//                                "${"%x".format(deviceInfo2Message.gapAddress.addr[2])}:" +
//                                "${"%x".format(deviceInfo2Message.gapAddress.addr[1])}:" +
//                                "%x".format(deviceInfo2Message.gapAddress.addr[0])).toUpperCase(
//                            Locale.ROOT)
//                        displayBleAddr = bleAddr
//                        updateDisplayDeviceInfo(
//                            DeviceInfo(
//                            targetNodeId, null, null, null,
//                            deviceNamePreferences.getString(bleAddr, "Unknown Device"))
//                        )
//                    }
//                } catch (e: TimeoutCancellationException) {
//
//                }
//            }
//        }
//    }

//    fun updateDeviceInfo(
//        targetNodeId: Short = meshAccessManager.getPartnerId(),
//        successCallback: (() -> Unit)? = null,
//        failedCallback: (() -> Unit)? = null, timeoutMillis: Long = 5000,
//    ) {
//        viewModelScope.launch {
//            withTimeout(timeoutMillis) {
//                try {
//                    sendModuleActionTriggerMessageAsync(targetNodeId,
//                        ModuleIdWrapper(ModuleId.STATUS_REPORTER_MODULE.id),
//                        StatusReporterModule.StatusModuleTriggerActionMessages.GET_STATUS.type,
//                        StatusReporterModule.StatusModuleActionResponseMessages.STATUS.type,
//                        1) { packet ->
//                        val statusMessage =
//                            StatusReporterModule.StatusReporterModuleStatusMessage.readFromBytePacket(
//                                packet.copyOfRange(ConnPacketModule.SIZEOF_PACKET, packet.size))
//                        updateDisplayDeviceInfo(
//                            DeviceInfo(targetNodeId,
//                            null, statusMessage.batteryInfo)
//                        )
//                        successCallback?.let { it() }
//                    }
//                } catch (e: TimeoutCancellationException) {
//                    failedCallback?.let { it() }
//                }
//            }
//        }
//    }

    override fun updateDisplayDeviceInfo(deviceInfo: DeviceInfo) {
//        if (deviceInfo.nodeId != displayNodeId) return
        deviceInfo.clusterSize?.let { clusterSize.postValue(it) }
//        deviceInfo.batteryInfo?.let { batteryInfo.postValue(it) }
//        deviceInfo.trapState?.let { trapState.postValue(it) }
        deviceInfo.deviceName?.let { deviceName.postValue(it) }
    }

    override fun updateCommonDeviceInfo(commonDeviceInfo: CommonDeviceInfo) {
        commonDeviceInfo.clusterSize?.let { clusterSize.postValue(it) }
    }

    fun updateDisplayNodeIdByPartnerId() {
        displayNodeId.postValue(meshAccessManager.getPartnerId())
    }


    private fun addModuleMessageObserver() {
        val appUartModule = AppUartModule().apply {
            addModuleMessageObserver(object : ModuleMessageObserver {
                override fun updateTriggerMessageData(type: Byte, message: ConnectionMessageTypes) {
                    TODO("Not yet implemented")
                }

                override fun updateResponseMessageData(
                    actionType: Byte, message: ConnectionMessageTypes
                ) {
                    when (actionType) {
                        AppUartModule.AppUartModuleActionResponseMessages.RECEIVE_LOG.type -> {
                            if (message !is AppUartModule.AppUartModuleDataMessage) return
                            tempLog += message.data.toString(Charsets.UTF_8)
                            if (message.splitHeader == MessageType.SPLIT_WRITE_CMD_END) {
                                log.postValue(tempLog)
                                tempLog = ""
                                return
                            }
                        }
                    }
                }
            })
        }
        meshAccessManager.initModules(listOf(appUartModule))
    }

    companion object {
        const val EXTRA_DEVICE: String = "com.matageek.EXTRA_DEVICE"
    }

}