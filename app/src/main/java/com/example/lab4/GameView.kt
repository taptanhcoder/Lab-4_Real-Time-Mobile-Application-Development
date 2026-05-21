package com.example.lab4

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GameView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs), SurfaceHolder.Callback {
    private enum class GameState {
        MENU,
        PLAYING,
        GAME_OVER,
        VICTORY
    }

    private data class Star(val xRatio: Float, val yRatio: Float, val radius: Float, val alpha: Int)

    private var thread: GameThread? = null
    private val firingObjects = mutableListOf<FiringObject>()
    private val opponents = mutableListOf<Opponent>()
    private val startButtonRect = RectF()
    private val replayButtonRect = RectF()
    private val menuButtonRect = RectF()
    private val gameManager = GameManager(context)
    private val backgroundBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.galaxy_background)
    private var backgroundScaledBitmap: Bitmap? = null
    private val stars = List(90) {
        Star(
            xRatio = Random.nextFloat(),
            yRatio = Random.nextFloat(),
            radius = Random.nextFloat() * 2.2f + 0.8f,
            alpha = Random.nextInt(90, 230)
        )
    }

    private val prefs = context.getSharedPreferences("lab4_game", Context.MODE_PRIVATE)
    private val shipBitmap = createShipBitmap()
    private val bossBitmap = createBossBitmap()
    private val boss = Boss(0f, 136f, bossBitmap, speed = 4.2f, initialHealth = 35)

    private var score: Int = 0
    private var highScore: Int = prefs.getInt(KEY_HIGH_SCORE, 0)
    private var lives: Int = STARTING_LIVES
    private var escapedEnemies: Int = 0
    private var gameState: GameState = GameState.MENU
    private var opponentBaseSpeed = 4.5f
    private var firingObjectBaseSpeed = 18f
    private var bossSpawnTimer = 0
    private var playerX = 0f

    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(210, 225, 255)
        textSize = 38f
        textAlign = Paint.Align.CENTER
    }
    private val gameOverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 86, 110)
        textSize = 88f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val victoryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(105, 244, 174)
        textSize = 88f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 78f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val menuTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(218, 230, 255)
        textSize = 34f
        textAlign = Paint.Align.CENTER
    }
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(78, 204, 255)
    }
    private val secondaryButtonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(92, 108, 145)
    }
    private val buttonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(8, 13, 34)
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(170, 4, 8, 22)
    }
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val bulletLevel: Int
        get() = (score / 60 + 1).coerceIn(1, MAX_BULLET_LEVEL)

    private val bulletCount: Int
        get() = bulletLevel * 2 - 1

    private val playerY: Float
        get() = height - 118f

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (playerX == 0f) {
            playerX = clampPlayerX(width / 2f)
        }
        thread = GameThread(holder, this).also {
            it.running = true
            it.start()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        backgroundScaledBitmap = backgroundBitmap.scale(width, height)
        playerX = clampPlayerX(if (playerX == 0f) width / 2f else playerX)
        boss.reset(width)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        val gameThread = thread ?: return
        gameThread.running = false
        var retry = true
        while (retry) {
            try {
                gameThread.join()
                retry = false
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        thread = null
    }

    @Synchronized
    fun update() {
        if (gameState != GameState.PLAYING) return

        updateDifficulty()
        updateBoss()
        updateBullets()
        updateOpponents()
        handleCollisions()
        handleOpponentBoundary()
    }

    @Synchronized
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        drawBackground(canvas)

        if (gameState == GameState.MENU) {
            drawMenu(canvas)
            return
        }

        boss.draw(canvas)
        opponents.forEach { it.draw(canvas) }
        firingObjects.forEach { it.draw(canvas) }
        drawShip(canvas)
        drawHud(canvas)

        if (gameState == GameState.GAME_OVER || gameState == GameState.VICTORY) {
            drawEndGame(canvas)
        }
    }

    @Synchronized
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN && event.action != MotionEvent.ACTION_MOVE) {
            return super.onTouchEvent(event)
        }

        if (event.action == MotionEvent.ACTION_DOWN) {
            performClick()
        }

        if (gameState == GameState.MENU) {
            if (event.action == MotionEvent.ACTION_DOWN && isStartButtonTouched(event.x, event.y)) {
                startGame()
            }
            return true
        }

        if (isEndGameState()) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                when {
                    isReplayButtonTouched(event.x, event.y) -> startGame()
                    isMenuButtonTouched(event.x, event.y) -> showMenu()
                }
                return true
            }
            return true
        }

        playerX = clampPlayerX(event.x)
        if (event.action == MotionEvent.ACTION_DOWN) {
            fireBullets()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun updateDifficulty() {
        opponentBaseSpeed = (4.5f + score * 0.015f).coerceAtMost(14f)
        firingObjectBaseSpeed = (18f + score * 0.01f).coerceAtMost(30f)
    }

    private fun updateBoss() {
        boss.update(width)
        bossSpawnTimer++
        val spawnInterval = (95 - score / 30).coerceAtLeast(36)
        if (bossSpawnTimer >= spawnInterval) {
            spawnOpponentFromBoss()
            bossSpawnTimer = 0
        }
    }

    private fun updateBullets() {
        firingObjects.forEach { it.update() }
        firingObjects.removeAll { it.isOffScreen(width, height) }
    }

    private fun updateOpponents() {
        opponents.forEach { it.update(width) }
        opponents.removeAll { it.isOffScreen(height) }
    }

    private fun handleCollisions() {
        val bulletsCopy = ArrayList(firingObjects)
        val opponentsCopy = ArrayList(opponents)

        for (bullet in bulletsCopy) {
            if (!firingObjects.contains(bullet)) continue

            if (RectF.intersects(bullet.rect, boss.getRect())) {
                firingObjects.remove(bullet)
                boss.takeDamage(bullet.damage)
                if (boss.isDead()) {
                    score += 100
                    saveHighScore()
                    finishGame(GameState.VICTORY)
                }
                continue
            }

            for (opponent in opponentsCopy) {
                if (!opponents.contains(opponent)) continue
                if (RectF.intersects(bullet.rect, opponent.getRect())) {
                    firingObjects.remove(bullet)
                    opponent.takeDamage(bullet.damage)
                    if (opponent.isDead()) {
                        opponents.remove(opponent)
                        score += 10
                        saveHighScore()
                    }
                    break
                }
            }
        }
    }

    private fun handleOpponentBoundary() {
        val bottomBoundary = height - 96f
        val escapedOpponents = opponents.filter { it.y + it.height >= bottomBoundary }
        if (escapedOpponents.isEmpty()) return

        opponents.removeAll(escapedOpponents.toSet())
        escapedEnemies += escapedOpponents.size
        lives = (STARTING_LIVES - escapedEnemies).coerceAtLeast(0)
        if (escapedEnemies >= MAX_ESCAPED_ENEMIES) {
            finishGame(GameState.GAME_OVER)
        }
    }

    private fun spawnOpponentFromBoss() {
        if (width <= 0) return

        val opponent = gameManager.createOpponentFromBoss(
            bossCenterX = boss.centerX,
            spawnY = boss.spawnY,
            screenWidth = width,
            speed = opponentBaseSpeed + Random.nextFloat() * 1.8f,
            score = score
        )
        opponents.add(opponent)
    }

    private fun fireBullets() {
        val count = bulletCount
        val center = (count - 1) / 2f
        val startY = playerY - shipBitmap.height / 2f + 18f

        for (index in 0 until count) {
            val spreadOffset = index - center
            val velocityX = spreadOffset * BULLET_SPREAD_SPEED
            firingObjects.add(
                FiringObject(
                    x = playerX,
                    y = startY,
                    velocityX = velocityX,
                    velocityY = firingObjectBaseSpeed,
                    damage = 1
                )
            )
        }
    }

    private fun drawBackground(canvas: Canvas) {
        val scaledBitmap = backgroundScaledBitmap
        if (scaledBitmap == null) {
            canvas.drawColor(Color.rgb(7, 10, 28))
        } else {
            canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
        }
        stars.forEach {
            starPaint.color = Color.argb(it.alpha, 226, 238, 255)
            canvas.drawCircle(it.xRatio * width, it.yRatio * height, it.radius, starPaint)
        }
    }

    private fun drawShip(canvas: Canvas) {
        val left = playerX - shipBitmap.width / 2f
        val top = playerY - shipBitmap.height / 2f
        canvas.drawBitmap(shipBitmap, left, top, null)
    }

    private fun drawHud(canvas: Canvas) {
        canvas.drawText("Score: $score", 34f, 56f, hudPaint)
        canvas.drawText("High Score: $highScore", 34f, 104f, hudPaint)
        canvas.drawText("Lives: $lives", width - 190f, 56f, hudPaint)
        canvas.drawText("Level: $bulletLevel", width - 190f, 104f, hudPaint)
        canvas.drawText("Missed: $escapedEnemies/$MAX_ESCAPED_ENEMIES", 34f, 152f, hudPaint)
    }

    private fun drawMenu(canvas: Canvas) {
        updateMenuButtonBounds()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        canvas.drawText("Lab 4 Shooter", width / 2f, height * 0.30f, titlePaint)
        canvas.drawBitmap(
            shipBitmap,
            width / 2f - shipBitmap.width / 2f,
            height * 0.36f,
            null
        )
        canvas.drawText("High Score: $highScore", width / 2f, height * 0.52f, menuTextPaint)
        drawButton(canvas, startButtonRect, "START", buttonPaint)
    }

    private fun drawEndGame(canvas: Canvas) {
        updateGameOverButtonBounds()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        if (gameState == GameState.VICTORY) {
            canvas.drawText("Victory", width / 2f, height / 2f - 110f, victoryPaint)
        } else {
            canvas.drawText("Game Over", width / 2f, height / 2f - 110f, gameOverPaint)
        }
        canvas.drawText("Score: $score", width / 2f, height / 2f - 44f, hintPaint)
        canvas.drawText("High Score: $highScore", width / 2f, height / 2f + 8f, hintPaint)
        drawButton(canvas, replayButtonRect, "PLAY AGAIN", buttonPaint)
        drawButton(canvas, menuButtonRect, "MENU", secondaryButtonPaint)
    }

    private fun drawButton(canvas: Canvas, rect: RectF, label: String, paint: Paint) {
        canvas.drawRoundRect(rect, 18f, 18f, paint)
        val centerY = rect.centerY() - (buttonTextPaint.descent() + buttonTextPaint.ascent()) / 2f
        canvas.drawText(label, rect.centerX(), centerY, buttonTextPaint)
    }

    private fun isStartButtonTouched(x: Float, y: Float): Boolean {
        updateMenuButtonBounds()
        return startButtonRect.contains(x, y)
    }

    private fun isReplayButtonTouched(x: Float, y: Float): Boolean {
        updateGameOverButtonBounds()
        return replayButtonRect.contains(x, y)
    }

    private fun isMenuButtonTouched(x: Float, y: Float): Boolean {
        updateGameOverButtonBounds()
        return menuButtonRect.contains(x, y)
    }

    private fun updateMenuButtonBounds() {
        setCenteredButton(startButtonRect, height * 0.64f, BUTTON_WIDTH, BUTTON_HEIGHT)
    }

    private fun updateGameOverButtonBounds() {
        setCenteredButton(replayButtonRect, height / 2f + 92f, BUTTON_WIDE_WIDTH, BUTTON_HEIGHT)
        setCenteredButton(menuButtonRect, height / 2f + 184f, BUTTON_WIDTH, BUTTON_HEIGHT)
    }

    private fun setCenteredButton(rect: RectF, centerY: Float, buttonWidth: Float, buttonHeight: Float) {
        val centerX = width / 2f
        rect.set(
            centerX - buttonWidth / 2f,
            centerY - buttonHeight / 2f,
            centerX + buttonWidth / 2f,
            centerY + buttonHeight / 2f
        )
    }

    private fun startGame() {
        resetGame()
        gameState = GameState.PLAYING
    }

    private fun showMenu() {
        resetGame()
        gameState = GameState.MENU
    }

    private fun isEndGameState(): Boolean {
        return gameState == GameState.GAME_OVER || gameState == GameState.VICTORY
    }

    private fun finishGame(resultState: GameState) {
        saveHighScore()
        firingObjects.clear()
        opponents.clear()
        gameState = resultState
    }

    private fun resetGame() {
        score = 0
        lives = STARTING_LIVES
        escapedEnemies = 0
        opponentBaseSpeed = 4.5f
        firingObjectBaseSpeed = 18f
        bossSpawnTimer = 0
        playerX = clampPlayerX(width / 2f)
        firingObjects.clear()
        opponents.clear()
        boss.reset(width)
    }

    private fun saveHighScore() {
        if (score > highScore) {
            highScore = score
            prefs.edit {
                putInt(KEY_HIGH_SCORE, highScore)
            }
        }
    }

    private fun clampPlayerX(rawX: Float): Float {
        val halfShipWidth = shipBitmap.width / 2f
        if (width <= shipBitmap.width) {
            return width / 2f
        }

        return rawX.coerceIn(halfShipWidth, width - halfShipWidth)
    }

    private fun createShipBitmap(): Bitmap {
        val width = SHIP_WIDTH
        val height = SHIP_HEIGHT
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val body = Path()

        paint.color = Color.rgb(83, 203, 255)
        body.moveTo(width / 2f, 8f)
        body.lineTo(width - 18f, height - 18f)
        body.lineTo(width / 2f, height - 40f)
        body.lineTo(18f, height - 18f)
        body.close()
        canvas.drawPath(body, paint)

        paint.color = Color.rgb(236, 249, 255)
        canvas.drawOval(width / 2f - 18f, 34f, width / 2f + 18f, 70f, paint)

        paint.color = Color.rgb(255, 190, 79)
        canvas.drawCircle(width / 2f, height - 20f, 12f, paint)
        return bitmap
    }

    private fun createBossBitmap(): Bitmap {
        val width = BOSS_WIDTH
        val height = BOSS_HEIGHT
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.rgb(161, 92, 255)
        canvas.drawOval(12f, 28f, width - 12f, height - 8f, paint)

        paint.color = Color.rgb(255, 105, 147)
        for (i in 0..5) {
            val angle = Math.PI * i / 5.0
            val cx = width / 2f + cos(angle).toFloat() * 82f
            val cy = 34f + sin(angle).toFloat() * 24f
            canvas.drawCircle(cx, cy, 16f, paint)
        }

        paint.color = Color.WHITE
        canvas.drawCircle(width / 2f - 38f, height / 2f, 18f, paint)
        canvas.drawCircle(width / 2f + 38f, height / 2f, 18f, paint)

        paint.color = Color.rgb(22, 18, 48)
        canvas.drawCircle(width / 2f - 38f, height / 2f, 8f, paint)
        canvas.drawCircle(width / 2f + 38f, height / 2f, 8f, paint)
        return bitmap
    }

    companion object {
        private const val STARTING_LIVES = 3
        private const val MAX_ESCAPED_ENEMIES = STARTING_LIVES
        private const val MAX_BULLET_LEVEL = 5
        private const val BULLET_SPREAD_SPEED = 4.4f
        private const val BUTTON_WIDTH = 260f
        private const val BUTTON_WIDE_WIDTH = 340f
        private const val BUTTON_HEIGHT = 74f
        private const val SHIP_WIDTH = 116
        private const val SHIP_HEIGHT = 116
        private const val BOSS_WIDTH = 220
        private const val BOSS_HEIGHT = 150
        private const val KEY_HIGH_SCORE = "high_score"
    }
}
