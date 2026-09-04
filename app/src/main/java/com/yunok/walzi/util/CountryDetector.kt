package com.yunok.walzi.util

import java.util.Locale

/**
 * Device-locale-based country detection (see earlier discussion: simple, offline, no
 * permission needed - trades some accuracy for zero infra cost). Falls back to the "GLOBAL"
 * pseudo-country code on the rare device/emulator where the locale's country is blank.
 */
fun detectCountryCode(): String {
    val code = Locale.getDefault().country
    return code.ifBlank { "GLOBAL" }
}