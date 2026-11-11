package com.example.firecontrol

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import java.util.UUID
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class AndroidBluetoothHidHelper(private val context: Context) {

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedHost: BluetoothDevice? = null
    private var advertiser: BluetoothLeAdvertiser? = null

    // Standard Bluetooth HID Service UUID (0x1812) — Makes Fire TV recognize the phone as a Keyboard
    private val HID_UUID = ParcelUuid(UUID.fromString("00001812-0000-1000-8000-00805f9b34fb"))

    private val keyboardDescriptor = byteArrayOf(
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x06.toByte(), // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x05.toByte(), 0x07.toByte(), //   Usage Page (Keypad)
        0x19.toByte(), 0xE0.toByte(), //   Usage Min (0xE0)
        0x29.toByte(), 0xE7.toByte(), //   Usage Max (0xE7)
        0x15.toByte(), 0x00.toByte(), //   Logical Min (0)
        0x25.toByte(), 0x01.toByte(), //   Logical Max (1)
        0x75.toByte(), 0x01.toByte(), //   Report Size (1)
        0x95.toByte(), 0x08.toByte(), //   Report Count (8)
        0x81.toByte(), 0x02.toByte(), //   Input (Data,Var,Abs)
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x81.toByte(), 0x01.toByte(), //   Input (Const)
        0x95.toByte(), 0x06.toByte(), //   Report Count (6)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x15.toByte(), 0x00.toByte(), //   Logical Min (0)
        0x25.toByte(), 0xFF.toByte(), //   Logical Max (255)
        0x05.toByte(), 0x07.toByte(), //   Usage Page (Keypad)
        0x19.toByte(), 0x00.toByte(), //   Usage Min (0)
        0x29.toByte(), 0xFF.toByte(), //   Usage Max (255)
        0x81.toByte(), 0x00.toByte(), //   Input (Data,Array,Abs)
        0xC0.toByte()                 // End Collection
    )

    fun registerHidProfile() {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return

        // 1. Start Bluetooth Advertising as a Wireless Keyboard
        try {
            advertiser = adapter.bluetoothLeAdvertiser
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(HID_UUID)
                .build()

            advertiser?.startAdvertising(settings, data, object : AdvertiseCallback() {})
        } catch (e: Exception) {}

        // 2. Register Bluetooth HID Device Profile Proxy
        try {
            adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onConnectionStateChanged(profile: Int, state: Int) {}

                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.HID_DEVICE) {
                        hidDevice = proxy as BluetoothHidDevice
                        val sdp = BluetoothHidDeviceAppSdpSettings(
                            "FireControl Remote",
                            "Fire TV Bluetooth Keyboard & Remote",
                            "FireControl",
                            BluetoothHidDevice.SUBCLASS1_KEYBOARD,
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
        } catch (e: Exception) {}
    }

    fun sendHidKey(usageId: Int) {
        val host = connectedHost ?: return
        val pressReport = byteArrayOf(0x00, 0x00, usageId.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00)
        val releaseReport = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        hidDevice?.sendReport(host, 0, pressReport)
        hidDevice?.sendReport(host, 0, releaseReport)
    }
}
