package com.example.fruitymeshappuart.manager

interface DeviceInfoObserver {
    fun updateDisplayDeviceInfo(deviceInfo: DeviceInfo)
    fun updateCommonDeviceInfo(commonDeviceInfo: CommonDeviceInfo)
}

data class CommonDeviceInfo(
    val clusterSize: Short? = null,
)


data class DeviceInfo(
    val nodeId: Short,
    val clusterSize: Short? = null,
    val batteryInfo: Byte? = null,
    val trapState: Boolean? = null,
    val deviceName: String? = null,
)