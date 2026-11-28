package com.tohsoft.music.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private const val YANDEX_OAUTH_URL =
    "https://oauth.yandex.ru/authorize?response_type=token&client_id=23cabbbdc6cd418abb4b39c32c41195d"

private val ACCESS_TOKEN_REGEX = Regex("(?:[#&?])access_token=([^&#\\s]+)")

private fun extractYandexAccessToken(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val match = ACCESS_TOKEN_REGEX.find(url) ?: return null
    val raw = match.groupValues.getOrNull(1)?.trim().orEmpty()
    if (raw.isEmpty()) return null
    val decoded = try {
        Uri.decode(raw).trim()
    } catch (_: Exception) {
        raw
    }
    return decoded.takeIf { it.length >= 10 }
}

private fun isAllowedYandexOAuthUrl(url: String): Boolean {
    val uri = try {
        Uri.parse(url)
    } catch (_: Exception) {
        return false
    }
    val scheme = uri.scheme?.lowercase() ?: return false
    if (scheme != "https") return false
    val host = uri.host?.lowercase() ?: return false
    return host == "yandex.ru" || host.endsWith(".yandex.ru") ||
        host == "yandex.com" || host.endsWith(".yandex.com") ||
        host == "ya.ru" || host.endsWith(".ya.ru") ||
        host == "yandex.net" || host.endsWith(".yandex.net") ||
        host == "yandexcloud.net" || host.endsWith(".yandexcloud.net")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YandexOAuthWebViewDialog(
    onTokenCaptured: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(10) }
    var captured by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewRef?.stopLoading()
                webViewRef?.clearHistory()
                webViewRef?.destroy()
            } catch (_: Exception) {
            }
            webViewRef = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0E1324)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF14192B))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🟡 Вход через Яндекс ID",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Войдите и нажмите «Разрешить» — токен перехватится сам",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { webViewRef?.loadUrl(YANDEX_OAUTH_URL) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("↻", color = Color(0xFFFFCC00), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = onDismiss,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Закрыть", color = Color(0xFF00C996), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { (loadProgress / 100f).coerceIn(0.05f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = Color(0xFFFFCC00),
                        trackColor = Color(0x22FFCC00)
                    )
                }

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            val cm = CookieManager.getInstance()
                            cm.setAcceptCookie(true)
                            cm.setAcceptThirdPartyCookies(this, true)

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.javaScriptCanOpenWindowsAutomatically = false
                            settings.setSupportMultipleWindows(false)
                            // Убираем маркер "; wv", чтобы Яндекс ID корректно открывал форму входа
                            settings.userAgentString = settings.userAgentString.replace("; wv", "")

                            fun interceptUrl(view: WebView?, url: String?): Boolean {
                                val token = extractYandexAccessToken(url)
                                if (token != null) {
                                    if (!captured) {
                                        captured = true
                                        view?.stopLoading()
                                        onTokenCaptured(token)
                                    }
                                    return true
                                }
                                // Блокируем перехват внешними приложениями (intent://, yandexmusic://)
                                if (!url.isNullOrBlank() && !isAllowedYandexOAuthUrl(url)) {
                                    return true
                                }
                                return false
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadProgress = newProgress
                                    isLoading = newProgress < 100
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString()
                                    return interceptUrl(view, url)
                                }

                                @Deprecated("Deprecated in Java")
                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                    return interceptUrl(view, url)
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    interceptUrl(view, url)
                                }

                                override fun doUpdateVisitedHistory(
                                    view: WebView?,
                                    url: String?,
                                    isReload: Boolean
                                ) {
                                    super.doUpdateVisitedHistory(view, url, isReload)
                                    interceptUrl(view, url)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    interceptUrl(view, url)
                                }
                            }

                            webViewRef = this
                            loadUrl(YANDEX_OAUTH_URL)
                        }
                    }
                )
            }
        }
    }
}
