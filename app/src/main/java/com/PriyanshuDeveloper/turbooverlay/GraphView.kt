package com.PriyanshuDeveloper.turbooverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class GraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dataArray = FloatArray(240)
    private var dataCount = 0
    private var maxPoints = 240

    private val linePaint = Paint().apply {
        color = Color.rgb(76, 255, 124)
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint().apply {
        color = Color.argb(45, 76, 255, 124)
        style = Paint.Style.FILL
    }
    private val gridPaint = Paint().apply {
        color = Color.argb(70, 255, 255, 255)
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val axisPaint = Paint().apply {
        color = Color.argb(120, 255, 255, 255)
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }
    private val labelPaint = Paint().apply {
        color = Color.argb(210, 255, 255, 255)
        textSize = 18f
        isAntiAlias = true
    }

    private val path = Path()
    private val fillPath = Path()
    private var maxValue = 100f

    private var labelTop = "100%"
    private var labelBottom = "0%"
    private var labelTime = "2m"

    private val xCoords = FloatArray(240)
    private var lastGraphW = 0f

    fun setData(
        history: FloatArray,
        count: Int,
        head: Int,
        maxVal: Float = 100f,
        unit: String = "%"
    ) {
        this.maxValue = if (maxVal <= 0) 1f else maxVal
        this.dataCount = count.coerceAtMost(dataArray.size)
        this.maxPoints = dataArray.size

        if (count < maxPoints) {
            System.arraycopy(history, 0, dataArray, 0, count)
        } else {
            val tailCount = maxPoints - head
            System.arraycopy(history, head, dataArray, 0, tailCount)
            System.arraycopy(history, 0, dataArray, tailCount, head)
        }

        labelTop = "${maxValue.toInt()}$unit"
        labelBottom = "0$unit"
        labelTime = "2m"

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val leftPad = 35f
        val rightPad = 4f
        val topPad = 16f
        val bottomPad = 22f

        if (w <= leftPad + rightPad || h <= topPad + bottomPad || dataCount < 2) return

        val graphW = w - leftPad - rightPad
        val graphH = h - topPad - bottomPad
        val graphRight = w - rightPad
        val graphBottom = h - bottomPad

        if (graphW != lastGraphW) {
            val stepX = graphW / (maxPoints - 1)
            for (i in 0 until maxPoints) {
                xCoords[i] = leftPad + (i * stepX)
            }
            lastGraphW = graphW
        }

        // Draw Axes and Grid
        canvas.drawRect(leftPad, topPad, graphRight, graphBottom, axisPaint)
        val gridStepY = graphH / 4f
        for (i in 1..3) {
            val y = topPad + gridStepY * i
            canvas.drawLine(leftPad, y, graphRight, y, gridPaint)
        }
        val gridStepX = graphW / 4f
        for (i in 1..3) {
            val x = leftPad + gridStepX * i
            canvas.drawLine(x, topPad, x, graphBottom, gridPaint)
        }

        canvas.drawText(labelTop, 2f, topPad + 6f, labelPaint)
        canvas.drawText(labelBottom, 2f, graphBottom, labelPaint)
        canvas.drawText(labelTime, leftPad, h - 4f, labelPaint)
        canvas.drawText("now", (w - 34f).coerceAtLeast(leftPad), h - 4f, labelPaint)

        path.rewind()
        fillPath.rewind()

        val startIndex = maxPoints - dataCount

        for (i in 0 until dataCount) {
            val x = xCoords[startIndex + i]
            val valFloat = dataArray[i]
            val normalized = (valFloat / maxValue).coerceIn(0f, 1f)
            val y = graphBottom - normalized * graphH

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(graphRight, graphBottom)
        fillPath.lineTo(xCoords[startIndex], graphBottom)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)
    }
}
