package com.test.emmacarebluetoothdevices.etc

import java.util.*

/**
 * Created by ZXX on 2015/8/31.
 */
object Const {
//    val OXYMETER_DEVICE_ID: UUID
    val OXYMETER_UUID_SERVICE_DATA: UUID = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455")
    val OXYMETER_UUID_CHARACTER_RECEIVE: UUID = UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616")
    val OXYMETER_UUID_MODIFY_BT_NAME: UUID = UUID.fromString("00005343-0000-1000-8000-00805F9B34FB")
    val OXYMETER_UUID_CLIENT_CHARACTER_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

//    val THERMOMETER_DEVICE_ID: UUID
    val THERMOMETER_UUID_SERVICE_DATA: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    val THERMOMETER_UUID_CHARACTER_RECEIVE: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    val THERMOMETER_UUID_MODIFY_BT_NAME: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    val THERMOMETER_UUID_CLIENT_CHARACTER_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

//    val SCALES_DEVICE_ID: UUID
    val SCALES_UUID_SERVICE_DATA: UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
    val SCALES_UUID_CHARACTER_RECEIVE: UUID = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")
    val SCALES_UUID_MODIFY_BT_NAME: UUID = UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb")
    val SCALES_UUID_CLIENT_CHARACTER_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
//    val SCALES_UUID_CHARACTER_RECEIVE_V2: UUID = UUID.fromString("FFF0")

    //  TONOMETER_DEVICE_ID: UUID
    val TONOMETER_UUID_SERVICE_DATA: UUID = UUID.fromString("0000feba-0000-1000-8000-00805f9b34fb")
    val TONOMETER_UUID_CHARACTER_RECEIVE: UUID = UUID.fromString("0000fa10-0000-1000-8000-00805f9b34fb")
    val TONOMETER_UUID_MODIFY_BT_NAME: UUID = UUID.fromString("0000fa11-0000-1000-8000-00805f9b34fb")
    val TONOMETER_UUID_CLIENT_CHARACTER_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
//    val TONOMETER_UUID_CHARACTER_RECEIVE_V1: UUID = UUID.fromString("FFF1")
//    val TONOMETER_UUID_CHARACTER_RECEIVE_V2: UUID = UUID.fromString("FFF2")
}