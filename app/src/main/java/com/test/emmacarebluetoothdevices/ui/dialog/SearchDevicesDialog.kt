package com.test.emmacarebluetoothdevices.ui.dialog

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.AdapterView
import com.test.emmacarebluetoothdevices.R
import com.test.emmacarebluetoothdevices.ui.adapter.DeviceListAdapter
import kotlinx.android.synthetic.main.dialog_devices.*

class SearchDevicesDialog(context: Context?, adapter: DeviceListAdapter?) : Dialog(context!!) {

    private lateinit var callback: (position: Int, dialog: Dialog) -> Unit

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_devices)

        lvBluetoothDevices.adapter = adapter
        lvBluetoothDevices.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ -> callback.invoke(position, this) }
    }

    fun setListener(callback: (position: Int, dialog: Dialog) -> Unit) {
        this.callback = callback
    }
}