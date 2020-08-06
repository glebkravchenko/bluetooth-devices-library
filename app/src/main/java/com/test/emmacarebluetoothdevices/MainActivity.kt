package com.test.emmacarebluetoothdevices

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.test.emmacarebluetoothdevices.ble.BleController
import com.test.emmacarebluetoothdevices.ble.BleController.Companion.getDefaultBleController
import com.test.emmacarebluetoothdevices.ble.BleController.StateListener
import com.test.emmacarebluetoothdevices.data.DataParser
import com.test.emmacarebluetoothdevices.data.DataParser.OxiParams
import com.test.emmacarebluetoothdevices.data.DataParser.PackageReceivedListener
import com.test.emmacarebluetoothdevices.dialog.DeviceListAdapter
import com.test.emmacarebluetoothdevices.dialog.SearchDevicesDialog
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), StateListener {

    private var dataParser: DataParser? = null
    private var bleControl: BleController? = null
    private var searchDialog: SearchDevicesDialog? = null
    private var btDevicesAdapter: DeviceListAdapter? = null
    private val btDevices = ArrayList<BluetoothDevice?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dataParser = DataParser(object : PackageReceivedListener {
            override fun onOxiParamsChanged(params: OxiParams?) {
                tvParams.text = "SpO2: ${params?.spo2}   Pulse Rate: ${params?.pulseRate}"
            }

            override fun onPlethWaveReceived(amp: Int) {
                wfvPleth.addAmp(amp)
            }
        })

        dataParser!!.start()
        bleControl = getDefaultBleController(this)
        bleControl!!.enableBtAdapter()
        bleControl!!.bindService(this)
        btDevicesAdapter = DeviceListAdapter(this, R.layout.device_adapter_view, btDevices)
        searchDialog = object : SearchDevicesDialog(this, btDevicesAdapter) {
            override fun onSearchButtonClicked() {
                btDevices.clear()
                btDevicesAdapter!!.notifyDataSetChanged()
                bleControl!!.scanLeDevice(this@MainActivity)
            }

            override fun onClickDeviceItem(pos: Int) {
                val device = btDevices[pos]
                bleControl!!.connect(device!!)
                dismiss()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bleControl!!.registerBtReceiver(this)
    }

    override fun onPause() {
        super.onPause()
        bleControl!!.unregisterBtReceiver(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        dataParser!!.stop()
        bleControl!!.unbindService(this)
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.btnSearch -> if (!bleControl!!.isConnected) {
                bleControl!!.scanLeDevice(this)
                searchDialog!!.show()
                btDevices.clear()
                btDevicesAdapter!!.notifyDataSetChanged()
            } else {
                bleControl!!.disconnect()
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
        btnSearch.text = "Disconnect"
        Toast.makeText(this@MainActivity, "Connected", Toast.LENGTH_SHORT).show()
    }

    override fun onDisconnected() {
        Toast.makeText(this@MainActivity, "Disconnected", Toast.LENGTH_SHORT).show()
        btnSearch.text = "Search"
    }

    override fun onReceiveData(dat: ByteArray?) {
        dataParser!!.add(dat!!)
    }

    override fun onServicesDiscovered() { }

    override fun onScanStop() {
        searchDialog!!.stopSearch()
    }

    override fun checkPermission() {
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
}