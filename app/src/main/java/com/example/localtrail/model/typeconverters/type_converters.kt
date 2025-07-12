package com.example.localtrail.model.typeconverters

import androidx.room.TypeConverter
import com.example.localtrail.model.enums.TrailPrivacy
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class TrailTypeConverters {
    @TypeConverter
    fun fromPrivacyToString(privacy: TrailPrivacy) : String {
        return privacy.name
    }

    @TypeConverter
    fun fromStringToPrivacy(string: String) : TrailPrivacy {
        return try {
            TrailPrivacy.valueOf(string)
        } catch (e: IllegalArgumentException) {
            TrailPrivacy.PUBLIC
        }
    }

    @TypeConverter
    fun fromStringListToString(list: List<String>?) : String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromJsonToStringList(json: String): List<String>? {
        val type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(json, type)
    }
}