package com.test.emmacare_bluettooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.test.emmacare_bluettooth.bluetooth.BluetoothUtils
import com.test.emmacare_bluettooth.devices.OxymeterController
import com.test.emmacare_bluettooth.devices.ScalesController
import com.test.emmacare_bluettooth.devices.ThermometerController
import com.test.emmacare_bluettooth.devices.TonometerController
import com.test.emmacare_bluettooth.devices.listener.MeasurementResultListener
import com.test.emmacare_bluettooth.etc.Const.OXYMETER
import com.test.emmacare_bluettooth.etc.Const.OXYMETER_UUID_CHARACTER_NOTIFY
import com.test.emmacare_bluettooth.etc.Const.OXYMETER_UUID_CHARACTER_WRITE
import com.test.emmacare_bluettooth.etc.Const.OXYMETER_UUID_SERVICE
import com.test.emmacare_bluettooth.etc.Const.SCALES
import com.test.emmacare_bluettooth.etc.Const.SCALES_UUID_CHARACTER_NOTIFY
import com.test.emmacare_bluettooth.etc.Const.SCALES_UUID_CHARACTER_WRITE
import com.test.emmacare_bluettooth.etc.Const.SCALES_UUID_SERVICE
import com.test.emmacare_bluettooth.etc.Const.THERMOMETER
import com.test.emmacare_bluettooth.etc.Const.THERMOMETER_UUID_CHARACTER_NOTIFY
import com.test.emmacare_bluettooth.etc.Const.THERMOMETER_UUID_CHARACTER_WRITE
import com.test.emmacare_bluettooth.etc.Const.THERMOMETER_UUID_SERVICE
import com.test.emmacare_bluettooth.etc.Const.TONOMETER
import com.test.emmacare_bluettooth.etc.Const.TONOMETER_UUID_CHARACTER_NOTIFY
import com.test.emmacare_bluettooth.etc.Const.TONOMETER_UUID_CHARACTER_WRITE
import com.test.emmacare_bluettooth.etc.Const.TONOMETER_UUID_SERVICE

class BluetoothController(private val stateListener: StateListener) : MeasurementResultListener {

    interface StateListener {
        fun onConnected()
        fun onDisconnected()
        fun onReceiveData(dat: ByteArray?)
    }

    private val oxymeterController = OxymeterController()
    private val scalesController = ScalesController()
    private val thermometerController = ThermometerController()
    private val tonometerController = TonometerController()

    private var selectedDeviceGlobal: String? = null

    init {
        BluetoothUtils.init(context)

        oxymeterController.setListener(this)
        thermometerController.setListener(this)
        tonometerController.setListener(this)
        scalesController.setListener(this)
    }

    /**
     * connect the bluetooth device
     *
     * @param device
     */
    fun connect(device: BluetoothDevice, selectedDevice: String) {
        selectedDeviceGlobal = selectedDevice
        when (selectedDevice) {
            OXYMETER -> oxymeterController.connect(
                device.address,
                OXYMETER_UUID_SERVICE,
                OXYMETER_UUID_CHARACTER_WRITE,
                OXYMETER_UUID_CHARACTER_NOTIFY
            )
            THERMOMETER -> thermometerController.connect(
                device.address,
                THERMOMETER_UUID_SERVICE,
                THERMOMETER_UUID_CHARACTER_WRITE,
                THERMOMETER_UUID_CHARACTER_NOTIFY
            )
            TONOMETER -> tonometerController.connect(
                device.address,
                TONOMETER_UUID_SERVICE,
                TONOMETER_UUID_CHARACTER_WRITE,
                TONOMETER_UUID_CHARACTER_NOTIFY
            )
            SCALES -> scalesController.connect(
                device.address,
                SCALES_UUID_SERVICE,
                SCALES_UUID_CHARACTER_WRITE,
                SCALES_UUID_CHARACTER_NOTIFY
            )
        }
    }

    /**
     * Disconnect the bluetooth
     */
    fun disconnect() {
        oxymeterController.disconnect()
        thermometerController.disconnect()
        tonometerController.disconnect()
        scalesController.disconnect()
    }

    /**
     *
     * @onScalesMeasurementFetched responsible for scales measurement result
     *
     */
    override fun onMeasurementFetched(byteArray: ByteArray?) {
        stateListener.onReceiveData(byteArray)
    }

    /**
     *
     * @onScalesConnected responsible for state connected for scales
     *
     */
    override fun onConnected() {
        stateListener.onConnected()
    }

    /**
     *
     * @onScalesDisconnected responsible for state disconnected for scales
     *
     */
    override fun onDisconnected() {
        stateListener.onDisconnected()
    }

    companion object {
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
            Companion.context = context
            bluetoothController = BluetoothController(stateListener)
            return bluetoothController
        }
    }
}