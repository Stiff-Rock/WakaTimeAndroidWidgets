package com.stiffrock.wakatimewidgets.data.model

data class WakaTimeUser(
    val username: String
)

data class UserResponse(val data: WakaTimeUser)