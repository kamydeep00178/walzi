package com.yunok.walzi.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String = value?.joinToString("|") ?: ""

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList() else value.split("|")
}