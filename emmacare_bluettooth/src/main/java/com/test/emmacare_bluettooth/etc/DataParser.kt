package com.test.emmacare_bluettooth.etc

import java.util.concurrent.LinkedBlockingQueue

class DataParser(private val packageReceivedListener: PackageReceivedListener) {

    private var TAG = this.javaClass.simpleName
    private val bufferQueue = LinkedBlockingQueue<Int>(256)
    private var mParseRunnable: ParseRunnable? = null
    private var isStop = true
    private val oxiParams by lazy { OxiParams() }
    private lateinit var packageData: IntArray

    /**
     * interface for parameters changed.
     */
    interface PackageReceivedListener {
        fun onOxiParamsChanged(params: OxiParams?)
    }

    fun start() {
        mParseRunnable = ParseRunnable()
        Thread(mParseRunnable).start()
    }

    fun stop() {
        isStop = true
    }

    /**
     * ParseRunnable
     */
    internal inner class ParseRunnable : Runnable {
        var dat = 0
        override fun run() {
            while (isStop) {
                dat = data
                packageData = IntArray(5)
                if (dat and 0x80 > 0) {
                    packageData[0] = dat
                    for (i in 1 until packageData.size) {
                        dat = data
                        if (dat and 0x80 == 0) {
                            packageData[i] = dat
                        } else {
                            continue
                        }
                    }
                    val spo2 = packageData[4]
                    val pulseRate = packageData[3] or (packageData[2] and 0x40 shl 1)
                    val pi = packageData[0] and 0x0f
                    if (spo2 != oxiParams.spo2 || pulseRate != oxiParams.pulseRate || pi != oxiParams.pi) {
                        oxiParams.update(spo2, pulseRate, pi)
                        packageReceivedListener.onOxiParamsChanged(oxiParams)
                    }
                }
            }
        }
    }

    /**
     * Add the data received from USB or Bluetooth
     *
     * @param dat
     */
    fun add(dat: ByteArray) {
        for (b in dat) {
            try {
                bufferQueue.put(toUnsignedInt(b))
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Get Dat from Queue
     *
     * @return
     */
    private val data: Int
        private get() {
            var dat = 0
            try {
                dat = bufferQueue.take()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            return dat
        }

    private fun toUnsignedInt(x: Byte): Int {
        return x.toInt() and 0xff
    }

    /**
     * Convert dat to temperature in fahrenheit
     *
     * @return
     */
    fun getTemperatureInFahrenheit(dataArray: ByteArray) : Float {
        val first = String.format("%02x", dataArray[8])
        val second = String.format("%02x", dataArray[9])
        val hex16 = Integer.parseInt("$first$second", 16).toFloat()
        val celsius = String.format("%.1f", hex16.div(100)).toFloat()
        return ((celsius * 9) / 5) + 32
    }

    /**
     * a small collection of Oximeter parameters.
     * you can add more parameters as the manual.
     *
     *
     * spo2          Pulse Oxygen Saturation
     * pulseRate     pulse rate
     * pi            perfusion index
     */
    inner class OxiParams {
        var spo2 = 0
            private set
        var pulseRate = 0
            private set
        var pi: Int = 0
            private set

        fun update(spo2: Int, pulseRate: Int, pi: Int) {
            this.spo2 = spo2
            this.pulseRate = pulseRate
            this.pi = pi
        }
    }
}