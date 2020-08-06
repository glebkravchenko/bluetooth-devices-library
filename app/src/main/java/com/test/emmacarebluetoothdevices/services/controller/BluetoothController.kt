package com.test.emmacarebluetoothdevices.services.controller

import android.bluetooth.BluetoothAdapter
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
        fun onFoundDevice(device: BluetoothDevice?)
        fun onConnected()
        fun onDisconnected()
        fun onReceiveData(dat: ByteArray?)
        fun onCheckPermission()
        fun onBluetoothEnabled()
    }

    private val btAdapter: BluetoothAdapter by lazy { BluetoothAdapter.getDefaultAdapter() }
    private var bluetoothService: BluetoothService? = null
    private var receiveData: BluetoothGattCharacteristic? = null
    private var modifyName: BluetoothGattCharacteristic? = null
    var isConnected = false

    /**
     * enable bluetooth adapter
     */
    fun enableBtAdapter(context: Context) {
        if (!btAdapter.isEnabled) {
            btAdapter.enable()
            registerBluetoothAdapterReceiver(context)
        } else {
            stateListener.onBluetoothEnabled()
        }
    }

    /**
     * connect the bluetooth device
     *
     * @param device
     */
    fun connect(device: BluetoothDevice) {
        bluetoothService!!.connect(device.address)
    }

    /**
     * Disconnect the bluetooth
     */
    fun disconnect() {
        bluetoothService!!.disconnect()
    }

    /**
     * Scan bluetooth devices
     *
     * @param enable
     */
    fun scanLeDevice(context: Context) {
        if (btAdapter.isDiscovering) {
            btAdapter.cancelDiscovery()

            stateListener.onCheckPermission()
            btAdapter.startDiscovery()
            registerFoundReceiver(context)
        }
        if (!btAdapter.isDiscovering) {
            stateListener.onCheckPermission()
            btAdapter.startDiscovery()
            registerFoundReceiver(context)
        }
    }

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            componentName: ComponentName,
            service: IBinder
        ) {
            bluetoothService = (service as LocalBinder).service
            if (!bluetoothService!!.initialize()) {
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
            mServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun unbindService(context: Context) {
        context.unbindService(mServiceConnection)
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
                    try {
                        initCharacteristic()
                        bluetoothService!!.setCharacteristicNotification(receiveData!!, true)
                    } catch (e: Exception) {
                        disconnect()
                    }
                }
                BluetoothService.ACTION_DATA_AVAILABLE -> {
                    Log.e(TAG, "onReceive: " + intent.getStringExtra(BluetoothService.EXTRA_DATA))
                }
                BluetoothService.ACTION_SPO2_DATA_AVAILABLE -> {
                    stateListener.onReceiveData(intent.getByteArrayExtra(BluetoothService.EXTRA_DATA))
                }
            }
        }
    }

    private val foundDeviceBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (action == BluetoothDevice.ACTION_FOUND) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                stateListener.onFoundDevice(device)
            }
        }
    }

    private val detectBluetoothAdapterAvailability: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_TURNING_ON -> stateListener.onBluetoothEnabled()
                }
            }
        }
    }

    fun initCharacteristic() {
        val services = bluetoothService!!.supportedGattServices
        var mDataService: BluetoothGattService? = null
        if (services == null) return
        for (service in services) {
            if (service.uuid == Const.UUID_SERVICE_DATA) {
                mDataService = service
            }
        }
        if (mDataService != null) {
            val characteristics = mDataService.characteristics
            for (ch in characteristics) {
                if (ch.uuid == Const.UUID_CHARACTER_RECEIVE) {
                    receiveData = ch
                } else if (ch.uuid == Const.UUID_MODIFY_BT_NAME) {
                    modifyName = ch
                }
            }
        }
    }

    fun registerBtReceiver(context: Context) {
        context.registerReceiver(gattUpdateReceiver,
            makeGattUpdateIntentFilter()
        )
    }

    fun unregisterBtReceiver(context: Context) {
        context.unregisterReceiver(gattUpdateReceiver)
    }

    fun registerFoundReceiver(context: Context) {
        context.registerReceiver(foundDeviceBroadcastReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

    fun unregisterFoundReceiver(context: Context) {
        context.unregisterReceiver(foundDeviceBroadcastReceiver)
    }

    fun registerBluetoothAdapterReceiver(context: Context) {
        context.registerReceiver(detectBluetoothAdapterAvailability, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    fun unregisterBluetoothAdapterReceiver(context: Context) {
        context.unregisterReceiver(detectBluetoothAdapterAvailability)
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
            intentFilter.addAction(BluetoothService.ACTION_SPO2_DATA_AVAILABLE)
            return intentFilter
        }
    }
}