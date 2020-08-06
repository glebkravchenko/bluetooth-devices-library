package com.test.emmacarebluetoothdevices.dialog

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.item_devices.view.*
import java.util.*

class DeviceListAdapter(context: Context, tvResourceId: Int, private val mDevices: ArrayList<BluetoothDevice?>): ArrayAdapter<BluetoothDevice>(context, tvResourceId, mDevices) {

    private val layoutInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val viewResourceId: Int = tvResourceId

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = layoutInflater.inflate(viewResourceId, null)
        val device = mDevices[position]
        view.tvDeviceName.text = device?.name
        view.tvDeviceAddress.text = device?.address
        return view
    }
}