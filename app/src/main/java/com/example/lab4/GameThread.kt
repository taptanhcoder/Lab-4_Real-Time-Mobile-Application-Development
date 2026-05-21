package com.example.lab4

import android.graphics.Canvas
import android.view.SurfaceHolder

class GameThread(private val surfaceHolder: SurfaceHolder, private val gameView: GameView) : Thread() {
    var running: Boolean = false
    private val targetFPS = 60

    override fun run() {
        var startTime: Long
        var timeMillis: Long
        var waitTime: Long
        val frameTime = (1000 / targetFPS).toLong()

        while (running) {
            startTime = System.nanoTime()
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                canvas?.let {
                    synchronized(surfaceHolder) {
                        gameView.update()
                        gameView.draw(it)
                    }
                }
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            timeMillis = (System.nanoTime() - startTime) / 1_000_000
            waitTime = frameTime - timeMillis

            try {
                if (waitTime > 0) {
                    sleep(waitTime)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}   




