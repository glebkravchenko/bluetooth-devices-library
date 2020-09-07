package com.test.emmacare_bluettooth.services.controller.scales.units

import kotlin.experimental.or

fun hexStr2ByteArray(hexString: String): ByteArray? {
    var hexString = hexString
    hexString = hexString.toLowerCase()
    val byteArray = ByteArray(hexString.length / 2)
    var k = 0
    for (i in byteArray.indices) {
        val high =
            (Character.digit(hexString[k], 16) and 0xff).toByte()
        val low =
            (Character.digit(hexString[k + 1], 16) and 0xff).toByte()
        byteArray[i] = high.toInt().toBigInteger().shl(4).toByte() or low
        k += 2
    }
    return byteArray
}