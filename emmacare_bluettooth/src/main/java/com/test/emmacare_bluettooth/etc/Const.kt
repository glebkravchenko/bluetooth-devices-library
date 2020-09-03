package com.test.emmacare_bluettooth.etc

import java.util.*

/**
 * Created by ZXX on 2015/8/31.
 */
object Const {
//   Template for 16-bit uuid to 128 bit uuid
//
//   0000XXXX-0000-1000-8000-00805F9B34FB
//

    val UUID_CLIENT_CHARACTER_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

//    val OXYMETER_DEVICE_ID: UUID
    val OXYMETER_UUID_SERVICE: UUID = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455")
    val OXYMETER_UUID_CHARACTER_NOTIFY: UUID = UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616")
    val OXYMETER_UUID_CHARACTER_WRITE: UUID = UUID.fromString("00005343-0000-1000-8000-00805F9B34FB")

//    val THERMOMETER_DEVICE_ID: UUID
    val THERMOMETER_UUID_SERVICE: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    val THERMOMETER_UUID_CHARACTER_NOTIFY: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    val THERMOMETER_UUID_CHARACTER_WRITE: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

//    val SCALES_DEVICE_ID: UUID
    val SCALES_UUID_SERVICE: UUID = UUID.fromString("0000FFF0-0000-1000-8000-00805f9b34fb")
    val SCALES_UUID_CHARACTER_NOTIFY: UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb")
    val SCALES_UUID_CHARACTER_WRITE: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")

//    val TONOMETER_DEVICE_ID: UUID
    val TONOMETER_UUID_SERVICE: UUID = UUID.fromString("0000FFF0-0000-1000-8000-00805f9b34fb")
    val TONOMETER_UUID_CHARACTER_NOTIFY: UUID = UUID.fromString("0000FFF1-0000-1000-8000-00805f9b34fb")
    val TONOMETER_UUID_CHARACTER_WRITE: UUID = UUID.fromString("0000FFF2-0000-1000-8000-00805f9b34fb")

    // broadcast data
    const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
    const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
    const val ACTION_GATT_SERVICES_DISCOVERED =
        "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
    const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
    const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"

    // device types
    const val TONOMETER = "Tonometer"
    const val OXYMETER = "Oxymeter"
    const val SCALES = "Scales"
    const val THERMOMETER = "Thermometer"

    // device names
    const val NAME_OXYMETER = "BerryMed"
    const val NAME_TONOMETER = "Bluetooth BP"
    const val NAME_THERMOMETER = "Comper IR-FT-EECE5C281FCA"
    const val NAME_SCALE = "Health Scale"
}