package com.yunok.walzi.data.local

import androidx.room.TypeConverter

/**
 * Room can't store List<String> natively - this joins/splits on a delimiter unlikely to
 * appear in an ISO country code ("IN", "GLOBAL", etc). Registered on WalziDatabase via
 * @TypeConverters so CategoryEntity.countryCodes works transparently.
 */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String = value?.joinToString("|") ?: ""

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList() else value.split("|")
}