package com.test.emmacarebluetoothdevices.ui

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.test.emmacarebluetoothdevices.R
import com.test.emmacarebluetoothdevices.etc.DataParser
import com.test.emmacarebluetoothdevices.etc.DataParser.OxiParams
import com.test.emmacarebluetoothdevices.etc.DataParser.PackageReceivedListener
import com.test.emmacarebluetoothdevices.services.controller.BluetoothController
import com.test.emmacarebluetoothdevices.services.controller.BluetoothController.Companion.getDefaultBleController
import com.test.emmacarebluetoothdevices.ui.adapter.DeviceListAdapter
import com.test.emmacarebluetoothdevices.ui.dialog.SearchDevicesDialog
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), BluetoothController.StateListener {

    private lateinit var dataParser: DataParser
    private lateinit var bluetoothController: BluetoothController
    private lateinit var searchDialog: SearchDevicesDialog
    private lateinit var btDevicesAdapter: DeviceListAdapter
    private val btDevices = ArrayList<BluetoothDevice?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupDataParser()
        setupBluetoothController()
        setupSearchDialog()
        setupListeners()
    }

    private fun setupDataParser() {
        dataParser = DataParser(object : PackageReceivedListener {
            override fun onOxiParamsChanged(params: OxiParams?) {
                tvParams.text = getString(R.string.spot_and_pulse, params?.spo2, params?.pulseRate)
            }

            override fun onPlethWaveReceived(amp: Int) {
                wfvPleth.addAmp(amp)
            }
        })
        dataParser.start()
    }

    private fun setupBluetoothController() {
        bluetoothController = getDefaultBleController(this)
        bluetoothController.bindService(this)
        btDevicesAdapter = DeviceListAdapter(this, R.layout.item_devices, btDevices)
    }

    private fun setupSearchDialog() {
        searchDialog = SearchDevicesDialog(this, btDevicesAdapter)
        searchDialog.setListener { position, dialog ->
            val device = btDevices[position]
            bluetoothController.connect(device!!)
            dialog.dismiss()
        }
    }

    private fun setupListeners() {
        btnSearch.setOnClickListener {
            if (!bluetoothController.isConnected) {
                bluetoothController.enableBtAdapter(this)
            } else {
                bluetoothController.disconnect()
            }
        }
    }

    override fun onFoundDevice(device: BluetoothDevice?) {
        if (!btDevices.contains(device)) {
            btDevices.add(device)
            btDevicesAdapter.notifyDataSetChanged()
        }
    }

    override fun onConnected() {
        btnSearch.text = getString(R.string.disconnected)
        Toast.makeText(this@MainActivity, getString(R.string.connected), Toast.LENGTH_SHORT).show()
    }

    override fun onDisconnected() {
        Toast.makeText(this@MainActivity, getString(R.string.disconnected), Toast.LENGTH_SHORT).show()
        btnSearch.text = getString(R.string.search_oximeters)
    }

    override fun onReceiveData(dat: ByteArray?) {
        dataParser.add(dat!!)
    }

    override fun onCheckPermission() {
        var permissionCheck = checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION")
        permissionCheck += checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION")
        if (permissionCheck != 0) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), 1001
            )
        }
    }

    override fun onBluetoothEnabled() {
        bluetoothController.scanLeDevice(this)
        searchDialog.show()

        btDevices.clear()
        btDevicesAdapter!!.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        bluetoothController.registerBtReceiver(this)
        bluetoothController.registerFoundReceiver(this)
        bluetoothController.registerBluetoothAdapterReceiver(this)
    }

    override fun onPause() {
        super.onPause()
        bluetoothController.unregisterBtReceiver(this)
        bluetoothController.unregisterFoundReceiver(this)
        bluetoothController.unregisterBluetoothAdapterReceiver(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        dataParser.stop()
        bluetoothController.unbindService(this)
    }
}