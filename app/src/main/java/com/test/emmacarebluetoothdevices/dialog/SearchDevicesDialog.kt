package com.test.emmacarebluetoothdevices.dialog

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.Window
import android.widget.AdapterView
import com.test.emmacarebluetoothdevices.R
import kotlinx.android.synthetic.main.devices_dialog.*

/**
 * Created by ZXX on 2017/4/28.
 */
abstract class SearchDevicesDialog(context: Context?, adapter: DeviceListAdapter?) : Dialog(context!!) {

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.devices_dialog)

        lvBluetoothDevices.adapter = adapter
        lvBluetoothDevices.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ -> onClickDeviceItem(position) }
        btnSearchDevices.setOnClickListener { startSearch() }
    }

    fun stopSearch() {
        pbSearchDevices.visibility = View.GONE
        btnSearchDevices.visibility = View.VISIBLE
    }

    private fun startSearch() {
        onSearchButtonClicked()
        pbSearchDevices.visibility = View.VISIBLE
        btnSearchDevices.visibility = View.GONE
    }

    override fun show() {
        super.show()
        startSearch()
    }

    abstract fun onSearchButtonClicked()
    abstract fun onClickDeviceItem(pos: Int)
}