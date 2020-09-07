package com.test.emmacare_bluettooth.services.scales

import android.text.TextUtils
import com.inuker.bluetooth.library.Constants
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener
import com.inuker.bluetooth.library.connect.options.BleConnectOptions
import com.inuker.bluetooth.library.connect.response.BleConnectResponse
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse
import com.inuker.bluetooth.library.model.BleGattProfile
import com.test.emmacare_bluettooth.etc.Const
import com.test.emmacare_bluettooth.services.scales.UnitConfig.UNIT_LB
import java.util.*

object ScalesController {

    private lateinit var measurementResultListener: MeasurementResultListener
    private var currentAppUnit = UNIT_LB
    private var macAddress = ""

    fun setListener(measurementMeasurementResult: MeasurementResultListener) {
        measurementResultListener = measurementMeasurementResult
    }

    private fun setUnit(unit: Int) {
        val command = BodyFatUtils.assemblyData(
            Units.convert(if (-1 == unit) UnitConfig.getUnit() else unit),
            "01"
        )
        currentAppUnit = unit
        send(command)
    }

    private fun notifyBluetooth() {
        BluetoothUtils.client.notify(
            macAddress,
            Const.SCALES_UUID_SERVICE,
            Const.SCALES_UUID_CHARACTER_NOTIFY,
            object : BleNotifyResponse {
                override fun onResponse(code: Int) {
                    if (code == 0) {
                        setUnit(currentAppUnit)
                    }
                }

                override fun onNotify(service: UUID?, character: UUID?, value: ByteArray?) {
                    if (service!! == Const.SCALES_UUID_SERVICE && character!! == character) {
                        measurementResultListener.onScalesMeasurementFetched(value)
                    }
                }
            }
        )
    }

    private fun send(cmd: String) {
        if (macAddress.isEmpty()) {
            return
        }

        BluetoothUtils.send(
            macAddress,
            Const.SCALES_UUID_SERVICE,
            Const.SCALES_UUID_CHARACTER_WRITE,
            cmd
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
                    BluetoothUtils.client.registerConnectStatusListener(macAddress, bleConnectStatusListener)

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

    fun disconnect() {
        currentAppUnit = 1
        BluetoothUtils.client.disconnect(macAddress)
    }

    interface MeasurementResultListener {
        fun onScalesMeasurementFetched(byteArray: ByteArray?)
        fun onScalesConnected()
        fun onScalesDisconnected()
    }
}
