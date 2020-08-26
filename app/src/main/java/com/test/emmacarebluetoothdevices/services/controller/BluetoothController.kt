package com.test.emmacarebluetoothdevices.services.controller

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.os.IBinder
import android.util.Log
import com.test.emmacarebluetoothdevices.etc.Const.ACTION_DATA_AVAILABLE
import com.test.emmacarebluetoothdevices.etc.Const.ACTION_GATT_CONNECTED
import com.test.emmacarebluetoothdevices.etc.Const.ACTION_GATT_DISCONNECTED
import com.test.emmacarebluetoothdevices.etc.Const.ACTION_GATT_SERVICES_DISCOVERED
import com.test.emmacarebluetoothdevices.etc.Const.EXTRA_DATA
import com.test.emmacarebluetoothdevices.etc.Const.OXYMETER
import com.test.emmacarebluetoothdevices.etc.Const.OXYMETER_UUID_CHARACTER_RECEIVE
import com.test.emmacarebluetoothdevices.etc.Const.OXYMETER_UUID_MODIFY_BT_NAME
import com.test.emmacarebluetoothdevices.etc.Const.OXYMETER_UUID_SERVICE_DATA
import com.test.emmacarebluetoothdevices.etc.Const.SCALES
import com.test.emmacarebluetoothdevices.etc.Const.SCALES_UUID_CHARACTER_RECEIVE
import com.test.emmacarebluetoothdevices.etc.Const.SCALES_UUID_MODIFY_BT_NAME
import com.test.emmacarebluetoothdevices.etc.Const.SCALES_UUID_SERVICE_DATA
import com.test.emmacarebluetoothdevices.etc.Const.THERMOMETER
import com.test.emmacarebluetoothdevices.etc.Const.THERMOMETER_UUID_CHARACTER_RECEIVE
import com.test.emmacarebluetoothdevices.etc.Const.THERMOMETER_UUID_MODIFY_BT_NAME
import com.test.emmacarebluetoothdevices.etc.Const.THERMOMETER_UUID_SERVICE_DATA
import com.test.emmacarebluetoothdevices.etc.Const.TONOMETER
import com.test.emmacarebluetoothdevices.etc.Const.TONOMETER_UUID_CHARACTER_RECEIVE
import com.test.emmacarebluetoothdevices.etc.Const.TONOMETER_UUID_MODIFY_BT_NAME
import com.test.emmacarebluetoothdevices.etc.Const.TONOMETER_UUID_SERVICE_DATA
import com.test.emmacarebluetoothdevices.services.BluetoothService
import com.test.emmacarebluetoothdevices.services.BluetoothService.LocalBinder

class BluetoothController private constructor(private val stateListener: StateListener) {

    interface StateListener {
        fun onConnected()
        fun onDisconnected()
        fun onReceiveData(dat: ByteArray?)
    }

    private var bluetoothService: BluetoothService? = null
    private var receiveData: BluetoothGattCharacteristic? = null
    private var modifyName: BluetoothGattCharacteristic? = null
    private var selectedDeviceGlobal: String? = null
    var isConnected = false

    /**
     * connect the bluetooth device
     *
     * @param device
     */
    fun connect(device: BluetoothDevice, selectedDevice: String) {
        selectedDeviceGlobal = selectedDevice
        bluetoothService?.connect(device.address)
    }

    /**
     * Disconnect the bluetooth
     */
    fun disconnect() {
        bluetoothService?.disconnect()
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            componentName: ComponentName,
            service: IBinder
        ) {
            bluetoothService = (service as LocalBinder).service
            if (!bluetoothService?.initialize()!!) {
                Log.e(TAG, "Unable to initialize Bluetooth")
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothService = null
        }
    }

    fun bindService(context: Context) {
        val gattServiceIntent = Intent(context, BluetoothService::class.java)
        context.bindService(
            gattServiceIntent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun unbindService(context: Context) {
        context.unbindService(serviceConnection)
    }

    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_GATT_CONNECTED -> {
                    stateListener.onConnected()
                    isConnected = true
                }
                ACTION_GATT_DISCONNECTED -> {
                    stateListener.onDisconnected()
                    modifyName = null
                    receiveData = null
                    isConnected = false
                }
                ACTION_GATT_SERVICES_DISCOVERED -> {
                    // Show all the supported services and characteristics on the user interface.
                    initCharacteristic()
                    bluetoothService?.setCharacteristicNotification(receiveData!!, selectedDeviceGlobal!!)
                    bluetoothService?.writeToDevice(receiveData!!, selectedDeviceGlobal!!)
                }
                ACTION_DATA_AVAILABLE -> {
                    val data = intent.getByteArrayExtra(EXTRA_DATA)
                    stateListener.onReceiveData(data)
                }
            }
        }
    }

    fun initCharacteristic() {
        val services = bluetoothService?.supportedGattServices
        var dataService: BluetoothGattService? = null
        if (services == null) {
            return
        }

        services.forEach { service ->
            if (service.uuid == OXYMETER_UUID_SERVICE_DATA && selectedDeviceGlobal == OXYMETER
                || service.uuid == THERMOMETER_UUID_SERVICE_DATA && selectedDeviceGlobal == THERMOMETER
                || service.uuid == SCALES_UUID_SERVICE_DATA && selectedDeviceGlobal == SCALES
                || service.uuid == TONOMETER_UUID_SERVICE_DATA && selectedDeviceGlobal == TONOMETER
            ) {
                dataService = service
            }
        }

        if (dataService != null) {
            val characteristics = dataService?.characteristics
            if (characteristics != null) {
                for (ch in characteristics) {
                    if (ch.uuid == OXYMETER_UUID_CHARACTER_RECEIVE && selectedDeviceGlobal == OXYMETER
                        || ch.uuid == THERMOMETER_UUID_CHARACTER_RECEIVE && selectedDeviceGlobal == THERMOMETER
                        || ch.uuid == SCALES_UUID_CHARACTER_RECEIVE && selectedDeviceGlobal == SCALES
                        || ch.uuid == TONOMETER_UUID_CHARACTER_RECEIVE && selectedDeviceGlobal == TONOMETER
                    ) {
                        receiveData = ch
                    } else if (ch.uuid == OXYMETER_UUID_MODIFY_BT_NAME && selectedDeviceGlobal == OXYMETER
                        || ch.uuid == THERMOMETER_UUID_MODIFY_BT_NAME && selectedDeviceGlobal == THERMOMETER
                        || ch.uuid == SCALES_UUID_MODIFY_BT_NAME && selectedDeviceGlobal == SCALES
                        || ch.uuid == TONOMETER_UUID_MODIFY_BT_NAME && selectedDeviceGlobal == TONOMETER
                    ) {
                        modifyName = ch
                    }
                }
            }
        }
    }

    fun registerBtReceiver(context: Context) {
        context.registerReceiver(
            gattUpdateReceiver,
            makeGattUpdateIntentFilter()
        )
    }

    fun unregisterBtReceiver(context: Context) {
        context.unregisterReceiver(gattUpdateReceiver)
    }

    companion object {
        private val TAG = this.javaClass.name
        private lateinit var mBluetoothController: BluetoothController

        /**
         * Get a Controller
         *
         * @return
         */
        @JvmStatic
        fun getDefaultBleController(stateListener: StateListener): BluetoothController {
            mBluetoothController = BluetoothController(stateListener)
            return mBluetoothController
        }

        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(ACTION_GATT_CONNECTED)
            intentFilter.addAction(ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(ACTION_DATA_AVAILABLE)
            return intentFilter
        }
    }
}