package com.yarik.encryptedsharedpreferences

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Base64

@SuppressLint("HardwareIds")
fun Context.getDeviceId(): String = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)

fun String.toBase64() = Base64.encodeToString(this.toByteArray(), Base64.NO_WRAP)