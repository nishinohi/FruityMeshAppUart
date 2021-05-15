package com.example.fruitymeshappuart.manager

import com.example.fruitymeshappuart.fruity.types.ConnectionMessageTypes

interface ModuleMessageObserver {
    fun updateTriggerMessageData(type: Byte, message: ConnectionMessageTypes)
    fun updateResponseMessageData(type: Byte, message: ConnectionMessageTypes)
}