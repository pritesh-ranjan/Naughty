package com.example.naughty.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromBindingType(value: BindingType): String = value.name

    @TypeConverter
    fun toBindingType(value: String): BindingType = BindingType.valueOf(value)
}
