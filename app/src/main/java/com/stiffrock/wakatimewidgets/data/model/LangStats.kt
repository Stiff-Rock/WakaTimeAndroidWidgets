package com.stiffrock.wakatimewidgets.data.model

import com.google.gson.annotations.SerializedName

data class Language(
    val name: String,
    @SerializedName("total_seconds") val totalSeconds: Double,
    val percent: Double,
    val digital: String,
    val decimal: String,
    val text: String,
    val hours: Int,
    val minutes: Int,
    var color: String? = null
)

// Default eequals and hashCode functions to avoid warnings
data class WakaTimeLangStats(
    val languages: Array<Language>,
    @SerializedName("human_readable_total") val humanReadableTotal: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WakaTimeLangStats

        if (!languages.contentEquals(other.languages)) return false
        if (humanReadableTotal != other.humanReadableTotal) return false

        return true
    }

    override fun hashCode(): Int {
        var result = languages.contentHashCode()
        result = 31 * result + humanReadableTotal.hashCode()
        return result
    }
}

data class LangStatsResponse(val data: WakaTimeLangStats)