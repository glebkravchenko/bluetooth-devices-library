package com.test.emmacarebluetoothdevices.services.controller

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.os.IBinder
import android.util.Log
import com.test.emmacarebluetoothdevices.etc.Const
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
    var isConnected = false

    /**
     * connect the bluetooth device
     *
     * @param device
     */
    fun connect(device: BluetoothDevice) {
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
                BluetoothService.ACTION_GATT_CONNECTED -> {
                    stateListener.onConnected()
                    isConnected = true
                }
                BluetoothService.ACTION_GATT_DISCONNECTED -> {
                    stateListener.onDisconnected()
                    modifyName = null
                    receiveData = null
                    isConnected = false
                }
                BluetoothService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    // Show all the supported services and characteristics on the user interface.
                    initCharacteristic()
                    bluetoothService?.setCharacteristicNotification(receiveData!!)
                    bluetoothService?.writeToDevice(receiveData!!)
                }
                BluetoothService.ACTION_DATA_AVAILABLE -> {
                    val data = intent.getByteArrayExtra(BluetoothService.EXTRA_DATA)
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
            if (service.uuid == Const.OXYMETER_UUID_SERVICE_DATA
                || service.uuid == Const.THERMOMETER_UUID_SERVICE_DATA
                || service.uuid == Const.SCALES_UUID_SERVICE_DATA
                || service.uuid == Const.TONOMETER_UUID_SERVICE_DATA
            ) {
                dataService = service
            }
        }

        if (dataService != null) {
            val characteristics = dataService?.characteristics
            if (characteristics != null) {
                for (ch in characteristics) {
                    if (ch.uuid == Const.OXYMETER_UUID_CHARACTER_RECEIVE
                        || ch.uuid == Const.THERMOMETER_UUID_CHARACTER_RECEIVE
                        || ch.uuid == Const.SCALES_UUID_CHARACTER_RECEIVE
                        || ch.uuid == Const.TONOMETER_UUID_CHARACTER_RECEIVE
                    ) {
                        receiveData = ch
                    } else if (ch.uuid == Const.OXYMETER_UUID_MODIFY_BT_NAME
                        || ch.uuid == Const.THERMOMETER_UUID_MODIFY_BT_NAME
                        || ch.uuid == Const.SCALES_UUID_MODIFY_BT_NAME
                        || ch.uuid == Const.TONOMETER_UUID_MODIFY_BT_NAME
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
            intentFilter.addAction(BluetoothService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BluetoothService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BluetoothService.ACTION_DATA_AVAILABLE)
            return intentFilter
        }
    }
}