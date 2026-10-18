package com.keon.jamendoplayer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import kotlinx.coroutines.delay

private fun fmt(ms: Long): String {
    val s = (ms / 1000).toInt().coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun PlayerScreen(vm: AppViewModel, track: Track, onBack: () -> Unit) {
    val playing by vm.isPlaying.collectAsState()
    val buffering by vm.buffering.collectAsState()
    val shuffle by vm.shuffle.collectAsState()
    val repeat by vm.repeat.collectAsState()
    val favs by vm.favorites.collectAsState()
    val error by vm.playerError.collectAsState()

    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            if (!dragging) pos = vm.position()
            dur = vm.duration().takeIf { it > 0 } ?: (track.duration * 1000L)
            delay(500)
        }
    }

    val active = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.onSurfaceVariant
    val max = maxOf(dur, 1L).toFloat()

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
        Spacer(Modifier.height(8.dp))
        Cover(track.image, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)))
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                Text(track.artist, color = idle)
            }
            IconButton(onClick = { vm.toggleFavorite(track) }) {
                val fav = favs.any { it.id == track.id }
                Icon(
                    if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    "Favori",
                    tint = if (fav) active else idle,
                )
            }
        }
        val msg = error
        if (msg != null) {
            Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        } else if (buffering) {
            Text("Chargement…", color = idle, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        Slider(
            value = pos.toFloat().coerceIn(0f, max),
            onValueChange = { dragging = true; pos = it.toLong() },
            onValueChangeFinished = { vm.seekTo(pos); dragging = false },
            valueRange = 0f..max,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmt(pos), style = MaterialTheme.typography.bodySmall, color = idle)
            Text(fmt(dur), style = MaterialTheme.typography.bodySmall, color = idle)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.toggleShuffle() }) {
                Icon(Icons.Filled.Shuffle, "Aléatoire", tint = if (shuffle) active else idle)
            }
            IconButton(onClick = { vm.previous() }) { Icon(Icons.Filled.SkipPrevious, "Précédent", Modifier.size(36.dp)) }
            FilledIconButton(onClick = { vm.togglePlay() }, modifier = Modifier.size(68.dp)) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (playing) "Pause" else "Lecture",
                    Modifier.size(38.dp),
                )
            }
            IconButton(onClick = { vm.next() }) { Icon(Icons.Filled.SkipNext, "Suivant", Modifier.size(36.dp)) }
            IconButton(onClick = { vm.cycleRepeat() }) {
                Icon(
                    if (repeat == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    "Répéter",
                    tint = if (repeat == Player.REPEAT_MODE_OFF) idle else active,
                )
            }
        }
    }
}
