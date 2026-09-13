package com.example.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Message
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.network.NetworkMonitor
import com.example.ui.components.TikIAErrorScreen
import com.example.ui.components.TikIALoadingScreen
import com.example.ui.components.TikIATopProgressBar
import com.example.ui.theme.TikIABlackBackground
import com.example.ui.theme.TikIABlackSurface
import com.example.ui.theme.TikIABorder
import com.example.ui.theme.TikIARedPrimary
import com.example.ui.theme.TikIATextPrimary

const val TIKIA_URL = "https://tik-ia.ai.studio/"

/**
 * Strips the WebView markers (`; wv` and `Version/X.X`) so Google OAuth does not
 * trigger the "disallowed_useragent" 403 error or black screen.
 */
fun sanitizeUserAgent(ua: String): String {
    return ua
        .replace("; wv", "")
        .replace(";wv", "")
        .replace(Regex("Version/\\d+(\\.\\d+)*\\s*"), "")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TikIAScreen(
    modifier: Modifier = Modifier,
    onExit: () -> Unit = {}
) {
    val context = LocalContext.current
    val networkMonitor = remember { NetworkMonitor(context) }
    val isNetworkOnline by networkMonitor.isOnline.collectAsState(initial = networkMonitor.isCurrentlyOnline())

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var popupWebViewState by remember { mutableStateOf<WebView?>(null) }
    var popupProgress by remember { mutableFloatStateOf(0f) }

    var canGoBack by remember { mutableStateOf(false) }
    var isInitialLoading by remember { mutableStateOf(true) }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var isErrorOccurred by remember { mutableStateOf(false) }

    var lastBackPressTime by remember { mutableStateOf(0L) }
    val exitWarningMessage = stringResource(R.string.press_again_to_exit)

    // File chooser callback state for uploads
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uriResults = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        filePathCallback?.onReceiveValue(uriResults)
        filePathCallback = null
    }

    // Handle Android Back Navigation
    BackHandler(enabled = true) {
        val popup = popupWebViewState
        if (popup != null) {
            if (popup.canGoBack()) {
                popup.goBack()
            } else {
                popup.destroy()
                popupWebViewState = null
            }
            return@BackHandler
        }

        val wv = webViewInstance
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            val currentTime = SystemClock.elapsedRealtime()
            if (currentTime - lastBackPressTime < 2000L) {
                onExit()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, exitWarningMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Retry action
    fun retryLoading() {
        if (!networkMonitor.isCurrentlyOnline()) {
            Toast.makeText(context, R.string.error_offline_title, Toast.LENGTH_SHORT).show()
            return
        }
        isErrorOccurred = false
        isInitialLoading = true
        pageProgress = 0.05f
        webViewInstance?.let { wv ->
            wv.loadUrl(TIKIA_URL)
        }
    }

    // Auto-retry when connection restores
    LaunchedEffect(isNetworkOnline) {
        if (isNetworkOnline && isErrorOccurred) {
            retryLoading()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(TikIABlackBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = TikIABlackBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(TikIABlackBackground)
        ) {
            // Main WebView container
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("tikia_webview"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // TikIA's native background is #09090b
                        setBackgroundColor(android.graphics.Color.parseColor("#09090b"))

                        // Enable Cookies & Third-party cookies for Firebase / Google auth
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        val rawUa = settings.userAgentString
                        val cleanUa = sanitizeUserAgent(rawUa)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            allowFileAccess = true
                            allowContentAccess = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            mediaPlaybackRequiresUserGesture = false

                            // Multiple windows support for Firebase / Google Sign-in popup
                            setSupportMultipleWindows(true)
                            javaScriptCanOpenWindowsAutomatically = true

                            // Modern mobile browser identity for Google OAuth
                            userAgentString = cleanUa

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                safeBrowsingEnabled = true
                            }
                        }

                        isVerticalScrollBarEnabled = true
                        isHorizontalScrollBarEnabled = false

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                canGoBack = view?.canGoBack() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                canGoBack = view?.canGoBack() == true
                                cookieManager.flush()
                                if (!isErrorOccurred) {
                                    isInitialLoading = false
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    // Only show offline screen on genuine connection failure
                                    if (!networkMonitor.isCurrentlyOnline()) {
                                        isErrorOccurred = true
                                        isInitialLoading = false
                                    }
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val uri = request?.url ?: return false
                                val scheme = uri.scheme?.lowercase() ?: ""
                                val host = uri.host?.lowercase() ?: ""
                                val urlLower = uri.toString().lowercase()

                                // Handle intent schemes
                                if (scheme == "mailto" || scheme == "tel" || scheme == "sms" || scheme == "intent") {
                                    return try {
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        ctx.startActivity(intent)
                                        true
                                    } catch (_: ActivityNotFoundException) {
                                        true
                                    }
                                }

                                // If this is an OAuth / auth redirect or app URL, stay inside the WebView!
                                if (request?.isRedirect == true ||
                                    host.contains("tik-ia.ai.studio") ||
                                    host.contains("ai.studio") ||
                                    host.contains("google.com") ||
                                    host.contains("gstatic.com") ||
                                    host.contains("googleusercontent.com") ||
                                    host.contains("firebaseapp.com") ||
                                    host.contains("web.app") ||
                                    host.contains("run.app") ||
                                    host.contains("googleapis.com") ||
                                    host.contains("identitytoolkit") ||
                                    urlLower.contains("auth") ||
                                    urlLower.contains("oauth") ||
                                    urlLower.contains("login") ||
                                    urlLower.contains("signin")
                                ) {
                                    return false
                                }

                                // External link opened via explicit user tap
                                if (request?.hasGesture() == true) {
                                    return try {
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        ctx.startActivity(intent)
                                        true
                                    } catch (_: Exception) {
                                        false
                                    }
                                }

                                return false
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                pageProgress = newProgress / 100f
                                if (newProgress >= 90 && !isErrorOccurred) {
                                    isInitialLoading = false
                                }
                            }

                            // Handle Firebase Auth signInWithPopup / window.open
                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: Message?
                            ): Boolean {
                                if (resultMsg == null) return false

                                val popup = WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    // Google sign-in forms are light background with dark text
                                    setBackgroundColor(android.graphics.Color.WHITE)

                                    val cm = CookieManager.getInstance()
                                    cm.setAcceptCookie(true)
                                    cm.setAcceptThirdPartyCookies(this, true)

                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        cacheMode = WebSettings.LOAD_DEFAULT
                                        setSupportZoom(true)
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                        userAgentString = cleanUa
                                        javaScriptCanOpenWindowsAutomatically = true
                                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    }

                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            v: WebView?,
                                            req: WebResourceRequest?
                                        ): Boolean {
                                            val reqUri = req?.url ?: return false
                                            val reqHost = reqUri.host?.lowercase() ?: ""

                                            // If the popup redirected back to tik-ia or auth handler
                                            if (reqHost.contains("tik-ia.ai.studio")) {
                                                popupWebViewState = null
                                                webViewInstance?.loadUrl(reqUri.toString())
                                                return true
                                            }
                                            return false
                                        }

                                        override fun onPageFinished(v: WebView?, url: String?) {
                                            super.onPageFinished(v, url)
                                            cm.flush()
                                        }
                                    }

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(v: WebView?, newProgress: Int) {
                                            super.onProgressChanged(v, newProgress)
                                            popupProgress = newProgress / 100f
                                        }

                                        override fun onCloseWindow(window: WebView?) {
                                            popupWebViewState = null
                                            window?.destroy()
                                            CookieManager.getInstance().flush()
                                            // Trigger reload or session check on main webview
                                            webViewInstance?.evaluateJavascript(
                                                "if (window.__tikiaAuthRefresh) window.__tikiaAuthRefresh();",
                                                null
                                            )
                                        }
                                    }
                                }

                                val transport = resultMsg.obj as? WebView.WebViewTransport
                                if (transport != null) {
                                    transport.webView = popup
                                    resultMsg.sendToTarget()
                                    popupWebViewState = popup
                                    return true
                                }
                                return false
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallbackRef: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                filePathCallback?.onReceiveValue(null)
                                filePathCallback = filePathCallbackRef

                                val intent = try {
                                    fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                        type = "*/*"
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                    }
                                } catch (_: Exception) {
                                    Intent(Intent.ACTION_GET_CONTENT).apply {
                                        type = "*/*"
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                    }
                                }

                                return try {
                                    fileChooserLauncher.launch(intent)
                                    true
                                } catch (_: Exception) {
                                    filePathCallback?.onReceiveValue(null)
                                    filePathCallback = null
                                    false
                                }
                            }
                        }

                        loadUrl(TIKIA_URL)
                        webViewInstance = this
                    }
                },
                update = { wv ->
                    webViewInstance = wv
                }
            )

            // Top Linear Progress Bar
            TikIATopProgressBar(
                progress = pageProgress,
                visible = pageProgress in 0.01f..0.99f && !isInitialLoading && !isErrorOccurred,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Initial Loading Splash Screen
            AnimatedVisibility(
                visible = isInitialLoading && !isErrorOccurred,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TikIALoadingScreen()
            }

            // Error / Offline Screen
            AnimatedVisibility(
                visible = isErrorOccurred || !isNetworkOnline,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TikIAErrorScreen(
                    onRetry = { retryLoading() }
                )
            }

            // Authentication Popup Dialog (Google / Firebase / Apple)
            popupWebViewState?.let { popupWv ->
                AuthPopupDialog(
                    popupWebView = popupWv,
                    progress = popupProgress,
                    onDismiss = {
                        popupWv.destroy()
                        popupWebViewState = null
                        CookieManager.getInstance().flush()
                    }
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            CookieManager.getInstance().flush()
            popupWebViewState?.destroy()
            popupWebViewState = null
            webViewInstance?.destroy()
        }
    }
}

/**
 * Secure Authentication Modal Dialog for Google/Firebase popups.
 */
@Composable
fun AuthPopupDialog(
    popupWebView: WebView,
    progress: Float,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .padding(horizontal = 8.dp, vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1E24),
            border = BorderStroke(1.dp, TikIABorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TikIABlackSurface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(TikIARedPrimary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Connexion TikIA",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TikIATextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Progress Indicator
                if (progress in 0.01f..0.99f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = TikIARedPrimary,
                        trackColor = TikIABlackSurface
                    )
                }

                // Embedded Web Content
                AndroidView(
                    factory = { popupWebView },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
