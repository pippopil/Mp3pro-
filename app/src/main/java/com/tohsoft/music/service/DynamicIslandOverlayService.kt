package com.tohsoft.music.service

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.tohsoft.music.MainActivity

/**
 * Плавающий визуальный островок в районе фронтальной камеры (Dynamic Camera Island).
 * Автоматически отображается поверх всех окон в районе селфи-камеры,
 * когда приложение свернуто и играет музыка.
 * Показывает анимированного танцующего лягушонка (🐸) и название трека.
 * При клике на островок разворачивает приложение на передний план.
 */
class DynamicIslandOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayLayout: View? = null
    private var frogAnimator: ObjectAnimator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_HIDE) {
            removeOverlay()
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "MP3 Player Pro"
        val artist = intent?.getStringExtra(EXTRA_ARTIST) ?: ""
        val isPlaying = intent?.getBooleanExtra(EXTRA_IS_PLAYING, true) ?: true

        if (isPlaying) {
            showOrUpdateOverlay(title, artist)
        } else {
            removeOverlay()
        }

        return START_NOT_STICKY
    }

    private fun showOrUpdateOverlay(title: String, artist: String) {
        if (overlayLayout != null) {
            val titleView = overlayLayout?.findViewWithTag<TextView>("island_title")
            titleView?.text = title
            return
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val density = resources.displayMetrics.density
        fun dp(v: Float) = (v * density).toInt()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = dp(52f) // Смещаем вправо, чтобы центральная селфи-камера не перекрывала лягушонка
            y = dp(8f)
        }

        val rootLayout = FrameLayout(this).apply {
            setPadding(dp(4f), dp(2f), dp(4f), dp(2f))
        }

        val pillLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10f), dp(5f), dp(10f), dp(5f))

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16f).toFloat()
                setColor(Color.parseColor("#F0080D1A"))
                setStroke(dp(1.2f), Color.parseColor("#10B981"))
            }

            // Нажатие разворачивает плеер
            setOnClickListener {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(launchIntent)
            }

            // Возможность перетаскивания островка по экрану
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isDragging = false

            setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        false
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > dp(4f) || Math.abs(dy) > dp(4f)) {
                            isDragging = true
                            params.x = initialX + dx
                            params.y = initialY + dy
                            try {
                                windowManager?.updateViewLayout(overlayLayout, params)
                            } catch (ignored: Exception) {}
                            true
                        } else false
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        if (isDragging) true else {
                            v.performClick()
                            false
                        }
                    }
                    else -> false
                }
            }
        }

        // Крупный анимированный лягушонок
        val frogView = TextView(this).apply {
            text = "🐸"
            textSize = 17f
            tag = "island_frog"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(6f)
            }
        }
        pillLayout.addView(frogView)

        frogAnimator = ObjectAnimator.ofFloat(frogView, "translationY", 0f, -dp(3f).toFloat()).apply {
            duration = 280
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }

        // Блок названия трека и исполнителя
        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(dp(85f), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(6f)
            }
        }

        val titleTv = TextView(this).apply {
            text = title
            tag = "island_title"
            setTextColor(Color.WHITE)
            textSize = 10.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        textContainer.addView(titleTv)

        val hintTv = TextView(this).apply {
            text = if (artist.isNotBlank()) artist else "MP3 Player Pro"
            setTextColor(Color.parseColor("#10B981"))
            textSize = 8.5f
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        textContainer.addView(hintTv)
        pillLayout.addView(textContainer)

        val noteTv = TextView(this).apply {
            text = "♪ ♫"
            setTextColor(Color.parseColor("#10B981"))
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        pillLayout.addView(noteTv)

        rootLayout.addView(pillLayout)
        overlayLayout = rootLayout

        try {
            windowManager?.addView(overlayLayout, params)
        } catch (ignored: Exception) {}
    }

    private fun removeOverlay() {
        frogAnimator?.cancel()
        frogAnimator = null
        overlayLayout?.let {
            try {
                windowManager?.removeView(it)
            } catch (ignored: Exception) {}
            overlayLayout = null
        }
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW = "com.tohsoft.music.SHOW_DYNAMIC_ISLAND"
        const val ACTION_HIDE = "com.tohsoft.music.HIDE_DYNAMIC_ISLAND"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
    }
}
