package com.test.emmacare_bluettooth.devices

import com.inuker.bluetooth.library.connect.response.BleNotifyResponse
import com.test.emmacare_bluettooth.bluetooth.BluetoothUtils
import com.test.emmacare_bluettooth.etc.Const
import java.util.*

class ThermometerController : BaseDevicesController() {

    private val WRITE_START_THERMOMETER =
        byteArrayOfInts(0xFE, 0xFD, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x15, 0x0D, 0x0A)
    private val WRITE_END_THERMOMETER =
        byteArrayOfInts(0xFE, 0xFD, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5A, 0x0D, 0x0A)

    override fun notifyBluetooth(deviceServiceUuid: UUID, deviceWriteUuid: UUID,) {
        BluetoothUtils.client.notify(
            macAddress,
            Const.THERMOMETER_UUID_SERVICE,
            Const.THERMOMETER_UUID_CHARACTER_NOTIFY,
            object : BleNotifyResponse {
                override fun onResponse(code: Int) {
                    if (code == 0) {
                        send(WRITE_START_THERMOMETER)
                    }
                }

                override fun onNotify(service: UUID?, character: UUID?, value: ByteArray?) {
                    if (service!! == Const.THERMOMETER_UUID_SERVICE && character!! == character) {
                        send(WRITE_END_THERMOMETER)
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
            Const.THERMOMETER_UUID_SERVICE,
            Const.THERMOMETER_UUID_CHARACTER_WRITE,
            byteArray
        )
    }
}
