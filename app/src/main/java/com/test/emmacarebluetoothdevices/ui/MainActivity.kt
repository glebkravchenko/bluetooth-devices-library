package com.test.emmacarebluetoothdevices.ui

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import c.tlgbltcn.library.BluetoothHelper
import c.tlgbltcn.library.BluetoothHelperListener
import com.sirvar.bluetoothkit.BluetoothKit
import com.test.emmacarebluetoothdevices.R
import com.test.emmacarebluetoothdevices.etc.Const.OXYMETER_DEVICE_ID
import kotlinx.android.synthetic.main.activity_main.*

const val OXYMETER = "BerryMed"
const val THERMOMETER = "Comper IR-FT-EECE5C281FCA"
const val TONOMETER = "Bluetooth BP"
const val SCALES = "SCALES"

private const val REQUEST_PERMISSION_COARSE_LOCATION = 0
private const val REQUEST_ENABLE_BT = 1

class MainActivity : AppCompatActivity(), BluetoothHelperListener, BluetoothListAdapter.Listener {

    private lateinit var bluetoothHelper: BluetoothHelper
    private lateinit var viewAdapter: RecyclerView.Adapter<*>

    private var itemList = ArrayList<BluetoothDevice>()
    private var bluetoothKit = BluetoothKit()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothHelper = BluetoothHelper(this@MainActivity, this@MainActivity)
            .setPermissionRequired(true)
            .create()

        if (bluetoothHelper.isBluetoothScanning()) {
            btnSearch.text = "Stop discovery"
        } else {
            btnSearch.text = "Start discovery"
        }

        btnSearch.setOnClickListener {
            if (bluetoothHelper.isBluetoothScanning()) {
                bluetoothHelper.stopDiscovery()
            } else {
                bluetoothHelper.startDiscovery()
            }
        }

        viewAdapter = BluetoothListAdapter(itemList, this)
        recycler_view.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = viewAdapter
        }
    }

    override fun onItemClickListener(device: BluetoothDevice) {
        bluetoothKit.write(3)
        bluetoothKit.connect(device, OXYMETER_DEVICE_ID)
    }

    override fun onStartDiscovery() {
        btnSearch.text = "Stop discovery"
    }

    override fun onFinishDiscovery() {
        btnSearch.text = "Start discovery"
        itemList.clear()
    }

    override fun onEnabledBluetooth() { }

    override fun onDisabledBluetooh() { }

    override fun getBluetoothDeviceList(device: BluetoothDevice) {
        itemList.add(device)
        viewAdapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        bluetoothHelper.registerBluetoothStateChanged()
    }


    override fun onStop() {
        super.onStop()
        bluetoothHelper.unregisterBluetoothStateChanged()
    }
}