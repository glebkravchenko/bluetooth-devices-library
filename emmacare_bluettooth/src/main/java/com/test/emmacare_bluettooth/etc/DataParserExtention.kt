package com.test.emmacare_bluettooth.etc

/**
 * Convert dat to temperature in fahrenheit
 *
 * @return
 */
fun ByteArray.getTemperatureInFahrenheit() : Float {
    val first = String.format("%02x", this[8])
    val second = String.format("%02x", this[9])
    val hex16 = Integer.parseInt("$first$second", 16).toFloat()
    val celsius = String.format("%.1f", hex16.div(100)).toFloat()
    return ((celsius * 9) / 5) + 32
}

fun ByteArray.getWeightInLb() : Double {
    val first = String.format("%02x", this[4])
    val second = String.format("%02x", this[3])
    val hex16 = Integer.parseInt("$first$second", 16).toFloat()
    val kg = String.format("%.1f", hex16.div(100)).toFloat()
    val change = 2.2046226218
    return kg * change
}

/**
 * Convert dat to SpO2
 *
 * @return
 */
fun ByteArray.getSpO2() : Int {
    return this[4].toInt()
}

/**
 * Convert dat to PulseRate
 *
 * @return
 */
fun ByteArray.getPulseRate() : Int {
    return this[3].toInt() or (this[2].toInt() and 0x40 shl 1)
}

/**
 * Convert dat to PI
 *
 * @return
 */
fun ByteArray.getPI() : Int {
    return this[0].toInt() and 0x0f
}

/**
 * Convert dat to SYS
 *
 * @return
 */
fun ByteArray.getSYS() : Int {
    return this[3].toInt()
}


/**
 * Convert dat to SYS
 *
 * @return
 */
fun ByteArray.getDIA() : Int {
    return this[4].toInt()
}

/**
 * Convert dat to Pulse
 *
 * @return
 */
fun ByteArray.getPulse(isPumping: Boolean) : Int {
    return this[if(isPumping) 4 else 5].toInt()
}