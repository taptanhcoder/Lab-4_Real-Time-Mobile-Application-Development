package com.example.lab4

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import kotlin.random.Random

class GameManager(context: Context) {
    private val opponentBitmaps: List<Bitmap> = listOf(
        BitmapFactory.decodeResource(context.resources, R.drawable.rocket),
        BitmapFactory.decodeResource(context.resources, R.drawable.rocket_2),
        BitmapFactory.decodeResource(context.resources, R.drawable.alian)
    ).map {
        it.scale(OPPONENT_SIZE, OPPONENT_SIZE)
    }

    @Suppress("unused")
    fun createOpponent(x: Float, y: Float, speed: Float): Opponent {
        return createOpponent(x, y, speed, score = 0)
    }

    fun createOpponent(x: Float, y: Float, speed: Float, score: Int): Opponent {
        val bitmap = opponentBitmaps.random()
        val health = Random.nextInt(MIN_OPPONENT_HEALTH, MAX_OPPONENT_HEALTH + score / 120)
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
        val maxSpawnX = (screenWidth - OPPONENT_SIZE).coerceAtLeast(0).toFloat()
        val spawnX = (bossCenterX - OPPONENT_SIZE / 2f).coerceIn(0f, maxSpawnX)

        return createOpponent(spawnX, spawnY, speed, score)
    }

    companion object {
        private const val OPPONENT_SIZE = 100
        private const val MIN_OPPONENT_HEALTH = 2
        private const val MAX_OPPONENT_HEALTH = 5
    }
}
