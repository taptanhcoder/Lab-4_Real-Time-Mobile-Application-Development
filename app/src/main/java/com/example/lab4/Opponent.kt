package com.example.lab4

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.abs
import kotlin.random.Random

class Opponent(
    var x: Float,
    var y: Float,
    var speed: Float,
    private val bitmap: Bitmap,
    val maxHealth: Int,
    private val horizontalSpeed: Float
) {
    val width: Float = bitmap.width.toFloat()
    val height: Float = bitmap.height.toFloat()
    private val rect: RectF = RectF(x, y, x + width, y + height)
    private val healthBackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(190, 40, 40, 48) }
    private val healthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(89, 232, 126) }
    var health: Int = maxHealth
        private set
    private var targetX: Float = x
    private var laneCooldown: Int = 0

    fun update(screenWidth: Int) {
        y += speed
        updateLane(screenWidth)
        rect.set(x, y, x + width, y + height)
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, null, rect, null)
        drawHealthBar(canvas)
    }

    fun isOffScreen(height: Int): Boolean {
        return y > height
    }

    fun takeDamage(amount: Int) {
        health = (health - amount).coerceAtLeast(0)
    }

    fun isDead(): Boolean {
        return health <= 0
    }

    fun getRect(): RectF {
        return rect
    }

    private fun updateLane(screenWidth: Int) {
        if (screenWidth <= width.toInt()) return

        laneCooldown--
        if (laneCooldown <= 0 || abs(targetX - x) < horizontalSpeed) {
            chooseNewLane(screenWidth)
        }

        val delta = (targetX - x).coerceIn(-horizontalSpeed, horizontalSpeed)
        x = (x + delta).coerceIn(0f, screenWidth - width)
    }

    private fun chooseNewLane(screenWidth: Int) {
        val laneCount = 5
        val laneWidth = screenWidth.toFloat() / laneCount
        val laneIndex = Random.nextInt(laneCount)
        targetX = (laneIndex * laneWidth + laneWidth / 2f - width / 2f).coerceIn(0f, screenWidth - width)
        laneCooldown = Random.nextInt(55, 140)
    }

    private fun drawHealthBar(canvas: Canvas) {
        val barHeight = 8f
        val top = (y - 14f).coerceAtLeast(4f)
        val background = RectF(x, top, x + width, top + barHeight)
        val healthRatio = health.toFloat() / maxHealth
        val foreground = RectF(x, top, x + width * healthRatio, top + barHeight)

        canvas.drawRoundRect(background, 4f, 4f, healthBackPaint)
        canvas.drawRoundRect(foreground, 4f, 4f, healthPaint)
    }
}



