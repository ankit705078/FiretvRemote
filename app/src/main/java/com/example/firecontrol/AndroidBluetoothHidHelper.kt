package com.example.firecontrol

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Context
import java.util.concurrent.Executors

/**
 * Offline Bluetooth HID Helper for Fire TV Stick (Android 9+ / API 28+)
 * Reference: https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice
 * Reference: https://developer.android.com/reference/android/bluetooth/BluetoothHidDeviceAppSdpSettings
 *
 * Registers your Android phone as a standard Bluetooth HID Keyboard & Remote so your
 * Fire TV Stick can pair with it directly under:
 * Settings > Controllers & Bluetooth Devices > Other Bluetooth Devices
 * even when the Fire TV Stick has NO Wi-Fi connection!
 */
@SuppressLint("MissingPermission")
class AndroidBluetoothHidHelper(private val context: Context) {

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedHost: BluetoothDevice? = null

    // Standard 8-byte Keyboard HID Report Descriptor (Usage Page 0x07)
    // Supports D-Pad Up (0x52), Down (0x51), Left (0x50), Right (0x4F), Enter/Select (0x28),
    // Back/Escape (0x29), Home (0x4A), Menu (0x65), Volume Up (0x80), Volume Down (0x81), Mute (0x7F)
    private val keyboardDescriptor = byteArrayOf(
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop Ctrls)
        0x09.toByte(), 0x06.toByte(), // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x05.toByte(), 0x07.toByte(), //   Usage Page (Kbrd/Keypad)
        0x19.toByte(), 0xE0.toByte(), //   Usage Minimum (0xE0)
        0x29.toByte(), 0xE7.toByte(), //   Usage Maximum (0xE7)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(), //   Report Size (1)
        0x95.toByte(), 0x08.toByte(), //   Report Count (8)
        0x81.toByte(), 0x02.toByte(), //   Input (Data,Var,Abs) -> Modifier byte
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x81.toByte(), 0x01.toByte(), //   Input (Const) -> Reserved byte (0x00)
        0x95.toByte(), 0x06.toByte(), //   Report Count (6)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0xFF.toByte(), //   Logical Maximum (255)
        0x05.toByte(), 0x07.toByte(), //   Usage Page (Kbrd/Keypad)
        0x19.toByte(), 0x00.toByte(), //   Usage Minimum (0)
        0x29.toByte(), 0xFF.toByte(), //   Usage Maximum (255)
        0x81.toByte(), 0x00.toByte(), //   Input (Data,Array,Abs) -> 6 Keycode slots
        0xC0.toByte()                 // End Collection
    )

    fun registerHidProfile() {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = proxy as BluetoothHidDevice
                    val sdp = BluetoothHidDeviceAppSdpSettings(
                        "FireControl Remote",
                        "Dual-Mode Fire TV Remote & Keyboard",
                        "FireControl",
                        BluetoothHidDevice.SUBCLASS1_COMBO,
                        keyboardDescriptor
                    )
                    hidDevice?.registerApp(sdp, null, null, Executors.newSingleThreadExecutor(), object : BluetoothHidDevice.Callback() {
                        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
                            connectedHost = if (state == BluetoothProfile.STATE_CONNECTED) device else null
                        }
                    })
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                hidDevice = null
            }
        }, BluetoothProfile.HID_DEVICE)
    }

    /**
     * Sends an 8-byte HID key press report followed immediately by an all-zero release report.
     * Example usageIds: Up = 0x52, Down = 0x51, Left = 0x50, Right = 0x4F, Select = 0x28, Vol+ = 0x80, Vol- = 0x81
     */
    fun sendHidKey(usageId: Int) {
        val host = connectedHost ?: return
        val pressReport = byteArrayOf(0x00, 0x00, usageId.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00)
        val releaseReport = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        hidDevice?.sendReport(host, 0, pressReport)
        hidDevice?.sendReport(host, 0, releaseReport)
    }
}
