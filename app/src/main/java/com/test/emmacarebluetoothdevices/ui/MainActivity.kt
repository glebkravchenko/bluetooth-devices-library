package com.test.emmacarebluetoothdevices.ui

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import c.tlgbltcn.library.BluetoothHelper
import c.tlgbltcn.library.BluetoothHelperListener
import com.test.emmacarebluetoothdevices.R
import com.test.emmacarebluetoothdevices.etc.DataParser
import com.test.emmacarebluetoothdevices.services.controller.BluetoothController
import com.test.emmacarebluetoothdevices.ui.adapter.BluetoothListAdapter
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), BluetoothHelperListener, BluetoothListAdapter.Listener,
    BluetoothController.StateListener {

    private lateinit var bluetoothHelper: BluetoothHelper
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var dataParser: DataParser

    private var itemList = ArrayList<BluetoothDevice>()
    private var bluetoothKit = BluetoothController.getDefaultBleController(this)

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

        bluetoothKit.bindService(this)

        btnSearch.setOnClickListener {
            if (bluetoothHelper.isBluetoothScanning()) {
                bluetoothHelper.stopDiscovery()
                bluetoothKit.disconnect()
            } else {
                itemList.clear()
                bluetoothHelper.startDiscovery()
            }
        }

        viewAdapter = BluetoothListAdapter(itemList, this)
        recycler_view.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(
                DividerItemDecoration(
                    this@MainActivity,
                    LinearLayoutManager.VERTICAL
                )
            )
            adapter = viewAdapter
        }

        dataParser = DataParser(object : DataParser.PackageReceivedListener {
            override fun onOxiParamsChanged(params: DataParser.OxiParams?) {
                tvParams.text = getString(R.string.spot_and_pulse, params?.spo2, params?.pulseRate)
            }

            override fun onPlethWaveReceived(amp: Int) {}
        })
        dataParser.start()

    }

    override fun onItemClickListener(device: BluetoothDevice) {
        bluetoothKit.connect(device)
    }

    override fun onStartDiscovery() {
        btnSearch.text = "Stop discovery"
    }

    override fun onFinishDiscovery() {
        btnSearch.text = "Start discovery"
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
        bluetoothKit.registerBtReceiver(this)
        bluetoothKit.registerBluetoothAdapterReceiver(this)
    }

    override fun onStop() {
        super.onStop()
        bluetoothHelper.unregisterBluetoothStateChanged()
    }

    override fun onPause() {
        super.onPause()
        bluetoothKit.unregisterBtReceiver(this)
        bluetoothKit.unregisterBluetoothAdapterReceiver(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothKit.unbindService(this)
        dataParser.stop()
    }

    override fun onConnected() {
        btnSearch.text = "Connected"
    }

    override fun onDisconnected() {
        btnSearch.text = "Disconnected"
    }

    override fun onReceiveData(dat: ByteArray?) {
        dataParser.add(dat!!)

        Log.e("MainActivity", "Temperature " + dataParser.getTemperature(dat))
    }

    override fun onCheckPermission() {
//        TODO("Not yet implemented")
    }

    override fun onBluetoothEnabled() {
//        TODO("Not yet implemented")
    }
}