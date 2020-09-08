package com.test.emmacare_bluettooth.devices.listener

interface MeasurementResultListener {
    fun onMeasurementFetched(byteArray: ByteArray?)
    fun onConnected()
    fun onDisconnected()
}