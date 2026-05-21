package com.example.lab4

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class FiringObject(
    var x: Float,
    var y: Float,
    private val velocityX: Float,
    private val velocityY: Float,
    val damage: Int = 1
) {
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 226, 89)
    }
    val width: Float = 18f
    val height: Float = 42f
    val rect: RectF = RectF()

    init {
        updateRect()
    }

    fun update() {
        x += velocityX
        y -= velocityY
        updateRect()
    }

    fun draw(canvas: Canvas) {
        canvas.drawRoundRect(rect, width / 2f, width / 2f, paint)
    }

    fun isOffScreen(screenWidth: Int, screenHeight: Int): Boolean {
        return rect.bottom < 0 || rect.right < 0 || rect.left > screenWidth || rect.top > screenHeight
    }

    private fun updateRect() {
        rect.set(x - width / 2f, y - height, x + width / 2f, y)
    }
}



