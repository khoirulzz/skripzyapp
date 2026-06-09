package id.skripzy.app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.FileChooserParams
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import id.skripzy.app.ui.theme.SuperWebViewTheme
import java.io.File

private const val TAG = "SkripzyWebView"
private const val WEBSITE = "https://app.skripzy.id"
private val WEBSITE_HOST = Uri.parse(WEBSITE).host
private const val USER_AGENT_PREFIX = "SkripzyApp/2.10.1"

// Required permissions for full functionality
private val REQUIRED_PERMISSIONS = arrayOf(
    android.Manifest.permission.INTERNET,
    android.Manifest.permission.ACCESS_NETWORK_STATE,
    android.Manifest.permission.CAMERA,
    android.Manifest.permission.READ_EXTERNAL_STORAGE,
    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
)

private val OPTIONAL_PERMISSIONS = arrayOf(
    android.Manifest.permission.ACCESS_FINE_LOCATION,
    android.Manifest.permission.ACCESS_COARSE_LOCATION,
)

class MainActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            SuperWebViewTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MainScreen() {
    var progress by rememberSaveable { mutableIntStateOf(0) }
    var fullScreenView by remember { mutableStateOf<View?>(null) }

    BackHandler(enabled = fullScreenView != null) {
        fullScreenView = null
    }

    Box(Modifier.fillMaxSize()) {
        WebViewWithRefresher(
            modifier = Modifier.fillMaxSize(),
            updateProgress = { currentProgress -> progress = currentProgress },
            onViewReceived = { view -> fullScreenView = view },
            isFullScreen = fullScreenView != null,
        )
        ProgressIndicator(progress)
    }

    fullScreenView?.let { view ->
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { view }
        )
    }
}

@Composable
private fun ProgressIndicator(progress: Int) {
    AnimatedVisibility(
        modifier = Modifier.fillMaxWidth(),
        visible = progress in 1..99
    ) {
        LinearProgressIndicator(progress = { progress.toFloat() / 100 })
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewWithRefresher(
    modifier: Modifier = Modifier,
    updateProgress: (Int) -> Unit,
    onViewReceived: (View?) -> Unit,
    isFullScreen: Boolean,
) {
    var webView: WebView? = null
    val webViewId = View.generateViewId()
    var isBackEnabled by rememberSaveable { mutableStateOf(false) }
    val primaryColorArgb = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryColorArgb = MaterialTheme.colorScheme.secondary.toArgb()
    val tertiaryColorArgb = MaterialTheme.colorScheme.tertiary.toArgb()

    var pendingFilePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Permission handlers
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "Permissions requested: $permissions")
        val context = LocalContext.current
        permissions.forEach { (permission, granted) ->
            Log.d(TAG, "Permission $permission: ${if (granted) "GRANTED" else "DENIED"}")
        }
    }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = pendingFilePathCallback
        pendingFilePathCallback = null

        if (callback == null) return@rememberLauncherForActivityResult

        if (result.resultCode != Activity.RESULT_OK) {
            callback.onReceiveValue(null)
            cameraImageUri = null
            return@rememberLauncherForActivityResult
        }

        val uris = mutableListOf<Uri>()
        val data = result.data

        when {
            data?.clipData != null -> {
                val clipData = data.clipData!!
                for (index in 0 until clipData.itemCount) {
                    clipData.getItemAt(index).uri?.let(uris::add)
                }
            }

            data?.data != null -> {
                uris.add(data.data!!)
            }

            cameraImageUri != null -> {
                uris.add(cameraImageUri!!)
            }
        }

        callback.onReceiveValue(uris.takeIf { it.isNotEmpty() }?.toTypedArray())
        cameraImageUri = null
    }

    fun createCameraIntent(context: Context): Intent? {
        val imageFile = try {
            File.createTempFile("skripzy_camera_", ".jpg", context.cacheDir)
        } catch (_: Exception) {
            return null
        }

        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                imageFile
            )
        } catch (_: Exception) {
            return null
        }

        cameraImageUri = uri

        return Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            clipData = ClipData.newUri(context.contentResolver, "camera", uri)
        }.takeIf { it.resolveActivity(context.packageManager) != null }
    }

    fun buildFileChooserIntent(
        context: Context,
        fileChooserParams: FileChooserParams,
    ): Intent? {
        val acceptTypes = fileChooserParams.acceptTypes
            .mapNotNull { it.trim().takeIf(String::isNotBlank) }
            .ifEmpty { listOf("*/*") }

        val contentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = if (acceptTypes.size == 1) acceptTypes.first() else "*/*"
            putExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE
            )
            if (acceptTypes.size > 1 || acceptTypes.first() != "*/*") {
                putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes.toTypedArray())
            }
        }

        val chooserInitialIntents = mutableListOf<Intent>()
        val cameraIntent = when {
            acceptTypes.any { type ->
                val normalized = type.lowercase()
                normalized == "image/*" || normalized.startsWith("image/")
            } -> createCameraIntent(context)
            fileChooserParams.isCaptureEnabled -> createCameraIntent(context)
            else -> null
        }

        if (cameraIntent != null) chooserInitialIntents.add(cameraIntent)

        return Intent.createChooser(contentIntent, "Select file").apply {
            if (chooserInitialIntents.isNotEmpty()) {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, chooserInitialIntents.toTypedArray())
            }
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    BackHandler(enabled = isBackEnabled && !isFullScreen) {
        webView?.goBack()
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            val swipeRefreshLayout = SwipeRefreshLayout(context).apply {
                setColorSchemeColors(
                    primaryColorArgb,
                    secondaryColorArgb,
                    tertiaryColorArgb
                )
                setOnRefreshListener {
                    webView?.reload()
                }
            }

            webView = WebView(context).apply {
                id = webViewId
                setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                    handleDownload(
                        context = context,
                        url = url,
                        userAgent = userAgent,
                        contentDisposition = contentDisposition,
                        mimeType = mimeType
                    )
                }

                webViewClient = object : WebViewClient() {
                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return handleUrlLoading(context, url)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return handleUrlLoading(context, request?.url?.toString())
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        isBackEnabled = view?.canGoBack() == true
                        Log.d(TAG, "Page started: $url")
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        swipeRefreshLayout.isRefreshing = false
                        Log.e(TAG, "WebView error: ${error?.description} (${error?.errorCode})")
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        updateProgress(newProgress)
                        if (newProgress == 100) swipeRefreshLayout.isRefreshing = false
                    }

                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        onViewReceived(view)
                        super.onShowCustomView(view, callback)
                        Log.d(TAG, "Fullscreen mode enabled")
                    }

                    override fun onHideCustomView() {
                        onViewReceived(null)
                        super.onHideCustomView()
                        Log.d(TAG, "Fullscreen mode disabled")
                    }

                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            Log.d(TAG, "[WebConsole] ${it.message()} (${it.lineNumber()}:${it.sourceId()})")
                        }
                        return true
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        val params = fileChooserParams ?: return false
                        pendingFilePathCallback?.onReceiveValue(null)
                        pendingFilePathCallback = filePathCallback

                        val chooserIntent = buildFileChooserIntent(context, params) ?: run {
                            pendingFilePathCallback = null
                            return false
                        }

                        fileChooserLauncher.launch(chooserIntent)
                        return true
                    }
                }

                with(settings) {
                    domStorageEnabled = true
                    javaScriptEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    allowFileAccess = true
                    allowContentAccess = true
                    mediaPlaybackRequiresUserGesture = false
                    setSupportZoom(false)
                    
                    // Caching strategy
                    cacheMode = WebSettings.LOAD_DEFAULT
                    databaseEnabled = true
                    
                    // User-Agent
                    userAgentString = "$USER_AGENT_PREFIX ${WebSettings.getDefaultUserAgent(context)}"
                    
                    // Dark mode support
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        isAlgorithmicDarkeningAllowed = true
                    }
                    
                    // Mixed content mode
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    }
                }

                // Enable debugging in debug builds
                if (BuildConfig.DEBUG) {
                    WebView.setWebContentsDebuggingEnabled(true)
                }

                // Enable cookies
                CookieManager.getInstance().setAcceptCookie(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                }

                loadUrl(WEBSITE)
                Log.d(TAG, "WebView initialized and loading: $WEBSITE")
            }

            swipeRefreshLayout.addView(webView)
            swipeRefreshLayout
        },
        update = { swipeRefreshLayout ->
            webView = swipeRefreshLayout.findViewById(webViewId)
        }
    )
}

private fun handleUrlLoading(context: Context, rawUrl: String?): Boolean {
    val url = rawUrl?.trim().orEmpty()
    if (url.isBlank()) return false

    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    val host = uri.host.orEmpty()

    val isInternal = host == WEBSITE_HOST || host.endsWith(".skripzy.id")
    if (isInternal || uri.scheme.isNullOrBlank()) {
        return false
    }

    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    } catch (_: Exception) {
        false
    }
}

private fun handleDownload(
    context: Context,
    url: String,
    userAgent: String,
    contentDisposition: String?,
    mimeType: String?,
) {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
    if (uri.scheme !in listOf("http", "https")) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
        }
        return
    }

    val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
    val request = DownloadManager.Request(uri).apply {
        setMimeType(mimeType)
        addRequestHeader("User-Agent", userAgent)
        CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
        setTitle(fileName)
        setDescription("Downloading file")
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        try {
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        } catch (_: Exception) {
        }
    }

    runCatching {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }.onFailure {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
        }
    }
}
