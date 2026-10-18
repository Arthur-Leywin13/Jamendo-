package com.keon.jamendoplayer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("jamendoplayer", Context.MODE_PRIVATE)

    fun favorites(): List<Track> = runCatching {
        val arr = JSONArray(sp.getString("favs", "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Track(
                o.getString("id"), o.getString("title"), o.getString("artist"), o.optString("album"),
                o.optInt("duration"), o.optString("image"), o.getString("audio"),
            )
        }
    }.getOrDefault(emptyList())

    fun saveFavorites(list: List<Track>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("title", it.title).put("artist", it.artist)
                    .put("album", it.album).put("duration", it.duration).put("image", it.image).put("audio", it.audio),
            )
        }
        sp.edit().putString("favs", arr.toString()).apply()
    }

    var theme: String
        get() = sp.getString("theme", "dark") ?: "dark"
        set(v) = sp.edit().putString("theme", v).apply()

    var clientId: String
        get() = sp.getString("client_id", "") ?: ""
        set(v) = sp.edit().putString("client_id", v).apply()
}
