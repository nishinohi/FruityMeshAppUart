# Overview

You can connect to a FruityMesh node from your smartphone via MeshAccessConnection and send any command. In addition, logs that were originally output via UART, SEGGER, or other wired methods can be viewed in the app.

<div align="center">
<img src="/doc/appuart_demo.gif" width="350px">
</div>

# How to use

## Build fruitymesh with AppUartModule

Build this branch with the AppUartModule added.

* [fruitymesh/ble-app-uart](https://github.com/nishinohi/fruitymesh/tree/ble-app-uart)

### Caution

This PJ is a personal development and **does not have a company identifier registered with the BLE SIG**. This AppUartModule uses `0xAAAAA` as a temporary Vendor ID (which is not yet used). Please change the Vendor ID if you want to use this module in a PJ that contains a module that uses a duplicate Vendor ID.

## Build Smart Phone App

Build this source in Android Studio.


# Feature

* Encrypttion handshake and connection via MeshAccessConnection
* Send Terminal Command to a node
* Viewing logs

# Smartphone OS and version
Only Android is supported; the AES 128 ECB encryption used in FruityMesh is not available in versions lower than Android SDK 26, so it can only be used on devices running Android 8.0 and above.

# limitation of feature

As this is a test application, it has the following functional limitations.
limitation of functions

* You can only connect with the Network key in the Encryption Key.
* Only the default network key of FruityMesh can be used.（22:22:22:22:22:22:22:22:22:22:22:22:22:22:22:22）
* Cannot send log of MeshAccessConnection

MeshAccessConnection logs (Log tag is `MACON`) the packets as they are sent and received. Of course, the same is true for the log packets that AppUartModule sends to the smartphone.In other words, if you output the log of MeshAccessConnection, it will keep sending logs forever: sending logs, sending logs of MeshAccessConnection by sending logs, and so on.