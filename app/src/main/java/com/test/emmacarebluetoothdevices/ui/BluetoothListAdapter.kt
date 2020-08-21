package com.test.emmacarebluetoothdevices.ui

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.test.emmacarebluetoothdevices.R
import kotlinx.android.synthetic.main.item_bluetooth_device.view.*

class BluetoothListAdapter(private var item: ArrayList<BluetoothDevice>, private val listener: Listener) :
    RecyclerView.Adapter<BluetoothListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bluetooth_device, parent, false)
        return ViewHolder(view as LinearLayout)
    }

    override fun getItemCount() = item.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.view.apply {
            device_name.text = item[position].name
            macAddress.text = item[position].address
        }

        holder.itemView.setOnClickListener { listener.onItemClickListener(item[position]) }
    }

    class ViewHolder(val view: LinearLayout) : RecyclerView.ViewHolder(view)

    interface Listener {
        fun onItemClickListener(device: BluetoothDevice)
    }
}
