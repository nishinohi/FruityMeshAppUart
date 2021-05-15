package com.example.fruitymeshappuart.util

import android.content.Context
import kotlin.experimental.xor

class Util {
    companion object {
        fun isLocationEnabled(context: Context) {
            // is marshmallow Or Above
        }

        fun xorBytes(
            src: ByteArray, offsetSrc: Int,
            xor: ByteArray, offsetXor: Int, length: Int,
        ): ByteArray {
            val dist = ByteArray(length)
            for (ii in 0 until length) {
                dist[ii] =
                    ((src[ii + offsetSrc]) xor (xor[ii + offsetXor]))
            }
            return dist
        }

    }
}