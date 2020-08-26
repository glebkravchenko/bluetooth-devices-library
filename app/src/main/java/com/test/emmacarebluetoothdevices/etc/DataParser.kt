package com.test.emmacarebluetoothdevices.etc

import android.util.Log
import java.util.concurrent.LinkedBlockingQueue

/**
 * Created by ZXX on 2016/1/8.
 */
class DataParser(private val mPackageReceivedListener: PackageReceivedListener) {

    private var TAG = this.javaClass.simpleName
    private val bufferQueue = LinkedBlockingQueue<Int>(256)
    private var mParseRunnable: ParseRunnable? = null
    private var isStop = true
    private val mOxiParams by lazy { OxiParams() }
    private lateinit var packageData: IntArray

    /**
     * interface for parameters changed.
     */
    interface PackageReceivedListener {
        fun onOxiParamsChanged(params: OxiParams?)
        fun onPlethWaveReceived(amp: Int)
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
                    if (spo2 != mOxiParams.spo2 || pulseRate != mOxiParams.pulseRate || pi != mOxiParams.pi) {
                        mOxiParams.update(spo2, pulseRate, pi)
                        mPackageReceivedListener.onOxiParamsChanged(mOxiParams)
                    }
                    mPackageReceivedListener.onPlethWaveReceived(packageData[1])
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
        Log.e(TAG, "add: " + dat.contentToString())
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

    fun getTemperature(dataArray: ByteArray) : Int {
        val first = dataArray[8].toInt() and 0xff
        val second = dataArray[9].toInt() and 0xff
        val hex = first.times(second)
        return hex.div(100)
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