package com.example.localtrail.model

import android.os.Parcelable
import com.example.localtrail.model.enums.TrailPrivacy
import kotlinx.parcelize.Parcelize

@Parcelize
class Trail(
    var id: String = "",
    var userID: String = "",
    var name: String? = null,
    var location: String? = null,
    var description: String? = null,
    var privacy: TrailPrivacy = TrailPrivacy.PUBLIC,
    var username: String = "",
    var distance: Double? = null,
    var duration: String? = null,
    var elevation: Int? = null,
    var avgSpeed: Double? = null,
    var effort: String? = null,
    var weather: String? = null,
    var tags: List<String>? = null,
    var notes: String? = null
) : Parcelable
