package com.test.emmacare_bluettooth.devices

import com.inuker.bluetooth.library.connect.response.BleNotifyResponse
import com.test.emmacare_bluettooth.bluetooth.BluetoothUtils
import com.test.emmacare_bluettooth.etc.Const
import java.util.*

class TonometerController : BaseDevicesController() {

    private val WRITE_START_TONOMETER = byteArrayOfInts(0xFD, 0xFD, 0xFA, 0x05, 0x0D, 0x0A)
    private val WRITE_END_TONOMETER = byteArrayOfInts(0xFD, 0xFD, 0xFA, 0x05, 0x0D, 0x0A)

    override fun notifyBluetooth(deviceServiceUuid: UUID, deviceWriteUuid: UUID,) {
        BluetoothUtils.client.notify(
            macAddress,
            Const.TONOMETER_UUID_SERVICE,
            Const.TONOMETER_UUID_CHARACTER_NOTIFY,
            object : BleNotifyResponse {
                override fun onResponse(code: Int) {
                    if (code == 0) {
                        send(WRITE_START_TONOMETER)
                    }
                }

                override fun onNotify(service: UUID?, character: UUID?, value: ByteArray?) {
                    if (service!! == Const.TONOMETER_UUID_SERVICE && character!! == character) {
                        send(WRITE_END_TONOMETER)
                        measurementResultListener.onMeasurementFetched(value)
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
            Const.TONOMETER_UUID_SERVICE,
            Const.TONOMETER_UUID_CHARACTER_WRITE,
            byteArray
        )
    }
}
