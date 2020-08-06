package com.test.emmacarebluetoothdevices.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.test.emmacarebluetoothdevices.R

class WaveformView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private var mHeight = 0
    private var mWidth = 0
    private val mWavePaint: Paint
    private val mBackgroundPaint: Paint
    private val mSurfaceHolder: SurfaceHolder
    private var mCanvas: Canvas? = null
    private var mLastPoint: Point? = null
    private val pointStep: Float
    private val mLineWidth: Float
    private val mDataBuffer: IntArray
    private var mDataBufferIndex: Int
    private val mBufferSize: Int
    private val mMaxValue: Int
    private var isSurfaceViewAvailable = false

    init {
        val metrics = context.resources.displayMetrics
        val arr = context.theme
            .obtainStyledAttributes(attrs, R.styleable.WaveformView, defStyleAttr, 0)
        val waveColor =
            arr.getColor(R.styleable.WaveformView_waveColor, Color.WHITE)
        mLineWidth = arr.getDimension(
            R.styleable.WaveformView_lineWidth,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, metrics)
        )
        pointStep = arr.getDimension(
            R.styleable.WaveformView_pointStep,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.4f, metrics)
        )
        mBufferSize = arr.getInt(R.styleable.WaveformView_bufferSize, 5)
        mMaxValue = arr.getInteger(R.styleable.WaveformView_maxValue, 100)
        mWavePaint = Paint()
        mWavePaint.color = waveColor
        mWavePaint.strokeWidth = mLineWidth
        mWavePaint.style = Paint.Style.STROKE
        mWavePaint.strokeCap = Paint.Cap.ROUND
        mWavePaint.strokeJoin = Paint.Join.ROUND
        val backgroundColor = arr.getColor(
            R.styleable.WaveformView_backgroundColor,
            resources.getColor(R.color.waveform_background)
        )
        mBackgroundPaint = Paint()
        mBackgroundPaint.color = backgroundColor
        mSurfaceHolder = holder
        mSurfaceHolder.addCallback(this)
        mDataBuffer = IntArray(mBufferSize * 2)
        mDataBufferIndex = 0
        setBackgroundColor(backgroundColor)
        setZOrderOnTop(true)
        holder.setFormat(PixelFormat.TRANSLUCENT)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        if (width > mWidth) mWidth = width
        val height = (MeasureSpec.getSize(heightMeasureSpec) * 0.95).toInt()
        if (height > mHeight) mHeight = height
    }

    fun addAmp(amp: Int) {
        if (!isSurfaceViewAvailable) {
            mDataBufferIndex = 0
            return
        }
        if (mLastPoint == null) {
            mLastPoint = Point()
            mLastPoint!!.x = 0
            mLastPoint!!.y = (mHeight - mHeight / mMaxValue.toFloat() * amp).toInt()
            return
        }
        mDataBuffer[mDataBufferIndex] = amp
        mDataBufferIndex++
        if (mDataBufferIndex >= mBufferSize) {
            mDataBufferIndex = 0
            var points = ((mWidth - mLastPoint!!.x) / pointStep).toInt()
            points = if (points > mBufferSize) mBufferSize else points
            val xRight = (mLastPoint!!.x + pointStep * points).toInt()
            mCanvas = mSurfaceHolder.lockCanvas(
                Rect(
                    mLastPoint!!.x,
                    0,
                    (xRight + pointStep * 2).toInt(),
                    (mHeight + mLineWidth).toInt()
                )
            )
            if (mCanvas == null) return
            mCanvas!!.drawRect(
                Rect(
                    mLastPoint!!.x,
                    0,
                    (xRight + pointStep * 2).toInt(),
                    (mHeight + mLineWidth).toInt()
                ), mBackgroundPaint
            )
            for (i in 0 until points) {
                val point = Point()
                point.x = (mLastPoint!!.x + pointStep).toInt()
                point.y =
                    (mHeight - mHeight / mMaxValue.toFloat() * mDataBuffer[i]).toInt()
                mCanvas!!.drawLine(
                    mLastPoint!!.x.toFloat(),
                    mLastPoint!!.y.toFloat(),
                    point.x.toFloat(),
                    point.y.toFloat(),
                    mWavePaint
                )
                mLastPoint = point
            }
            mSurfaceHolder.unlockCanvasAndPost(mCanvas)
            postInvalidate()
            if (((mWidth - mLastPoint!!.x) / pointStep).toInt() < 1) {
                mLastPoint!!.x = 0
            }
            if (points < mBufferSize) {
                mDataBufferIndex = mBufferSize - points
                for (i in 0 until mDataBufferIndex) {
                    mDataBuffer[i] = mDataBuffer[points + i]
                }
                mLastPoint!!.x = 0
            }
        }
    }

    fun reset() {
        mDataBufferIndex = 0
        mLastPoint =
            Point(0, (mHeight - mHeight / mMaxValue.toFloat() * 128).toInt())
        val c = mSurfaceHolder.lockCanvas()
        c.drawRect(Rect(0, 0, mWidth, mHeight), mBackgroundPaint)
        mSurfaceHolder.unlockCanvasAndPost(c)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (mLastPoint != null) {
            mLastPoint = null
        }

        isSurfaceViewAvailable = true
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
        val c = holder.lockCanvas()
        c.drawRect(Rect(0, 0, mWidth, mHeight), mBackgroundPaint)
        holder.unlockCanvasAndPost(c)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceViewAvailable = false
    }
}