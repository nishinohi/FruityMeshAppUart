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

BLE advertisement packets have to include a Company Identifier that has been applied for and registered with the BLE SIG. Although this PJ is a personal development, it uses the `Company Identifier (0x024D)` of [M-Way Solutions GmbH](https://mway.io/en/) with their permission.
**Please be careful if you fork the source and modify it.**

## Build Smart Phone App

Build this source in Android Studio.


# Feature

* Encrypttion handshake and connection via MeshAccessConnection
* Send Terminal Command to a node
* Viewing logs

# Smartphone OS and version
Only Android is supported the AES 128 ECB encryption used in FruityMesh is not available in versions lower than Android SDK 26, so it can only be used on devices running Android 8.0 and above.

# Limitation of feature

As this is a test application, it has the following functional limitations.

* You can only connect with the Network key in the Encryption Key.
* Only the default network key of FruityMesh can be used.（22:22:22:22:22:22:22:22:22:22:22:22:22:22:22:22）
* Logs output by the module will not be sent to the app until the module finishes sending packets to the app

Logs output by the module will not be sent to the app until the module finishes sending packets to the app (or more precisely, until the end of the SendModuleActionMessage call).
The reason for this is to **prevent an infinite loop of log sending**.
For example, if `MeshAccessConnection` logging is enabled, the log will be output when a packet is sent. This will result in an infinite number of logs being sent, as shown below.
`Send log A` -> `MeshAccessConnection outputs log B when sending log A` -> `Send log B` -> `MeshAccessConnection outputs log C when sending log B` -> `Send log C` ->...