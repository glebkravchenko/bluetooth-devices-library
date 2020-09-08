package com.test.emmacare_bluettooth.devices

import android.text.TextUtils
import com.inuker.bluetooth.library.Constants
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener
import com.inuker.bluetooth.library.connect.options.BleConnectOptions
import com.inuker.bluetooth.library.connect.response.BleConnectResponse
import com.inuker.bluetooth.library.model.BleGattProfile
import com.test.emmacare_bluettooth.bluetooth.BluetoothUtils
import com.test.emmacare_bluettooth.devices.listener.MeasurementResultListener
import java.util.*

abstract class BaseDevicesController {

    lateinit var measurementResultListener: MeasurementResultListener
    var macAddress = ""

    fun setListener(measurementMeasurementResult: MeasurementResultListener) {
        measurementResultListener = measurementMeasurementResult
    }

    fun connect(mac: String?, deviceServiceUuid: UUID, deviceWriteUuid: UUID, deviceNotifyUuid: UUID) {
        if (mac.isNullOrEmpty()) {
            return
        }

        macAddress = mac
        val options = BleConnectOptions.Builder()
            .setConnectRetry(1)
            .setConnectTimeout(20000)
            .build()

        BluetoothUtils.client.connect(macAddress, options, object : BleConnectResponse {
            override fun onResponse(code: Int, data: BleGattProfile?) {
                if (code == Constants.REQUEST_SUCCESS) {
                    BluetoothUtils.client.registerConnectStatusListener(
                        macAddress,
                        bleConnectStatusListener
                    )

                    if (data == null) {
                        return
                    }

                    val services = data.services
                    services.forEach { service ->
                        service.characters.forEach {
                            if (TextUtils.equals(
                                    it.uuid.toString(),
                                    deviceNotifyUuid.toString()
                                )
                            ) {
                                notifyBluetooth(deviceServiceUuid, deviceWriteUuid)
                                measurementResultListener.onConnected()
                                return
                            }
                        }
                    }
                }
            }
        })
    }

    var bleConnectStatusListener = object : BleConnectStatusListener() {
        override fun onConnectStatusChanged(mac: String?, status: Int) {
            if (status == Constants.STATUS_CONNECTED) {
                measurementResultListener.onConnected()
            } else if (status == Constants.STATUS_DISCONNECTED) {
                measurementResultListener.onDisconnected()
            }
        }
    }

    protected fun byteArrayOfInts(vararg ints: Int) =
        ByteArray(ints.size) { pos -> ints[pos].toByte() }

    fun disconnect() {
        BluetoothUtils.client.disconnect(macAddress)
    }

    abstract fun notifyBluetooth(deviceServiceUuid: UUID, deviceWriteUuid: UUID)
}