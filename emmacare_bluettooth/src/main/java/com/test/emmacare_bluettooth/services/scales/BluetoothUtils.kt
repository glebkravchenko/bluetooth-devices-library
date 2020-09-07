package com.test.emmacare_bluettooth.services.scales
import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.inuker.bluetooth.library.BluetoothClient
import com.inuker.bluetooth.library.connect.response.BleWriteResponse
import java.util.*
import kotlin.properties.Delegates

object BluetoothUtils {

    private const val TAG = "BluetoothUtils"

    private val WRITE_START_SCALES =
        byteArrayOfInts(0xFD, 0x37, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xCA)

    var client: BluetoothClient by Delegates.notNull()

    private val writeRes = BleWriteResponse { code -> Log.e(TAG, "bluetooth write response code = $code") }

    fun init(context: Context) {
        client = BluetoothClient(context)
    }

    fun send(mac: String?, service: UUID?, character: UUID?, value: String?) {
        if (TextUtils.isEmpty(mac) || service == null || character == null || TextUtils.isEmpty(value)) {
            return
        }

//        client.write(mac, service, character, hexStr2ByteArray(value!!), writeRes)
        client.write(mac, service, character, WRITE_START_SCALES, writeRes)
    }

    private fun byteArrayOfInts(vararg ints: Int) =
        ByteArray(ints.size) { pos -> ints[pos].toByte() }
}