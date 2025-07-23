package com.example.localtrail.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.localtrail.model.enums.TrailPrivacy
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize
import java.util.Date

@Entity(tableName = "trails")
@Parcelize
data class Trail(
    @PrimaryKey
    @DocumentId
    val id: String = "",
    var userID: String = "",
    var name: String? = null,
    var location: String? = null,
    var description: String? = null,
    var privacy: TrailPrivacy = TrailPrivacy.FRIENDS_ONLY,
    var username: String = "",
    var distance: Double? = null,
    var duration: String? = null,
    var elevation: Int? = null,
    var avgSpeed: Double? = null,
    var effort: String? = null,
    var weather: String? = null,
    var tags: List<String>? = null,
    var notes: String? = null,
    var createdAt: Date = Date()
) : Parcelable
