package com.example.fruitymeshappuart.viewmodels

import androidx.lifecycle.LiveData

class ScannerStateLiveData(
) : LiveData<ScannerStateLiveData>() {
    private var hasRecord: Boolean = false

    var isScanning: Boolean

    init {
        this.isScanning = false
        postValue(this)
    }

    /* package */
    fun scanningStarted() {
        isScanning = true
        postValue(this)
    }

    /* package */
    fun scanningStopped() {
        isScanning = false
        postValue(this)
    }

    fun refresh() {
        hasRecord = true
        postValue(this)
    }

    fun recordFound() {
        hasRecord = true
        postValue(this)
    }

}