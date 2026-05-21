package com.example.lab4

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class Boss(
    var x: Float,
    private val fixedY: Float,
    private val bitmap: Bitmap,
    private val speed: Float,
    initialHealth: Int
) {
    val width: Float = bitmap.width.toFloat()
    val height: Float = bitmap.height.toFloat()
    private val rect = RectF(x, fixedY, x + width, fixedY + height)
    private val healthBackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(190, 42, 42, 52) }
    private val healthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 104, 120) }
    private var direction = 1f
    var maxHealth: Int = initialHealth
        private set
    var health: Int = initialHealth
        private set

    val centerX: Float
        get() = x + width / 2f

    val spawnY: Float
        get() = fixedY + height - 8f

    fun update(screenWidth: Int) {
        if (screenWidth <= width.toInt()) return

        x += direction * speed
        if (x <= 0f) {
            x = 0f
            direction = 1f
        } else if (x + width >= screenWidth) {
            x = screenWidth - width
            direction = -1f
        }
        updateRect()
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, null, rect, null)
        drawHealthBar(canvas)
    }

    fun takeDamage(amount: Int) {
        health = (health - amount).coerceAtLeast(0)
    }

    fun isDead(): Boolean {
        return health <= 0
    }

    fun reset(screenWidth: Int, extraHealth: Int = 0) {
        maxHealth = 35 + extraHealth
        health = maxHealth
        x = ((screenWidth - width) / 2f).coerceAtLeast(0f)
        direction = 1f
        updateRect()
    }

    fun getRect(): RectF {
        return rect
    }

    private fun updateRect() {
        rect.set(x, fixedY, x + width, fixedY + height)
    }

    private fun drawHealthBar(canvas: Canvas) {
        val barHeight = 10f
        val top = fixedY - 18f
        val back = RectF(x, top, x + width, top + barHeight)
        val ratio = health.toFloat() / maxHealth
        val front = RectF(x, top, x + width * ratio, top + barHeight)

        canvas.drawRoundRect(back, 5f, 5f, healthBackPaint)
        canvas.drawRoundRect(front, 5f, 5f, healthPaint)
    }
}
