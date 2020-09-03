package com.test.emmacarebluetoothdevices.ui

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import c.tlgbltcn.library.BluetoothHelper
import c.tlgbltcn.library.BluetoothHelperListener
import com.test.emmacare_bluettooth.etc.Const.NAME_OXYMETER
import com.test.emmacare_bluettooth.etc.Const.NAME_SCALE
import com.test.emmacare_bluettooth.etc.Const.NAME_THERMOMETER
import com.test.emmacare_bluettooth.etc.Const.NAME_TONOMETER
import com.test.emmacare_bluettooth.etc.Const.OXYMETER
import com.test.emmacare_bluettooth.etc.Const.SCALES
import com.test.emmacare_bluettooth.etc.Const.THERMOMETER
import com.test.emmacare_bluettooth.etc.Const.TONOMETER
import com.test.emmacare_bluettooth.etc.DataParser
import com.test.emmacare_bluettooth.services.controller.BluetoothController
import com.test.emmacarebluetoothdevices.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), BluetoothHelperListener,
    BluetoothController.StateListener,
    DataParser.PackageReceivedListener {

    private lateinit var bluetoothHelper: BluetoothHelper
    private lateinit var dataParser: DataParser

    private var itemList = ArrayList<BluetoothDevice>()
    private var bluetoothController = BluetoothController.getDefaultBleController(this)
    private var selectedDevice: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothController.bindService(this)
        bluetoothHelper = BluetoothHelper(this@MainActivity, this@MainActivity)
            .setPermissionRequired(true)
            .create()

        btn_connect.setOnClickListener {
            if (bluetoothHelper.isBluetoothScanning()) {
                bluetoothHelper.stopDiscovery()
                bluetoothController.disconnect()
            } else {
                itemList.clear()
                bluetoothHelper.startDiscovery()
            }
        }

        setupDataParser()
        setupSpinnerAdapter()
    }

    private fun setupDataParser() {
        dataParser = DataParser(this)
        dataParser.start()
    }

    override fun onOxiParamsChanged(params: DataParser.OxiParams?) {
        tvParams.text = getString(R.string.spot_and_pulse, params?.spo2, params?.pulseRate)
    }

    private fun setupSpinnerAdapter() {
        val devicesList = mutableListOf<String>()
        devicesList.add(OXYMETER)
        devicesList.add(TONOMETER)
        devicesList.add(THERMOMETER)
        devicesList.add(SCALES)

        val userAdapter: ArrayAdapter<*> =
            ArrayAdapter<String>(this, R.layout.item_spinner, devicesList)
        spinner.adapter = userAdapter
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedDevice = devicesList[position]
                bluetoothHelper.stopDiscovery()
                bluetoothController.disconnect()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onStartDiscovery() {}

    override fun onFinishDiscovery() {}

    override fun onEnabledBluetooth() {}

    override fun onDisabledBluetooh() {}

    override fun getBluetoothDeviceList(device: BluetoothDevice) {
        when (selectedDevice) {
            OXYMETER -> if (device.name == NAME_OXYMETER) {
                bluetoothController.connect(device, selectedDevice.toString())
            }
            TONOMETER -> if (device.name == NAME_TONOMETER) {
                bluetoothController.connect(device, selectedDevice.toString())
            }
            THERMOMETER -> if (device.name == NAME_THERMOMETER) {
                bluetoothController.connect(device, selectedDevice.toString())
            }
            SCALES -> if (device.name == NAME_SCALE) {
                bluetoothController.connect(device, selectedDevice.toString())
            }
        }
    }

    override fun onReceiveData(dat: ByteArray?) {
        when (selectedDevice) {
            OXYMETER -> dat?.let { data ->
                dataParser.add(data)
            }
            TONOMETER -> {
                when (dat?.size) {
                    7 -> tvParams.text = getString(R.string.pulse, dat[4])
                    8 -> tvParams.text = getString(R.string.blood_pressure, dat[3], dat[4], dat[5])
                }
            }
            THERMOMETER -> {
                val oneDigitAfterComma = String.format("%.1f", dat?.let { temperature ->
                    dataParser.getTemperatureInFahrenheit(temperature)
                })
                tvParams.text = getString(R.string.temperature, oneDigitAfterComma)
            }
            SCALES -> tvParams.text = dat?.contentToString()
        }
    }

    override fun onResume() {
        super.onResume()
        bluetoothHelper.registerBluetoothStateChanged()
        bluetoothController.registerBtReceiver(this)
    }

    override fun onStop() {
        super.onStop()
        bluetoothHelper.unregisterBluetoothStateChanged()
    }

    override fun onPause() {
        super.onPause()
        bluetoothController.unregisterBtReceiver(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothController.unbindService(this)
        dataParser.stop()
    }

    override fun onConnected() {
        btn_connect.text = getString(R.string.connected)
    }

    override fun onDisconnected() {
        btn_connect.text = getString(R.string.disconnected)
    }
}