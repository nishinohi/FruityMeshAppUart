package com.example.fruitymeshappuart.customexception

class MessagePacketSizeException(messageName: String, size: Int) : Exception(
    "$messageName packet must larger than $size"
) {
}