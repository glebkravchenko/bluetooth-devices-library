package com.test.emmacare_bluettooth.devices

import com.inuker.bluetooth.library.connect.response.BleNotifyResponse
import com.test.emmacare_bluettooth.bluetooth.BluetoothUtils
import com.test.emmacare_bluettooth.etc.Const
import java.util.*

class OxymeterController : BaseDevicesController() {

    override fun notifyBluetooth(deviceServiceUuid: UUID, deviceWriteUuid: UUID,) {
        BluetoothUtils.client.notify(
            macAddress,
            Const.OXYMETER_UUID_SERVICE,
            Const.OXYMETER_UUID_CHARACTER_NOTIFY,
            object : BleNotifyResponse {
                override fun onResponse(code: Int) { }

                override fun onNotify(service: UUID?, character: UUID?, value: ByteArray?) {
                    if (service!! == Const.OXYMETER_UUID_SERVICE && character!! == character) {
                        measurementResultListener.onMeasurementFetched(value)
                    }
                }
            }
        )
    }
}
