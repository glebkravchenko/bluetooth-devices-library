package com.test.emmacare_bluettooth.services.controller.scales

import android.text.TextUtils
import com.inuker.bluetooth.library.Constants
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener
import com.inuker.bluetooth.library.connect.options.BleConnectOptions
import com.inuker.bluetooth.library.connect.response.BleConnectResponse
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse
import com.inuker.bluetooth.library.model.BleGattProfile
import com.test.emmacare_bluettooth.etc.Const
import java.util.*

object ScalesController {

    private val WRITE_START_SCALES =
        byteArrayOfInts(0xFD, 0x37, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xCA)
    private val WRITE_END_SCALES =
        byteArrayOfInts(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5A, 0x00, 0x00)

    private lateinit var measurementResultListener: MeasurementResultListener
    private var macAddress = ""

    fun setListener(measurementMeasurementResult: MeasurementResultListener) {
        measurementResultListener = measurementMeasurementResult
    }

    private fun notifyBluetooth() {
        BluetoothUtils.client.notify(
            macAddress,
            Const.SCALES_UUID_SERVICE,
            Const.SCALES_UUID_CHARACTER_NOTIFY,
            object : BleNotifyResponse {
                override fun onResponse(code: Int) {
                    if (code == 0) {
                        send(WRITE_START_SCALES)
                    }
                }

                override fun onNotify(service: UUID?, character: UUID?, value: ByteArray?) {
                    if (service!! == Const.SCALES_UUID_SERVICE && character!! == character) {
                        send(WRITE_END_SCALES)
                        measurementResultListener.onScalesMeasurementFetched(value)
                    }
                }
            }
        )
    }

    private fun send(byteArray: ByteArray) {
        if (macAddress.isEmpty()) {
            return
        }

        BluetoothUtils.send(
            macAddress,
            Const.SCALES_UUID_SERVICE,
            Const.SCALES_UUID_CHARACTER_WRITE,
            byteArray
        )
    }

    fun connect(mac: String?) {
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
                                    Const.SCALES_UUID_CHARACTER_NOTIFY.toString()
                                )
                            ) {
                                notifyBluetooth()
                                measurementResultListener.onScalesConnected()
                                return
                            }
                        }
                    }
                }
            }
        })
    }

    private var bleConnectStatusListener = object : BleConnectStatusListener() {
        override fun onConnectStatusChanged(mac: String?, status: Int) {
            if (status == Constants.STATUS_CONNECTED) {
                measurementResultListener.onScalesConnected()
            } else if (status == Constants.STATUS_DISCONNECTED) {
                measurementResultListener.onScalesDisconnected()
            }
        }
    }

    private fun byteArrayOfInts(vararg ints: Int) =
        ByteArray(ints.size) { pos -> ints[pos].toByte() }

    fun disconnect() {
        BluetoothUtils.client.disconnect(macAddress)
    }

    interface MeasurementResultListener {
        fun onScalesMeasurementFetched(byteArray: ByteArray?)
        fun onScalesConnected()
        fun onScalesDisconnected()
    }
}
