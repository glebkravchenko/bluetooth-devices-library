package com.test.emmacarebluetoothdevices.services

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.test.emmacarebluetoothdevices.etc.Const

class BluetoothService : Service() {

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
                Log.i(TAG, "Attempting to start service discovery:" + bluetoothGatt!!.discoverServices())
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

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            broadcastUpdate(ACTION_SPO2_DATA_AVAILABLE, characteristic)
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)
        if (Const.OXYMETER_UUID_CHARACTER_RECEIVE == characteristic.uuid) {
            val data = characteristic.value
            for (b in data) {
                buf[bufIndex] = b
                bufIndex++
                if (bufIndex == buf.size) {
                    intent.putExtra(EXTRA_DATA, buf)
                    sendBroadcast(intent)
                    bufIndex = 0
                }
            }
        } else {
            val data = characteristic.value
            if (data != null && data.isNotEmpty()) {
                intent.putExtra(EXTRA_DATA, String(data))
                sendBroadcast(intent)
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
        bluetoothAdapter = bluetoothManager!!.adapter
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

        // Previously connected device.  Try to reconnect.
        if (bluetoothDeviceAddress != null && address == bluetoothDeviceAddress && bluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.")
            return if (bluetoothGatt!!.connect()) {
                connectionState = STATE_CONNECTING
                true
            } else {
                false
            }
        }
        val device = bluetoothAdapter!!.getRemoteDevice(address)
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.")
            return false
        }

        bluetoothGatt = device.connectGatt(this, false, mGattCallback)
        Log.d(TAG, "Trying to create a new connection.")
        bluetoothDeviceAddress = address
        connectionState =
            STATE_CONNECTING
        return true
    }

    fun disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        bluetoothGatt!!.disconnect()
    }

    fun close() {
        if (bluetoothGatt == null) {
            return
        }
        bluetoothGatt!!.close()
        bluetoothGatt = null
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic?) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        bluetoothGatt!!.readCharacteristic(characteristic)
    }

    fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic, enabled: Boolean) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        bluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)

        // This is specific to Oximeter Data Transfer.
        when {
            Const.OXYMETER_UUID_CHARACTER_RECEIVE == characteristic.uuid -> {
                val descriptor = characteristic.getDescriptor(Const.OXYMETER_UUID_CLIENT_CHARACTER_CONFIG)
                writeDescriptor(descriptor, enabled)
            }
            Const.THERMOMETER_UUID_CHARACTER_RECEIVE == characteristic.uuid -> {
                val descriptor = characteristic.getDescriptor(Const.THERMOMETER_UUID_CLIENT_CHARACTER_CONFIG)
                writeDescriptor(descriptor, enabled)
            }
            Const.SCALES_UUID_CHARACTER_RECEIVE == characteristic.uuid -> {
                val descriptor = characteristic.getDescriptor(Const.SCALES_UUID_CLIENT_CHARACTER_CONFIG)
                writeDescriptor(descriptor, enabled)
            }
            Const.TONOMETER_UUID_CHARACTER_RECEIVE == characteristic.uuid -> {
                val descriptor = characteristic.getDescriptor(Const.TONOMETER_UUID_CLIENT_CHARACTER_CONFIG)
                writeDescriptor(descriptor, enabled)
            }
        }
    }

    private fun writeDescriptor(descriptor: BluetoothGattDescriptor, enabled: Boolean) {
        if (enabled) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }

        bluetoothGatt?.writeDescriptor(descriptor)
    }

    val supportedGattServices: List<BluetoothGattService>?
        get() = if (bluetoothGatt == null) null else bluetoothGatt!!.services

    fun write(ch: BluetoothGattCharacteristic, bytes: ByteArray) {
        var byteOffset = 0
        while (bytes.size - byteOffset > TRANSFER_PACKAGE_SIZE) {
            val b =
                ByteArray(TRANSFER_PACKAGE_SIZE)
            System.arraycopy(
                bytes,
                byteOffset,
                b,
                0,
                TRANSFER_PACKAGE_SIZE
            )
            ch.value = b
            bluetoothGatt!!.writeCharacteristic(ch)
            byteOffset += TRANSFER_PACKAGE_SIZE
        }
        if (bytes.size - byteOffset != 0) {
            val b = ByteArray(bytes.size - byteOffset)
            System.arraycopy(bytes, byteOffset, b, 0, bytes.size - byteOffset)
            ch.value = b
            bluetoothGatt!!.writeCharacteristic(ch)
        }
    }

    companion object {
        private val TAG = BluetoothService::class.java.simpleName
        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTING = 1
        private const val STATE_CONNECTED = 2
        const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        const val ACTION_SPO2_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_SPO2_DATA_AVAILABLE"
        const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
        private const val TRANSFER_PACKAGE_SIZE = 10
    }
}