package com.test.emmacare_bluettooth.services.controller

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.os.IBinder
import android.util.Log
import com.test.emmacare_bluettooth.etc.Const.ACTION_DATA_AVAILABLE
import com.test.emmacare_bluettooth.etc.Const.ACTION_GATT_CONNECTED
import com.test.emmacare_bluettooth.etc.Const.ACTION_GATT_DISCONNECTED
import com.test.emmacare_bluettooth.etc.Const.ACTION_GATT_SERVICES_DISCOVERED
import com.test.emmacare_bluettooth.etc.Const.EXTRA_DATA
import com.test.emmacare_bluettooth.etc.Const.OXYMETER
import com.test.emmacare_bluettooth.etc.Const.OXYMETER_UUID_CHARACTER_NOTIFY
import com.test.emmacare_bluettooth.etc.Const.OXYMETER_UUID_CHARACTER_WRITE
import com.test.emmacare_bluettooth.etc.Const.OXYMETER_UUID_SERVICE
import com.test.emmacare_bluettooth.etc.Const.SCALES
import com.test.emmacare_bluettooth.etc.Const.THERMOMETER
import com.test.emmacare_bluettooth.etc.Const.THERMOMETER_UUID_CHARACTER_NOTIFY
import com.test.emmacare_bluettooth.etc.Const.THERMOMETER_UUID_CHARACTER_WRITE
import com.test.emmacare_bluettooth.etc.Const.THERMOMETER_UUID_SERVICE
import com.test.emmacare_bluettooth.etc.Const.TONOMETER
import com.test.emmacare_bluettooth.etc.Const.TONOMETER_UUID_CHARACTER_NOTIFY
import com.test.emmacare_bluettooth.etc.Const.TONOMETER_UUID_CHARACTER_WRITE
import com.test.emmacare_bluettooth.etc.Const.TONOMETER_UUID_SERVICE
import com.test.emmacare_bluettooth.services.BluetoothService
import com.test.emmacare_bluettooth.services.BluetoothService.LocalBinder
import com.test.emmacare_bluettooth.services.controller.scales.BluetoothUtils
import com.test.emmacare_bluettooth.services.controller.scales.ScalesController

class BluetoothController(private val stateListener: StateListener) :
    ScalesController.MeasurementResultListener {

    interface StateListener {
        fun onConnected()
        fun onDisconnected()
        fun onReceiveData(dat: ByteArray?)
    }

    private var bluetoothService: BluetoothService? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var selectedDeviceGlobal: String? = null
    var isConnected = false

    init {
        BluetoothUtils.init(context)
        ScalesController.setListener(this)
    }

    /**
     * connect the bluetooth device
     *
     * @param device
     */
    fun connect(device: BluetoothDevice, selectedDevice: String) {
        selectedDeviceGlobal = selectedDevice
        if (selectedDevice != SCALES) {
            bluetoothService?.connect(device.address)
        } else {
            ScalesController.connect(device.address)
        }
    }

    /**
     * Disconnect the bluetooth
     */
    fun disconnect() {
        bluetoothService?.disconnect()
    }

    /**
     *
     * @serviceConnection service which responsible for connection to tonometer, thermometer or oximeter
     *
     */
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

    /**
     *
     * @gattUpdateReceiver is receiver which says whether device connected or not
     * and when receives from device some data
     *
     */
    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_GATT_CONNECTED -> {
                    stateListener.onConnected()
                    isConnected = true
                }
                ACTION_GATT_DISCONNECTED -> {
                    stateListener.onDisconnected()
                    notifyCharacteristic = null
                    isConnected = false
                }
                ACTION_GATT_SERVICES_DISCOVERED -> {
                    // Show all the supported services and characteristics on the user interface.
                    initCharacteristic()
                    notifyCharacteristic?.let { gattCharacteristic ->
                        bluetoothService?.setCharacteristicNotification(
                            gattCharacteristic,
                            selectedDeviceGlobal.toString()
                        )
                    }
                    writeCharacteristic?.let { gattCharacteristic ->
                        bluetoothService?.writeToStartMeasurement(
                            gattCharacteristic,
                            selectedDeviceGlobal.toString()
                        )
                    }
                }
                ACTION_DATA_AVAILABLE -> {
                    val data = intent.getByteArrayExtra(EXTRA_DATA)
                    writeCharacteristic?.let { gattCharacteristic ->
                        selectedDeviceGlobal?.let { device ->
                            bluetoothService?.writeToEndMeasurement(
                                gattCharacteristic,
                                device,
                                data
                            )
                        }
                    }
                    stateListener.onReceiveData(data)
                }
            }
        }
    }

    /**
     *
     * @onScalesMeasurementFetched responsible for scales measurement result
     *
     */
    override fun onScalesMeasurementFetched(byteArray: ByteArray?) {
        stateListener.onReceiveData(byteArray)
    }

    /**
     *
     * @onScalesConnected responsible for state connected for scales
     *
     */
    override fun onScalesConnected() {
        stateListener.onConnected()
    }

    /**
     *
     * @onScalesDisconnected responsible for state disconnected for scales
     *
     */
    override fun onScalesDisconnected() {
        stateListener.onDisconnected()
    }

    fun initCharacteristic() {
        val services = bluetoothService?.supportedGattServices
        var dataService: BluetoothGattService? = null
        if (services == null) {
            return
        }

        services.forEach { service ->
            if (service.uuid == OXYMETER_UUID_SERVICE && selectedDeviceGlobal == OXYMETER
                || service.uuid == THERMOMETER_UUID_SERVICE && selectedDeviceGlobal == THERMOMETER
                || service.uuid == TONOMETER_UUID_SERVICE && selectedDeviceGlobal == TONOMETER
            ) {
                dataService = service
            }
        }

        if (dataService != null) {
            val characteristics = dataService?.characteristics
            if (characteristics != null) {
                for (gattCharacteristic in characteristics) {
                    if (gattCharacteristic.uuid == OXYMETER_UUID_CHARACTER_NOTIFY && selectedDeviceGlobal == OXYMETER
                        || gattCharacteristic.uuid == THERMOMETER_UUID_CHARACTER_NOTIFY && selectedDeviceGlobal == THERMOMETER
                        || gattCharacteristic.uuid == TONOMETER_UUID_CHARACTER_NOTIFY && selectedDeviceGlobal == TONOMETER
                    ) {
                        notifyCharacteristic = gattCharacteristic
                    } else if (gattCharacteristic.uuid == OXYMETER_UUID_CHARACTER_WRITE && selectedDeviceGlobal == OXYMETER
                        || gattCharacteristic.uuid == THERMOMETER_UUID_CHARACTER_WRITE && selectedDeviceGlobal == THERMOMETER
                        || gattCharacteristic.uuid == TONOMETER_UUID_CHARACTER_WRITE && selectedDeviceGlobal == TONOMETER
                    ) {
                        writeCharacteristic = gattCharacteristic
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

    fun onDestroyBLE() {
        ScalesController.disconnect()
    }

    companion object {
        private val TAG = this.javaClass.name
        private lateinit var bluetoothController: BluetoothController
        private lateinit var context: Context

        /**
         * Get a Controller
         *
         * @return
         */
        @JvmStatic
        fun getDefaultBleController(
            context: Context,
            stateListener: StateListener
        ): BluetoothController {
            this.context = context
            this.bluetoothController = BluetoothController(stateListener)
            return bluetoothController
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