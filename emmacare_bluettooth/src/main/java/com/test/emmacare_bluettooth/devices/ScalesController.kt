package com.test.emmacare_bluettooth.devices

import com.inuker.bluetooth.library.connect.response.BleNotifyResponse
import com.test.emmacare_bluettooth.bluetooth.BluetoothUtils
import com.test.emmacare_bluettooth.etc.Const
import java.util.*

class ScalesController : BaseDevicesController() {

    private val WRITE_START_SCALES =
        byteArrayOfInts(0xFD, 0x37, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xCA)
    private val WRITE_END_SCALES =
        byteArrayOfInts(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5A, 0x00, 0x00)

    override fun notifyBluetooth(deviceServiceUuid: UUID, deviceWriteUuid: UUID,) {
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
            Const.SCALES_UUID_SERVICE,
            Const.SCALES_UUID_CHARACTER_WRITE,
            byteArray
        )
    }
}
