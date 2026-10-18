package com.keon.jamendoplayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(message: String) : Exception(message)

/** Client minimal de l'API Jamendo v3.0 (flux audio officiels, licences Creative Commons). */
object JamendoApi {
    private const val BASE = "https://api.jamendo.com/v3.0/tracks/"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun popular(id: String) = fetch(id, mapOf("order" to "popularity_week"))
    suspend fun newest(id: String) = fetch(id, mapOf("order" to "releasedate_desc"))
    suspend fun byTag(id: String, tag: String) = fetch(id, mapOf("order" to "popularity_week", "tags" to tag))
    suspend fun search(id: String, q: String) = fetch(id, mapOf("order" to "relevance", "search" to q))

    private suspend fun fetch(clientId: String, params: Map<String, String>): List<Track> =
        withContext(Dispatchers.IO) {
            if (clientId.isBlank()) {
                throw ApiException("Client ID Jamendo manquant. Ajoute-le dans Réglages.")
            }
            val url = BASE.toHttpUrl().newBuilder().apply {
                addQueryParameter("client_id", clientId)
                addQueryParameter("format", "json")
                addQueryParameter("limit", "30")
                addQueryParameter("imagesize", "400")
                addQueryParameter("audioformat", "mp32")
                params.forEach { (k, v) -> addQueryParameter(k, v) }
            }.build()
            try {
                client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                    val body = r.body?.string().orEmpty()
                    if (!r.isSuccessful) throw ApiException("Erreur Jamendo (${r.code}).")
                    parse(body)
                }
            } catch (e: IOException) {
                throw ApiException("Connexion réseau indisponible. Vérifie ta connexion.")
            }
        }

    private fun parse(body: String): List<Track> {
        val root = JSONObject(body)
        val headers = root.optJSONObject("headers")
        if (headers != null && headers.optString("status") != "success") {
            val msg = headers.optString("error_message").ifBlank { "Requête refusée" }
            throw ApiException("Jamendo : $msg")
        }
        val arr = root.optJSONArray("results") ?: return emptyList()
        val out = ArrayList<Track>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val audio = o.optString("audio")
            val id = o.optString("id")
            if (audio.isBlank() || id.isBlank()) continue
            out += Track(
                id = id,
                title = o.optString("name"),
                artist = o.optString("artist_name"),
                album = o.optString("album_name"),
                duration = o.optInt("duration"),
                image = o.optString("image"),
                audio = audio,
            )
        }
        return out
    }
}
