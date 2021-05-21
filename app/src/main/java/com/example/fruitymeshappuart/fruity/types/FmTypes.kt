package com.example.fruitymeshappuart.fruity.types

import java.util.*

class FmTypes {
    companion object {
        /** MeshAccessService UUID */
        val MESH_SERVICE_DATA_SERVICE_UUID16: UUID =
            UUID.fromString("0000FE12-0000-0000-0000-000000000000")

        const val MAX_DATA_SIZE_PER_WRITE: Int = 20
        const val SECRET_KEY_LENGTH = 16
        const val EOL = "\r\n"
    }

}