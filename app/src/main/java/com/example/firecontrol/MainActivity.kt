package com.example.firecontrol

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var adbClient: EmbeddedAdbClient
    private lateinit var btHidHelper: AndroidBluetoothHidHelper

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Embedded ADB Client
        adbClient = EmbeddedAdbClient(this)
        btHidHelper = AndroidBluetoothHidHelper(this)

        // Safely check and request Bluetooth permissions without crashing
        checkAndRequestPermissions()

        // Setup WebView
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

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
        } else {
            initBluetoothSafely()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        initBluetoothSafely()
    }

    private fun initBluetoothSafely() {
        try {
            btHidHelper.registerHidProfile()
        } catch (e: Exception) {
            // Fails gracefully if device does not support Bluetooth HID or Bluetooth is toggled off
        }
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
            try {
                btHidHelper.sendHidKey(usageId)
            } catch (e: Exception) {}
        }
    }
}
