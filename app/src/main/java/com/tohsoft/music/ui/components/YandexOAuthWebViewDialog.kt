package com.tohsoft.music.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
import com.tohsoft.music.data.streaming.StreamingProvider
import java.util.concurrent.atomic.AtomicBoolean

private const val YANDEX_OAUTH_URL =
    "https://oauth.yandex.ru/authorize?response_type=token&client_id=23cabbbdc6cd418abb4b39c32c41195d"

private const val VK_OAUTH_URL =
    "https://oauth.vk.com/authorize?client_id=2685278&scope=audio,offline&redirect_uri=https://oauth.vk.com/blank.html&display=mobile&response_type=token&v=5.131"

private const val SPOTIFY_LOGIN_URL =
    "https://accounts.spotify.com/login?continue=https%3A%2F%2Fopen.spotify.com%2F"

private const val SOUNDCLOUD_LOGIN_URL =
    "https://m.soundcloud.com/signin"

private const val AUDIUS_LOGIN_URL =
    "https://audius.co/signin"

private val ACCESS_TOKEN_REGEX = Regex("(?:[#&?])access_token=([^&#\\s]+)")
private val SPOTIFY_LINK_REGEX = Regex("https?://open\\.spotify\\.com/(playlist|album|artist|track)/[A-Za-z0-9]+", RegexOption.IGNORE_CASE)
private val AUDIUS_EXCLUDED_PATHS = setOf(
    "", "signin", "signup", "trending", "feed", "explore", "search",
    "settings", "upload", "playlists", "favorites", "library", "dashboard", "audio", "messages"
)

private val API_CAPTURE_JS = """
(function() {
  if (window.__mp3proHooked) return;
  window.__mp3proHooked = true;
  function send(tok) {
    try {
      if (tok && typeof tok === 'string' && tok.length >= 12 && window.Mp3ProAuthBridge) {
        window.Mp3ProAuthBridge.onApiToken(tok);
      }
    } catch (e) {}
  }
  function checkHeader(name, val) {
    if (!name || !val) return;
    var n = String(name).toLowerCase();
    if (n === 'authorization') {
      var v = String(val).trim();
      if (v.toLowerCase().indexOf('bearer ') === 0) send(v.substring(7).trim());
      else if (v.toLowerCase().indexOf('oauth ') === 0) send(v.substring(6).trim());
    }
  }
  if (window.fetch) {
    var origFetch = window.fetch;
    window.fetch = function(input, init) {
      try {
        if (init && init.headers) {
          if (typeof init.headers.forEach === 'function') {
            init.headers.forEach(function(v, k) { checkHeader(k, v); });
          } else {
            Object.keys(init.headers).forEach(function(k) { checkHeader(k, init.headers[k]); });
          }
        }
      } catch (e) {}
      var p = origFetch.apply(this, arguments);
      try {
        var u = (typeof input === 'string') ? input : (input && input.url ? input.url : '');
        if (u.indexOf('get_access_token') !== -1 || u.indexOf('/api/token') !== -1) {
          p.then(function(res) {
            try {
              res.clone().json().then(function(j) {
                if (j && j.accessToken && !j.isAnonymous) send(j.accessToken);
                else if (j && j.access_token) send(j.access_token);
              }).catch(function(){});
            } catch (e) {}
          }).catch(function(){});
        }
      } catch (e) {}
      return p;
    };
  }
  if (window.XMLHttpRequest) {
    var origSetHeader = XMLHttpRequest.prototype.setRequestHeader;
    XMLHttpRequest.prototype.setRequestHeader = function(k, v) {
      try { checkHeader(k, v); } catch (e) {}
      return origSetHeader.apply(this, arguments);
    };
  }
})();
""".trimIndent()

private fun extractAccessTokenFromUrl(url: String?): String? {
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

private fun extractCookieValue(cookieHeader: String?, key: String): String? {
    if (cookieHeader.isNullOrBlank()) return null
    return cookieHeader.split(";")
        .map { it.trim() }
        .firstOrNull { it.startsWith("$key=") }
        ?.substringAfter("$key=")
        ?.trim()
        ?.takeIf { it.length >= 10 && it != "deleted" }
}

private fun extractAuthHeaderToken(headers: Map<String, String>?): String? {
    if (headers.isNullOrEmpty()) return null
    val auth = headers.entries.firstOrNull { it.key.equals("Authorization", ignoreCase = true) }?.value?.trim()
        ?: return null
    return when {
        auth.startsWith("Bearer ", ignoreCase = true) -> auth.substring(7).trim()
        auth.startsWith("OAuth ", ignoreCase = true) -> auth.substring(6).trim()
        else -> null
    }?.takeIf { it.length >= 15 }
}

private fun extractAudiusHandleFromUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val uri = try { Uri.parse(url) } catch (_: Exception) { return null }
    val host = uri.host?.lowercase() ?: return null
    if (host != "audius.co" && !host.endsWith(".audius.co")) return null
    val seg = uri.pathSegments.firstOrNull()?.trim()?.lowercase() ?: return null
    if (seg in AUDIUS_EXCLUDED_PATHS || seg.length < 2) return null
    return "@$seg"
}

private fun isAllowedProviderUrl(provider: StreamingProvider, url: String): Boolean {
    val uri = try {
        Uri.parse(url)
    } catch (_: Exception) {
        return false
    }
    val scheme = uri.scheme?.lowercase() ?: return false
    if (scheme != "https") return false
    val host = uri.host?.lowercase() ?: return false

    fun matchesAny(domains: List<String>): Boolean =
        domains.any { d -> host == d || host.endsWith(".$d") }

    val commonAuth = listOf("google.com", "gstatic.com", "googleusercontent.com", "apple.com", "facebook.com", "fbcdn.net", "recaptcha.net")
    return when (provider) {
        StreamingProvider.YANDEX -> matchesAny(listOf("yandex.ru", "yandex.com", "ya.ru", "yandex.net", "yandexcloud.net"))
        StreamingProvider.VK -> matchesAny(listOf("vk.com", "vk.ru", "vk.me", "userapi.com", "vk-cdn.net", "mycdn.me", "mail.ru"))
        StreamingProvider.SPOTIFY -> matchesAny(listOf("spotify.com", "scdn.co", "spotifycdn.com") + commonAuth)
        StreamingProvider.SOUNDCLOUD -> matchesAny(listOf("soundcloud.com", "sndcdn.com") + commonAuth)
        StreamingProvider.AUDIUS -> matchesAny(listOf("audius.co", "audius.org") + commonAuth)
    }
}

@Composable
fun YandexOAuthWebViewDialog(
    onTokenCaptured: (String) -> Unit,
    onDismiss: () -> Unit
) {
    StreamingOAuthWebViewDialog(
        provider = StreamingProvider.YANDEX,
        onCredentialCaptured = onTokenCaptured,
        onDismiss = onDismiss
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StreamingOAuthWebViewDialog(
    provider: StreamingProvider,
    onCredentialCaptured: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val startUrl = remember(provider) {
        when (provider) {
            StreamingProvider.YANDEX -> YANDEX_OAUTH_URL
            StreamingProvider.VK -> VK_OAUTH_URL
            StreamingProvider.SPOTIFY -> SPOTIFY_LOGIN_URL
            StreamingProvider.SOUNDCLOUD -> SOUNDCLOUD_LOGIN_URL
            StreamingProvider.AUDIUS -> AUDIUS_LOGIN_URL
        }
    }
    val accentColor = remember(provider) {
        when (provider) {
            StreamingProvider.YANDEX -> Color(0xFFFFCC00)
            StreamingProvider.VK -> Color(0xFF0077FF)
            StreamingProvider.SPOTIFY -> Color(0xFF1DB954)
            StreamingProvider.SOUNDCLOUD -> Color(0xFFFF5500)
            StreamingProvider.AUDIUS -> Color(0xFFCC0FE0)
        }
    }
    val headerTitle = remember(provider) {
        when (provider) {
            StreamingProvider.YANDEX -> "🟡 Вход через Яндекс ID"
            StreamingProvider.VK -> "🔵 Вход через VK ID"
            StreamingProvider.SPOTIFY -> "🟢 Вход в Spotify"
            StreamingProvider.SOUNDCLOUD -> "🟠 Вход в SoundCloud"
            StreamingProvider.AUDIUS -> "🟣 Профиль Audius"
        }
    }
    val headerSubtitle = remember(provider) {
        when (provider) {
            StreamingProvider.YANDEX -> "Войдите и нажмите «Разрешить» — токен перехватится сам"
            StreamingProvider.VK -> "Войдите в VK — токен VK Audio перехватится автоматически"
            StreamingProvider.SPOTIFY -> "Войдите в Spotify или откройте плейлист — API перехватится сам"
            StreamingProvider.SOUNDCLOUD -> "Войдите в SoundCloud — OAuth-токен перехватится из сессии"
            StreamingProvider.AUDIUS -> "Войдите или откройте страницу артиста — @handle перехватится сам"
        }
    }

    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(10) }
    var currentPageUrl by remember { mutableStateOf(startUrl) }
    val captured = remember { AtomicBoolean(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun deliverCaptured(view: WebView?, credential: String) {
        val clean = credential.trim()
        if (clean.isEmpty()) return
        if (captured.compareAndSet(false, true)) {
            mainHandler.post {
                try {
                    view?.stopLoading()
                } catch (_: Exception) {
                }
                onCredentialCaptured(clean)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewRef?.removeJavascriptInterface("Mp3ProAuthBridge")
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
                            text = headerTitle,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = headerSubtitle,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val canGrabSpotifyLink = provider == StreamingProvider.SPOTIFY &&
                            SPOTIFY_LINK_REGEX.containsMatchIn(currentPageUrl)
                        val audiusHandle = if (provider == StreamingProvider.AUDIUS) {
                            extractAudiusHandleFromUrl(currentPageUrl)
                        } else null

                        if (canGrabSpotifyLink) {
                            TextButton(
                                onClick = { deliverCaptured(webViewRef, currentPageUrl) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Забрать", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (audiusHandle != null) {
                            TextButton(
                                onClick = { deliverCaptured(webViewRef, audiusHandle) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Выбрать", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(
                            onClick = { webViewRef?.loadUrl(startUrl) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text("↻", color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = onDismiss,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
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
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.2f)
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
                            // Убираем маркер "; wv", чтобы OAuth-страницы не блокировали встроенный браузер
                            settings.userAgentString = settings.userAgentString.replace("; wv", "")

                            fun hasSpotifySessionCookie(): Boolean {
                                val c1 = cm.getCookie("https://open.spotify.com")
                                val c2 = cm.getCookie("https://spotify.com")
                                return extractCookieValue(c1, "sp_dc") != null || extractCookieValue(c2, "sp_dc") != null
                            }

                            fun checkProviderCookies(view: WebView?) {
                                if (captured.get()) return
                                when (provider) {
                                    StreamingProvider.SOUNDCLOUD -> {
                                        val token = extractCookieValue(cm.getCookie("https://soundcloud.com"), "oauth_token")
                                            ?: extractCookieValue(cm.getCookie("https://m.soundcloud.com"), "oauth_token")
                                        if (token != null) {
                                            deliverCaptured(view, token)
                                        }
                                    }
                                    else -> Unit
                                }
                            }

                            fun acceptApiToken(view: WebView?, rawToken: String?) {
                                val tok = rawToken?.trim().orEmpty()
                                if (tok.length < 15 || captured.get()) return
                                when (provider) {
                                    StreamingProvider.SPOTIFY -> {
                                        // Принимаем Bearer-токен Spotify только после реального входа (наличие sp_dc cookie)
                                        if (hasSpotifySessionCookie()) {
                                            deliverCaptured(view, tok)
                                        }
                                    }
                                    StreamingProvider.SOUNDCLOUD,
                                    StreamingProvider.VK,
                                    StreamingProvider.YANDEX -> {
                                        deliverCaptured(view, tok)
                                    }
                                    StreamingProvider.AUDIUS -> Unit
                                }
                            }

                            addJavascriptInterface(
                                object {
                                    @JavascriptInterface
                                    fun onApiToken(token: String?) {
                                        acceptApiToken(this@apply, token)
                                    }
                                },
                                "Mp3ProAuthBridge"
                            )

                            fun interceptUrl(view: WebView?, url: String?): Boolean {
                                if (!url.isNullOrBlank()) {
                                    mainHandler.post { currentPageUrl = url }
                                }
                                // 1) Перехват access_token из URL (Яндекс ID, VK ID и OAuth Implicit Flow)
                                val urlToken = extractAccessTokenFromUrl(url)
                                if (urlToken != null && (provider == StreamingProvider.YANDEX || provider == StreamingProvider.VK || provider == StreamingProvider.SPOTIFY || provider == StreamingProvider.SOUNDCLOUD)) {
                                    deliverCaptured(view, urlToken)
                                    return true
                                }
                                // 2) Проверка сессионных cookies (SoundCloud oauth_token)
                                checkProviderCookies(view)
                                if (captured.get()) return true

                                // 3) Для Audius перехватываем профиль после входа
                                if (provider == StreamingProvider.AUDIUS) {
                                    val handle = extractAudiusHandleFromUrl(url)
                                    if (handle != null && url != startUrl) {
                                        deliverCaptured(view, handle)
                                        return true
                                    }
                                }

                                // 4) Блокируем уход во внешние приложения (intent://, vk://, spotify://, yandexmusic://)
                                if (!url.isNullOrBlank() && !isAllowedProviderUrl(provider, url)) {
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
                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    if (!captured.get() && request != null) {
                                        val reqUrl = request.url?.toString().orEmpty()
                                        val headerTok = extractAuthHeaderToken(request.requestHeaders)
                                        if (headerTok != null) {
                                            val isMatchingApi = when (provider) {
                                                StreamingProvider.SPOTIFY -> reqUrl.contains("spotify.com", ignoreCase = true)
                                                StreamingProvider.SOUNDCLOUD -> reqUrl.contains("soundcloud.com", ignoreCase = true)
                                                StreamingProvider.VK -> reqUrl.contains("vk.com", ignoreCase = true)
                                                StreamingProvider.YANDEX -> reqUrl.contains("yandex", ignoreCase = true)
                                                StreamingProvider.AUDIUS -> false
                                            }
                                            if (isMatchingApi) {
                                                acceptApiToken(view, headerTok)
                                            }
                                        }
                                        if (provider == StreamingProvider.SOUNDCLOUD) {
                                            checkProviderCookies(view)
                                        }
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }

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
                                    if (provider == StreamingProvider.SPOTIFY || provider == StreamingProvider.SOUNDCLOUD) {
                                        view?.evaluateJavascript(API_CAPTURE_JS, null)
                                    }
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
                                    if (provider == StreamingProvider.SPOTIFY || provider == StreamingProvider.SOUNDCLOUD) {
                                        view?.evaluateJavascript(API_CAPTURE_JS, null)
                                    }
                                    interceptUrl(view, url)
                                    checkProviderCookies(view)
                                }
                            }

                            webViewRef = this
                            loadUrl(startUrl)
                        }
                    }
                )
            }
        }
    }
}
