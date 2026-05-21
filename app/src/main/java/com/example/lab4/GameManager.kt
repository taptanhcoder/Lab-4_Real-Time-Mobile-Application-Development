package com.example.lab4

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.random.Random

class GameManager(private val context: Context) {
    private val opponentBitmaps: List<Bitmap> = listOf(
        BitmapFactory.decodeResource(context.resources, R.drawable.rocket),
        BitmapFactory.decodeResource(context.resources, R.drawable.rocket_2),
        BitmapFactory.decodeResource(context.resources, R.drawable.alian)
    ).map {
        Bitmap.createScaledBitmap(it, OPPONENT_SIZE, OPPONENT_SIZE, true)
    }

    fun createOpponent(x: Float, y: Float, speed: Float): Opponent {
        return createOpponent(x, y, speed, score = 0)
    }

    fun createOpponent(x: Float, y: Float, speed: Float, score: Int): Opponent {
        val bitmap = opponentBitmaps.random()
        val health = Random.nextInt(1, 4 + score / 120)
        val horizontalSpeed = Random.nextFloat() * 3.5f + 1.5f

        return Opponent(
            x = x,
            y = y,
            speed = speed,
            bitmap = bitmap,
            maxHealth = health,
            horizontalSpeed = horizontalSpeed
        )
    }

    fun createOpponentFromBoss(
        bossCenterX: Float,
        spawnY: Float,
        screenWidth: Int,
        speed: Float,
        score: Int
    ): Opponent {
        val bitmap = opponentBitmaps.random()
        val spawnX = (bossCenterX - bitmap.width / 2f).coerceIn(0f, screenWidth - bitmap.width.toFloat())
        val health = Random.nextInt(1, 4 + score / 120)
        val horizontalSpeed = Random.nextFloat() * 3.5f + 1.5f

        return Opponent(
            x = spawnX,
            y = spawnY,
            speed = speed,
            bitmap = bitmap,
            maxHealth = health,
            horizontalSpeed = horizontalSpeed
        )
    }

    companion object {
        private const val OPPONENT_SIZE = 100
    }
}
