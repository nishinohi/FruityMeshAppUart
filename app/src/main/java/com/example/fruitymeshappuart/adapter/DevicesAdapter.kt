package com.example.fruitymeshappuart.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fruitymeshappuart.R
import com.example.fruitymeshappuart.databinding.DeviceItemBinding

class DevicesAdapter(private val onItemClick: (DiscoveredDevice) -> Unit) :
    ListAdapter<DiscoveredDevice, DevicesAdapter.DeviceViewHolder>(DiscoveredDeviceDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false)
        return DeviceViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val discoveredDevice: DiscoveredDevice = getItem(position)
        holder.currentDiscoveredDevice = discoveredDevice
        holder.binding.discoveredDeviceName.text =
            if (discoveredDevice.name.isEmpty()) "Unknown Device" else discoveredDevice.name
        holder.binding.deviceAddress.text = discoveredDevice.device.address

        (holder.binding.deviceIcon.background as GradientDrawable).setColor(ContextCompat.getColor(
            holder.itemView.context,
            if (discoveredDevice.enrolled) R.color.icon_background_color_activated else
                R.color.icon_background_color_non_activated)
        )
        val rssiPercent = (100.0f * (127.0f + discoveredDevice.rssi) / (127.0f + 20.0f)).toInt()
        holder.binding.rssi.setImageLevel(rssiPercent)

    }

    object DiscoveredDeviceDiffCallback : DiffUtil.ItemCallback<DiscoveredDevice>() {
        override fun areItemsTheSame(
            oldItem: DiscoveredDevice, newItem: DiscoveredDevice,
        ): Boolean {
            return oldItem.device == newItem.device
        }

        override fun areContentsTheSame(
            oldItem: DiscoveredDevice, newItem: DiscoveredDevice,
        ): Boolean {
            return !oldItem.hasRssiLevelChanged() &&
                    oldItem.enrolled == newItem.enrolled &&
                    newItem.name == oldItem.name
        }
    }

    class DeviceViewHolder(itemView: View, val onItemClick: (DiscoveredDevice) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        private var _binding: DeviceItemBinding = DeviceItemBinding.bind(itemView)
        val binding get() = _binding
        var currentDiscoveredDevice: DiscoveredDevice? = null

        init {
            binding.deviceContainer.setOnClickListener {
                currentDiscoveredDevice?.let {
                    onItemClick(it)
                }
            }
        }
    }

}