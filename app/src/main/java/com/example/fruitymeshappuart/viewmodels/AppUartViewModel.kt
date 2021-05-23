package com.example.fruitymeshappuart.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fruitymeshappuart.R
import com.example.fruitymeshappuart.adapter.DiscoveredDevice
import com.example.fruitymeshappuart.fruity.module.AppUartModule
import com.example.fruitymeshappuart.fruity.types.*
import com.example.fruitymeshappuart.manager.*
import kotlinx.coroutines.*
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import kotlin.coroutines.resume

class AppUartViewModel(application: Application) :
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

    val deviceName: MutableLiveData<String> = MutableLiveData()

    /** Terminal Command **/
    var stackTerminalCommand: String = ""
    var stackSentLen: Int = 0

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

    /**
     * responseActionType is used to generate a key that identifies the callback to be called when
     * the response is received, if a response to the outgoing message is expected
     */
    private suspend fun sendModuleActionTriggerMessageAsync(
        targetNodeId: Short, moduleIdWrapper: ModuleIdWrapper, triggerActionType: Byte,
        responseActionType: Byte = 255.toByte(), counter: Short = 0,
        additionalData: ByteArray? = null, additionalDataSize: Int = 0,
        customCallback: ((packet: ByteArray) -> Unit)? = null,
    ): Boolean {
        return suspendCancellableCoroutine {
            meshAccessManager.sendModuleActionMessage(
                MessageType.MODULE_TRIGGER_ACTION, moduleIdWrapper,
                triggerActionType, targetNodeId, 0, additionalData, additionalDataSize
            )
            if (counter == 0.toShort()) {
                it.resume(true)
                return@suspendCancellableCoroutine
            }
            meshAccessManager.addTimeoutJob(
                moduleIdWrapper, responseActionType, 0, counter,
                { it.resume(true) }, customCallback
            )
        }
    }

    fun sendTerminalCommand(
        inputCommand: String?,
        targetNodeId: Short = meshAccessManager.getPartnerId(),
        successCallback: (() -> Unit)? = null,
        failedCallback: (() -> Unit)? = null, timeoutMillis: Long = 5000
    ) {
        if (inputCommand == null && stackSentLen == 0) return
        inputCommand?.let { stackTerminalCommand = inputCommand }
        inputCommand?.let { stackSentLen = 0 }

        viewModelScope.launch {
            withTimeout(timeoutMillis) {
                try {
                    val isAllSent =
                        stackTerminalCommand.length - stackSentLen <= AppUartModule.AppUartModuleDataMessage.DATA_MAX_LEN
                    val terminalCommand =
                        if (isAllSent) stackTerminalCommand.substring(
                            stackSentLen,
                            stackTerminalCommand.length
                        ) else
                            stackTerminalCommand.substring(
                                stackSentLen,
                                AppUartModule.AppUartModuleDataMessage.DATA_MAX_LEN
                            )
                    val splitCount =
                        (stackSentLen / AppUartModule.AppUartModuleDataMessage.DATA_MAX_LEN).toByte()
                    val message = AppUartModule.AppUartModuleDataMessage(
                        if (isAllSent) MessageType.SPLIT_WRITE_CMD_END else MessageType.SPLIT_WRITE_CMD,
                        splitCount, terminalCommand.length.toByte(),
                        terminalCommand.toByteArray()
                    )
                    if (isAllSent) stackSentLen = 0
                    else stackSentLen += terminalCommand.length

                    if (inputCommand != null) inProgress()

                    sendModuleActionTriggerMessageAsync(
                        targetNodeId,
                        ModuleIdWrapper.generateVendorModuleIdWrapper(
                            VendorModuleId.APP_UART_MODULE.id,
                            1
                        ),
                        AppUartModule.AppUartModuleTriggerActionMessages.TERMINAL_COMMAND.type,
                        AppUartModule.AppUartModuleActionResponseMessages.TERMINAL_RETURN_TYPE.type,
                        1, message.createBytePacket(), message.createBytePacket().size,
                        if (isAllSent) null else fun(_: ByteArray) {
                            sendTerminalCommand(null, targetNodeId)
                        }
                    )

                    if (isAllSent) endProgress()
                    successCallback?.let { it() }
                } catch (e: TimeoutCancellationException) {
                    meshAccessManager.deleteTimeoutJob(
                        ModuleIdWrapper.generateVendorModuleIdWrapper(
                            VendorModuleId.APP_UART_MODULE.id,
                            1
                        ),
                        AppUartModule.AppUartModuleActionResponseMessages.TERMINAL_RETURN_TYPE.type,
                        0
                    )
                    endProgress()
                    failedCallback?.let { it() }
                }
            }
        }
    }

    override fun updateDisplayDeviceInfo(deviceInfo: DeviceInfo) {
        deviceInfo.clusterSize?.let { clusterSize.postValue(it) }
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
                                // Do not output logs with only newlines
                                if (tempLog == FmTypes.EOL) {
                                    tempLog = ""
                                    return
                                }
                                // Remove newline codes from logs.
                                if (tempLog.length > 2 && tempLog.substring(tempLog.length - 2) == FmTypes.EOL) {
                                    tempLog = tempLog.substring(0, tempLog.length - 2)
                                }
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
        const val TERMINAL_READ_BUFFER_LENGTH = 300
    }

}