package com.test.emmacarebluetoothdevices.ui

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.view.View
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

    private var dataParser: DataParser? = null
    private var bluetoothControl: BluetoothController? = null
    private var searchDialog: SearchDevicesDialog? = null
    private var btDevicesAdapter: DeviceListAdapter? = null
    private val btDevices = ArrayList<BluetoothDevice?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dataParser = DataParser(object : PackageReceivedListener {
            override fun onOxiParamsChanged(params: OxiParams?) {
                tvParams.text = getString(R.string.spot_and_pulse, params?.spo2, params?.pulseRate)
            }

            override fun onPlethWaveReceived(amp: Int) {
                wfvPleth.addAmp(amp)
            }
        })

        dataParser!!.start()
        bluetoothControl = getDefaultBleController(this)
        bluetoothControl!!.bindService(this)
        btDevicesAdapter =
            DeviceListAdapter(
                this,
                R.layout.item_devices,
                btDevices
            )
        searchDialog = object : SearchDevicesDialog(this, btDevicesAdapter) {
            override fun onClickDeviceItem(pos: Int) {
                val device = btDevices[pos]
                bluetoothControl?.connect(device!!)
                dismiss()
            }
        }
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.btnSearch -> if (!bluetoothControl!!.isConnected) {
                bluetoothControl?.enableBtAdapter(this)
            } else {
                bluetoothControl!!.disconnect()
            }
        }
    }

    override fun onFoundDevice(device: BluetoothDevice?) {
        if (!btDevices.contains(device)) {
            btDevices.add(device)
            btDevicesAdapter!!.notifyDataSetChanged()
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
        dataParser!!.add(dat!!)
    }

    override fun onScanStop() {
        searchDialog!!.stopSearch()
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
        bluetoothControl?.scanLeDevice(this)
        searchDialog!!.show()

        btDevices.clear()
        btDevicesAdapter!!.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        bluetoothControl?.registerBtReceiver(this)
        bluetoothControl?.registerFoundReceiver(this)
        bluetoothControl?.registerBluetoothAdapterReceiver(this)
    }

    override fun onPause() {
        super.onPause()
        bluetoothControl?.unregisterBtReceiver(this)
        bluetoothControl?.unregisterFoundReceiver(this)
        bluetoothControl?.unregisterBluetoothAdapterReceiver(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        dataParser?.stop()
        bluetoothControl?.unbindService(this)
    }
}