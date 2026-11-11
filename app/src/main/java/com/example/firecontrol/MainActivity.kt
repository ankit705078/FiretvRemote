package com.example.firecontrol

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

/**
 * MainActivity hosts the self-contained `firetv_remote.html` interface in a full-screen
 * WebView and injects `window.FireControlNative` so every tap on the UI directly invokes
 * either the embedded pure-Kotlin ADB client or the native Android BluetoothHidDevice profile.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var adbClient: EmbeddedAdbClient
    private lateinit var btHidHelper: AndroidBluetoothHidHelper

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adbClient = EmbeddedAdbClient(this)
        btHidHelper = AndroidBluetoothHidHelper(this)
        btHidHelper.registerHidProfile()

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            addJavascriptInterface(FireControlBridge(), "FireControlNative")
            loadUrl("file:///android_asset/firetv_remote.html")
        }

        setContentView(webView)
    }

    inner class FireControlBridge {
        @JavascriptInterface
        fun sendAdbKeyEvent(hostPort: String, keyCode: Int) {
            adbClient.sendShellCommand(hostPort, "input keyevent $keyCode")
        }

        @JavascriptInterface
        fun sendAdbSwipe(hostPort: String, x1: Int, y1: Int, x2: Int, y2: Int) {
            adbClient.sendShellCommand(hostPort, "input swipe $x1 $y1 $x2 $y2 150")
        }

        @JavascriptInterface
        fun sendAdbText(hostPort: String, text: String) {
            val escaped = text.replace(" ", "%s")
            adbClient.sendShellCommand(hostPort, "input text \"$escaped\"")
        }

        @JavascriptInterface
        fun launchAdbPackage(hostPort: String, pkg: String) {
            adbClient.sendShellCommand(hostPort, "monkey -p $pkg -c android.intent.category.LAUNCHER 1")
        }

        @JavascriptInterface
        fun sendBluetoothHidKey(usageId: Int) {
            btHidHelper.sendHidKey(usageId)
        }
    }
}
