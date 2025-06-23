package com.example.localtrail.model

import com.example.localtrail.model.enums.TrailPrivacy

class Trail() {
    var id: String = ""
    var userID: String = ""
    var name: String? = null
    var location: String? = null
    var description: String? = null
    var privacy: TrailPrivacy = TrailPrivacy.PUBLIC

    constructor(
        id: String = "",
        userID: String = "",
        name: String? = null,
        location: String? = null,
        description: String? = null,
        privacy: TrailPrivacy = TrailPrivacy.PUBLIC
    ) : this() {
        this.id = id
        this.userID = userID
        this.name = name
        this.location = location
        this.description = description
        this.privacy = privacy
    }
}
