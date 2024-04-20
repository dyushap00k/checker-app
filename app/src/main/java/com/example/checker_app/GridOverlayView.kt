package com.example.checker_app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class GridOverlayView : View {
    private val paint = Paint()

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        // Инициализация атрибутов, если есть
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGrid(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        val screenWidth = width
        val screenHeight = height
        val paddingVertical = screenHeight * 0.05f // Отступы по вертикали - 5% от высоты экрана
        val paddingHorizontal = screenWidth * 0.05f // Отступы по горизонтали - 5% от ширины экрана
        val rectWidth = screenWidth - 2 * paddingHorizontal // Ширина прямоугольника
        val rectHeight = screenHeight - 2 * paddingVertical // Высота прямоугольника
        val columnWidth = rectWidth / 6f // 6 колонок
        val rowHeight = rectHeight / 20f // 20 рядов

        paint.color = Color.GREEN
        paint.strokeWidth = 3f // Установим толщину линий в 3 пикселя
        paint.strokeCap = Paint.Cap.SQUARE // Установим квадратные концы для более одинакового вида
        paint.style = Paint.Style.STROKE // Установим только стиль обводки, без заливки

        // Рисуем ободок
        canvas.drawRect(paddingHorizontal, paddingVertical, paddingHorizontal + rectWidth, paddingVertical + rectHeight, paint)

        // Рисуем вертикальные линии
        for (i in 1 until 6) {
            val x = paddingHorizontal + i * columnWidth
            canvas.drawLine(x, paddingVertical, x, paddingVertical + rectHeight, paint)
        }

        // Рисуем горизонтальные линии
        for (i in 1 until 20) {
            val y = paddingVertical + i * rowHeight
            canvas.drawLine(paddingHorizontal, y, paddingHorizontal + rectWidth, y, paint)
        }
    }
}