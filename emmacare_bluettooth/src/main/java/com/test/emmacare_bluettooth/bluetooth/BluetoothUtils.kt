package com.test.emmacare_bluettooth.bluetooth
import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.inuker.bluetooth.library.BluetoothClient
import com.inuker.bluetooth.library.connect.response.BleWriteResponse
import java.util.*
import kotlin.properties.Delegates

object BluetoothUtils {

    private const val TAG = "BluetoothUtils"

    var client: BluetoothClient by Delegates.notNull()

    private val writeRes = BleWriteResponse { code -> Log.e(TAG, "bluetooth write response code = $code") }

    fun init(context: Context) {
        client = BluetoothClient(context)
    }

    fun send(mac: String?, service: UUID?, character: UUID?, value: ByteArray) {
        if (TextUtils.isEmpty(mac) || service == null || character == null || value.isEmpty()) {
            return
        }

        client.write(mac, service, character, value, writeRes)
    }
}