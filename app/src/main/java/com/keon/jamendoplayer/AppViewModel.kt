package com.keon.jamendoplayer

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

val GENRES = listOf(
    "pop" to "Pop", "rock" to "Rock", "electronic" to "Électro", "hiphop" to "Hip-hop",
    "jazz" to "Jazz", "lounge" to "Lounge", "classical" to "Classique", "reggae" to "Reggae",
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)

    val favorites = MutableStateFlow(prefs.favorites())
    val theme = MutableStateFlow(prefs.theme)
    val clientId = MutableStateFlow(prefs.clientId)

    val popular = MutableStateFlow<Load<List<Track>>>(Load.Idle)
    val newest = MutableStateFlow<Load<List<Track>>>(Load.Idle)
    val genreTracks = MutableStateFlow<Load<List<Track>>>(Load.Idle)
    val results = MutableStateFlow<Load<List<Track>>>(Load.Idle)
    val genre = MutableStateFlow(GENRES.first().first)

    val current = MutableStateFlow<Track?>(null)
    val isPlaying = MutableStateFlow(false)
    val buffering = MutableStateFlow(false)
    val shuffle = MutableStateFlow(false)
    val repeat = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val playerError = MutableStateFlow<String?>(null)

    private val known = HashMap<String, Track>()
    private val loadJobs = HashMap<MutableStateFlow<Load<List<Track>>>, Job>()
    private var controller: MediaController? = null
    private val future: ListenableFuture<MediaController>

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync(player)
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            playerError.value = "Lecture impossible. Vérifie ta connexion Internet."
        }
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            runCatching { future.get() }.getOrNull()?.let {
                controller = it
                it.addListener(listener)
                sync(it)
            }
        }, ContextCompat.getMainExecutor(app))
    }

    private fun sync(p: Player) {
        current.value = trackFrom(p.currentMediaItem)
        isPlaying.value = p.isPlaying
        buffering.value = p.playbackState == Player.STATE_BUFFERING
        shuffle.value = p.shuffleModeEnabled
        repeat.value = p.repeatMode
        if (p.playbackState == Player.STATE_READY) playerError.value = null
    }

    private fun trackFrom(item: MediaItem?): Track? {
        if (item == null) return null
        known[item.mediaId]?.let { return it }
        favorites.value.firstOrNull { it.id == item.mediaId }?.let { return it }
        val m = item.mediaMetadata
        return Track(
            id = item.mediaId,
            title = m.title?.toString().orEmpty(),
            artist = m.artist?.toString().orEmpty(),
            album = m.albumTitle?.toString().orEmpty(),
            duration = 0,
            image = m.artworkUri?.toString().orEmpty(),
            audio = item.requestMetadata.mediaUri?.toString().orEmpty(),
        )
    }

    private fun cid() = clientId.value.ifBlank { BuildConfig.JAMENDO_CLIENT_ID }

    private fun launchLoad(target: MutableStateFlow<Load<List<Track>>>, block: suspend () -> List<Track>) {
        loadJobs[target]?.cancel()
        target.value = Load.Loading
        loadJobs[target] = viewModelScope.launch {
            try {
                val list = block()
                target.value = Load.Ok(list.also { tracks -> tracks.forEach { known[it.id] = it } })
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                target.value = Load.Err(e.message ?: "Erreur")
            } catch (e: Exception) {
                target.value = Load.Err("Erreur inattendue. Réessaie.")
            }
        }
    }

    private fun needs(s: Load<*>) = s is Load.Idle || s is Load.Err

    fun loadHome(force: Boolean = false) {
        if (force || needs(popular.value)) launchLoad(popular) { JamendoApi.popular(cid()) }
        if (force || needs(newest.value)) launchLoad(newest) { JamendoApi.newest(cid()) }
        if (force || needs(genreTracks.value)) launchLoad(genreTracks) { JamendoApi.byTag(cid(), genre.value) }
    }

    fun selectGenre(tag: String) {
        genre.value = tag
        launchLoad(genreTracks) { JamendoApi.byTag(cid(), tag) }
    }

    fun search(query: String) {
        val q = query.trim()
        if (q.isEmpty()) {
            loadJobs[results]?.cancel()
            results.value = Load.Idle
            return
        }
        launchLoad(results) { JamendoApi.search(cid(), q) }
    }

    private fun item(t: Track) = MediaItem.Builder()
        .setMediaId(t.id)
        .setUri(t.audio)
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(Uri.parse(t.audio)).build())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(t.title).setArtist(t.artist).setAlbumTitle(t.album)
                .setArtworkUri(if (t.image.isBlank()) null else Uri.parse(t.image))
                .build(),
        )
        .build()

    fun play(list: List<Track>, index: Int, shuffleOn: Boolean = false) {
        val c = controller ?: return
        if (list.isEmpty()) return
        list.forEach { known[it.id] = it }
        playerError.value = null
        c.shuffleModeEnabled = shuffleOn
        c.setMediaItems(list.map(::item), index.coerceIn(0, list.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            playerError.value = null
            c.play()
        }
    }

    fun next() { controller?.seekToNextMediaItem() }
    fun previous() {
        val c = controller ?: return
        if (c.currentPosition > 3000) c.seekTo(0) else c.seekToPreviousMediaItem()
    }
    fun seekTo(ms: Long) { controller?.seekTo(ms) }
    fun position(): Long = controller?.currentPosition ?: 0L
    fun duration(): Long = controller?.duration?.takeIf { it > 0 } ?: 0L
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun toggleFavorite(t: Track) {
        val l = favorites.value
        favorites.value = if (l.any { it.id == t.id }) l.filterNot { it.id == t.id } else l + t
        prefs.saveFavorites(favorites.value)
    }

    fun setTheme(v: String) {
        prefs.theme = v
        theme.value = v
    }

    fun setClientId(v: String) {
        prefs.clientId = v.trim()
        clientId.value = v.trim()
        loadJobs.values.forEach { it.cancel() }
        loadJobs.clear()
        popular.value = Load.Idle
        newest.value = Load.Idle
        genreTracks.value = Load.Idle
        results.value = Load.Idle
    }

    override fun onCleared() {
        loadJobs.values.forEach { it.cancel() }
        loadJobs.clear()
        controller?.removeListener(listener)
        MediaController.releaseFuture(future)
        super.onCleared()
    }
}
