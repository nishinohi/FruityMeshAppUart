package com.example.fruitymeshappuart

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.fragment.app.DialogFragment
import com.example.fruitymeshappuart.adapter.DiscoveredDevice
import com.example.fruitymeshappuart.adapter.LogAdapter
import com.example.fruitymeshappuart.databinding.ActivityDeviceConfigBinding
import com.example.fruitymeshappuart.dialog.DialogDeviceNameEdit
import com.example.fruitymeshappuart.manager.DeviceInfo
import com.example.fruitymeshappuart.manager.MeshAccessManager
import com.example.fruitymeshappuart.viewmodels.DeviceConfigViewModel
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import no.nordicsemi.android.ble.observer.ConnectionObserver

class DeviceConfigActivity : AppCompatActivity(),
    DialogDeviceNameEdit.NoticeDeviceConfigListener {
    private lateinit var _bind: ActivityDeviceConfigBinding
    private val bind get() = _bind
    private val currentViewModel: DeviceConfigViewModel by viewModels()
    lateinit var deviceNamePreferences: SharedPreferences
    private lateinit var discoveredDevice: DiscoveredDevice
    private lateinit var spinnerAdapter: ArrayAdapter<Short>

    private val logAdapter = LogAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _bind = ActivityDeviceConfigBinding.inflate(layoutInflater)
        setContentView(bind.root)
        // load setting
        deviceNamePreferences = getSharedPreferences(
            getString(R.string.preference_device_name_key),
            Context.MODE_PRIVATE
        )
        // get bundle
        discoveredDevice =
            intent.getParcelableExtra(DeviceConfigViewModel.EXTRA_DEVICE)
                ?: throw Resources.NotFoundException("device")
        // action bar enable back press
        setSupportActionBar(bind.deviceManageToolBar)
        supportActionBar?.title = "Device Manager"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // set spinner adapter
        spinnerAdapter =
            ArrayAdapter<Short>(this, android.R.layout.simple_spinner_item).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        // connect device
        currentViewModel.connect(discoveredDevice)
        // set observer
        currentViewModel.connectionState.observe(this, {
            onConnectionUpdated(it)
        })
        currentViewModel.handShakeState.observe(this, {
            onHandShakeUpdated(it)
        })
        currentViewModel.progressState.observe(this, {
            bind.messageProgress.visibility = if (it) View.VISIBLE else View.INVISIBLE
        })
        currentViewModel.displayNodeId.observe(this) {
            bind.nodeId.text = "$it"
        }
        currentViewModel.clusterSize.observe(this, {
            bind.activatedClusterSize.text = it.toString()
        })
        currentViewModel.log.observe(this) {
            logAdapter.pushLog(it)
            logAdapter.notifyItemInserted(logAdapter.itemCount - 1)
        }
        bind.log.adapter = logAdapter
//        currentViewModel.nodeIdList.observe(this, {
//            spinnerAdapter.clear()
//            spinnerAdapter.addAll(it.sorted())
//            spinnerAdapter.notifyDataSetChanged()
//            it.sorted().forEachIndexed { index, nodeId ->
//                if (nodeId == currentViewModel.displayNodeId) {
//                    bind.nodeIdSpinner.setSelection(index)
//                }
//            }
//        })
    }

    private fun isActivated(): Boolean {
        return discoveredDevice.enrolled
    }

    override fun onBackPressed() {
        currentViewModel.disconnect()
        super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                currentViewModel.disconnect()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onConnectionUpdated(
        connectionState
        : ConnectionState,
    ) {
        when (connectionState.state) {
            ConnectionState.State.CONNECTING -> {
                showConnectingStatus(R.string.status_connecting)
                Log.d("MATAG", "onCreate: CONNECTING")
            }
            ConnectionState.State.INITIALIZING -> {
                showConnectingStatus(R.string.status_initializing)
                Log.d("MATAG", "onCreate: INITIALIZING")
            }
            ConnectionState.State.READY -> {
                currentViewModel.startHandShake()
            }
            ConnectionState.State.DISCONNECTING -> {
                Log.d("MATAG", "onCreate: DISCONNECTING")
            }
            ConnectionState.State.DISCONNECTED -> {
                if ((connectionState as ConnectionState.Disconnected).reason == ConnectionObserver.REASON_TERMINATE_PEER_USER) {
                    // TODO delete history
                    Intent(this, ScannerActivity::class.java).apply {
                        startActivity(this)
                    }
                }
                Log.d("MATAG", "onCreate: DISCONNECTING reason ${connectionState.reason}")
            }
        }
    }

    private fun onHandShakeUpdated(handShakeState: MeshAccessManager.HandShakeState) {
        when (handShakeState) {
            MeshAccessManager.HandShakeState.HANDSHAKING -> {
                showConnectingStatus(R.string.handshake_state_handshaking)
                Log.d("MATAG", "onCreate: CONNECTING")
            }
            MeshAccessManager.HandShakeState.HANDSHAKE_DONE -> {
                currentViewModel.updateDisplayNodeIdByPartnerId()
                currentViewModel.displayBleAddr = discoveredDevice.device.address
                bind.connectingGroup.visibility = View.GONE
                bind.deviceConfigGroup.visibility = View.VISIBLE
            }
            else -> throw Exception("Unknown Handshake state")
        }
    }

    private fun showConnectingStatus(stringId: Int) {
        bind.connectingGroup.visibility = View.VISIBLE
        bind.connectingText.setText(stringId)
    }

    override fun onDialogPositiveClick(dialog: DialogFragment, deviceName: String) {
        deviceNamePreferences.edit().putString(currentViewModel.displayBleAddr, deviceName).apply()
        currentViewModel.updateDisplayDeviceInfo(
            DeviceInfo(
                currentViewModel.displayNodeId.value ?: 0,
                null, null, null, deviceName
            )
        )
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
    }

    companion object {
        const val EXTRA_DEVICE: String = "com.matageek.EXTRA_DEVICE"
    }

//    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//        spinnerAdapter.getItem(position)?.let {
//            currentViewModel.displayNodeId = it
////            currentViewModel.updateDeviceInfo(it)
////            currentViewModel.updateDeviceInfo2(it)
//        }
//    }
//
//    override fun onNothingSelected(parent: AdapterView<*>?) {
////        TODO("Not yet implemented")
//    }


}