package com.test.emmacare_bluettooth.services

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.test.emmacare_bluettooth.etc.Const
import com.test.emmacare_bluettooth.etc.Const.ACTION_DATA_AVAILABLE
import com.test.emmacare_bluettooth.etc.Const.ACTION_GATT_CONNECTED
import com.test.emmacare_bluettooth.etc.Const.ACTION_GATT_DISCONNECTED
import com.test.emmacare_bluettooth.etc.Const.ACTION_GATT_SERVICES_DISCOVERED
import com.test.emmacare_bluettooth.etc.Const.EXTRA_DATA
import com.test.emmacare_bluettooth.etc.Const.OXYMETER
import com.test.emmacare_bluettooth.etc.Const.SCALES
import com.test.emmacare_bluettooth.etc.Const.THERMOMETER
import com.test.emmacare_bluettooth.etc.Const.TONOMETER

class BluetoothService : Service() {

    private val WRITE_START_TONOMETER = byteArrayOfInts(0xFD, 0xFD, 0xFA, 0x05, 0x0D, 0x0A)
    private val WRITE_START_THERMOMETER =
        byteArrayOfInts(0xFE, 0xFD, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x15, 0x0D, 0x0A)
    private val WRITE_START_SCALES =
        byteArrayOfInts(0xFD, 0x37, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xCB)
    private val WRITE_END_TONOMETER = byteArrayOfInts(0xFD, 0xFD, 0xFA, 0x05, 0x0D, 0x0A)
    private val WRITE_END_THERMOMETER =
        byteArrayOfInts(0xFE, 0xFD, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5A, 0x0D, 0x0A)
    private val WRITE_END_SCALES =
        byteArrayOfInts(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5A, 0x00, 0x00)


    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothDeviceAddress: String? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectionState = STATE_DISCONNECTED
    private val buf = ByteArray(10)
    private var bufIndex = 0

    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED
                connectionState = STATE_CONNECTED
                broadcastUpdate(intentAction)
                Log.i(TAG, "Connected to GATT server.")
                // Attempts to discover services after successful connection.
                Log.i(
                    TAG,
                    "Attempting to start service discovery:" + bluetoothGatt?.discoverServices()
                )
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED
                connectionState = STATE_DISCONNECTED
                Log.i(TAG, "Disconnected from GATT server.")
                broadcastUpdate(intentAction)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }

            val value = characteristic.value
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)

            val value = characteristic.value
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        if (Const.OXYMETER_UUID_CHARACTER_NOTIFY == characteristic.uuid) {
            val spo2Intent = Intent(action)
            val data = characteristic.value
            for (b in data) {
                buf[bufIndex] = b
                bufIndex++
                if (bufIndex == buf.size) {
                    spo2Intent.putExtra(EXTRA_DATA, buf)
                    sendBroadcast(spo2Intent)
                    bufIndex = 0
                }
            }
        } else {
            val data = characteristic.value
            if (data != null && data.isNotEmpty()) {
                val defaultIntent = Intent(action)
                defaultIntent.putExtra(EXTRA_DATA, data)
                sendBroadcast(defaultIntent)
            }
        }
    }

    inner class LocalBinder : Binder() {
        val service: BluetoothService
            get() = this@BluetoothService
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        close()
        return super.onUnbind(intent)
    }

    private val mBinder: IBinder = LocalBinder()

    fun initialize(): Boolean {
        if (bluetoothManager == null) {
            bluetoothManager =
                getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.")
                return false
            }
        }
        bluetoothAdapter = bluetoothManager?.adapter
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }

    fun connect(address: String?): Boolean {
        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.")
            return false
        }

        // Previously connected device. Try to reconnect.
        if (bluetoothDeviceAddress != null && address == bluetoothDeviceAddress && bluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.")
            return if (bluetoothGatt?.connect()!!) {
                connectionState = STATE_CONNECTING
                true
            } else {
                false
            }
        }
        val device = bluetoothAdapter?.getRemoteDevice(address)
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.")
            return false
        }

        bluetoothGatt = device.connectGatt(this, false, mGattCallback)
        Log.d(TAG, "Trying to create a new connection.")
        bluetoothDeviceAddress = address
        connectionState = STATE_CONNECTING
        return true
    }

    fun disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        bluetoothGatt?.disconnect()
    }

    private fun close() {
        if (bluetoothGatt == null) {
            return
        }
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic?) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        bluetoothGatt?.readCharacteristic(characteristic)
    }

    fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
        selectedDevice: String
    ) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }

        bluetoothGatt?.setCharacteristicNotification(characteristic, true)
        if (Const.OXYMETER_UUID_CHARACTER_NOTIFY == characteristic.uuid && selectedDevice == OXYMETER
            || Const.THERMOMETER_UUID_CHARACTER_NOTIFY == characteristic.uuid && selectedDevice == THERMOMETER
            || Const.SCALES_UUID_CHARACTER_NOTIFY == characteristic.uuid && selectedDevice == SCALES
            || Const.TONOMETER_UUID_CHARACTER_NOTIFY == characteristic.uuid && selectedDevice == TONOMETER
        ) {
            val descriptor = characteristic.getDescriptor(Const.UUID_CLIENT_CHARACTER_CONFIG)
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            bluetoothGatt?.writeDescriptor(descriptor)
        }
    }

    fun writeToStartMeasurement(
        characteristic: BluetoothGattCharacteristic,
        selectedDevice: String
    ) {
        when {
            isWriteTonometer(characteristic, selectedDevice) ->
                writeCharacteristic(characteristic, WRITE_START_TONOMETER)
            isWriteThermometer(characteristic, selectedDevice) ->
                writeCharacteristic(characteristic, WRITE_START_THERMOMETER)
            isWriteScales(characteristic, selectedDevice) ->
                writeCharacteristic(characteristic, WRITE_START_SCALES)
        }
    }

    fun writeToEndMeasurement(
        characteristic: BluetoothGattCharacteristic,
        selectedDevice: String,
        data: ByteArray?
    ) {
        when {
            isWriteTonometer(characteristic, selectedDevice) -> {
                if (data?.size == 8) {
                    writeCharacteristic(characteristic, WRITE_END_TONOMETER)
                }
            }
            isWriteThermometer(characteristic, selectedDevice) ->
                writeCharacteristic(characteristic, WRITE_END_THERMOMETER)
            isWriteScales(characteristic, selectedDevice) ->
                writeCharacteristic(characteristic, WRITE_END_SCALES)
        }
    }

    private fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        characteristic.value = value
        bluetoothGatt?.writeCharacteristic(characteristic)
        Log.w(TAG, "Wrote ${value.size} bytes")
    }

    private fun isWriteTonometer(
        characteristic: BluetoothGattCharacteristic,
        selectedDevice: String
    ) =
        Const.TONOMETER_UUID_CHARACTER_WRITE == characteristic.uuid && selectedDevice == TONOMETER

    private fun isWriteThermometer(
        characteristic: BluetoothGattCharacteristic,
        selectedDevice: String
    ) =
        Const.THERMOMETER_UUID_CHARACTER_WRITE == characteristic.uuid && selectedDevice == THERMOMETER

    private fun isWriteScales(characteristic: BluetoothGattCharacteristic, selectedDevice: String) =
        Const.SCALES_UUID_CHARACTER_WRITE == characteristic.uuid && selectedDevice == SCALES

    private fun byteArrayOfInts(vararg ints: Int) =
        ByteArray(ints.size) { pos -> ints[pos].toByte() }

    val supportedGattServices: List<BluetoothGattService>?
        get() = if (bluetoothGatt == null) null else bluetoothGatt?.services

    companion object {
        private val TAG = BluetoothService::class.java.simpleName
        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTING = 1
        private const val STATE_CONNECTED = 2
    }
}