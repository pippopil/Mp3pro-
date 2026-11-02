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
            y = 18
        }

        val density = resources.displayMetrics.density
        fun dp(v: Float) = (v * density).toInt()

        val rootLayout = FrameLayout(this).apply {
            setPadding(dp(4f), dp(2f), dp(4f), dp(2f))
        }

        val pillLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), dp(6f), dp(12f), dp(6f))

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(24f).toFloat()
                setColor(Color.parseColor("#F0080D1A"))
                setStroke(dp(1.5f), Color.parseColor("#06B6D4"))
            }

            setOnClickListener {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(launchIntent)
            }
        }

        val cameraDot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(10f), dp(10f)).apply {
                marginEnd = dp(8f)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.BLACK)
                setStroke(dp(1f), Color.parseColor("#334155"))
            }
        }
        pillLayout.addView(cameraDot)

        val frogView = TextView(this).apply {
            text = "🐸"
            textSize = 15f
            tag = "island_frog"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(6f)
            }
        }
        pillLayout.addView(frogView)

        frogAnimator = ObjectAnimator.ofFloat(frogView, "translationY", 0f, -dp(4f).toFloat()).apply {
            duration = 260
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }

        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(dp(130f), LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val titleTv = TextView(this).apply {
            text = title
            tag = "island_title"
            setTextColor(Color.WHITE)
            textSize = 11f
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        textContainer.addView(titleTv)

        val hintTv = TextView(this).apply {
            text = if (artist.isNotBlank()) artist else "Нажмите для открытия"
            setTextColor(Color.parseColor("#06B6D4"))
            textSize = 9f
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        textContainer.addView(hintTv)

        pillLayout.addView(textContainer)

        val noteTv = TextView(this).apply {
            text = "♪"
            setTextColor(Color.parseColor("#67E8F9"))
            textSize = 12f
            setPadding(dp(4f), 0, 0, 0)
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
