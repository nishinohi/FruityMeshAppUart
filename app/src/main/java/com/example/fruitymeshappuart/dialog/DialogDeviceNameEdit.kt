package com.example.fruitymeshappuart.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.example.fruitymeshappuart.R
import kotlin.IllegalStateException

class DialogDeviceNameEdit : DialogFragment() {

    private lateinit var listener: NoticeDeviceConfigListener

    interface NoticeDeviceConfigListener {
        fun onDialogPositiveClick(dialog: DialogFragment, deviceName: String)
        fun onDialogNegativeClick(dialog: DialogFragment)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val deviceNameEditText = EditText(activity)
            val builder = AlertDialog.Builder(it)
            builder.setMessage("enter device name")
                .setView(deviceNameEditText)
                .setTitle(activity?.getString(R.string.device_config_edit_title))
                .setPositiveButton("OK") { _, _ ->
                    listener.onDialogPositiveClick(this, deviceNameEditText.text.toString())
                }
                .setNegativeButton("Cancel") { _, _ ->
                    listener.onDialogNegativeClick(this)
                }
            builder.create()
        } ?: throw IllegalStateException("illegal")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as NoticeDeviceConfigListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement NoticeDialogListener")
        }
    }
}