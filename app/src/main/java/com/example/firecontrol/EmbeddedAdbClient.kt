package com.example.firecontrol

import android.content.Context
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Pure-Kotlin Embedded ADB Client (No Python, No PC, No SDK Platform Tools Required!)
 * Uses `dev.mobile:dadb` to generate persistent RSA keys inside the app's private storage
 * and open direct TCP sockets to the Fire TV Stick on port 5555.
 */
class EmbeddedAdbClient(private val context: Context) {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private var activeTarget: String? = null
    private var activeDadb: Dadb? = null

    private fun getOrCreateKeyPair(): AdbKeyPair {
        val privKey = File(context.filesDir, "adbkey")
        val pubKey = File(context.filesDir, "adbkey.pub")
        if (!privKey.exists() || !pubKey.exists()) {
            AdbKeyPair.generate(privKey, pubKey)
        }
        return AdbKeyPair.read(privKey, pubKey)
    }

    private fun getConnection(hostPort: String): Dadb {
        val parts = hostPort.trim().split(":")
        val host = parts[0]
        val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 5555 else 5555

        if (activeDadb != null && activeTarget == "$host:$port") {
            return activeDadb!!
        }
        activeDadb?.close()
        val conn = Dadb.create(host, port, getOrCreateKeyPair())
        activeDadb = conn
        activeTarget = "$host:$port"
        return conn
    }

    fun sendShellCommand(hostPort: String, shellCmd: String) {
        ioScope.launch {
            try {
                getConnection(hostPort).shell(shellCmd)
            } catch (e: Exception) {
                activeDadb = null
            }
        }
    }
}
