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
                tvParams.text = getString(R.string.spot_and_pulse, params?.spo2, params?.pulseRate)
            }

            override fun onPlethWaveReceived(amp: Int) {
                wfvPleth.addAmp(amp)
            }
        })

        dataParser!!.start()
        bleControl = getDefaultBleController(this)
        bleControl!!.bindService(this)
        btDevicesAdapter = DeviceListAdapter(this, R.layout.item_devices, btDevices)
        searchDialog = object : SearchDevicesDialog(this, btDevicesAdapter) {
            override fun onClickDeviceItem(pos: Int) {
                val device = btDevices[pos]
                bleControl?.connect(device!!)
                dismiss()
            }
        }
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.btnSearch -> if (!bleControl!!.isConnected) {
                bleControl?.enableBtAdapter(this)
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
        bleControl?.scanLeDevice(this)
        searchDialog!!.show()

        btDevices.clear()
        btDevicesAdapter!!.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        bleControl?.registerBtReceiver(this)
        bleControl?.registerFoundReceiver(this)
        bleControl?.registerBluetoothAdapterReceiver(this)
    }

    override fun onPause() {
        super.onPause()
        bleControl?.unregisterBtReceiver(this)
        bleControl?.unregisterFoundReceiver(this)
        bleControl?.unregisterBluetoothAdapterReceiver(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        dataParser?.stop()
        bleControl?.unbindService(this)
    }
}