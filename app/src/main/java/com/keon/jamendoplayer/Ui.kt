
package com.keon.jamendoplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B7CF6),
    onPrimary = Color.White,
    background = Color(0xFF09090B),
    surface = Color(0xFF09090B),
    surfaceVariant = Color(0xFF18181B),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFA1A1AA),
    secondaryContainer = Color(0xFF27272A),
    onSecondaryContainer = Color.White,
    outline = Color(0xFF3F3F46),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6D5BD0),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFFEDEDF0),
)

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val theme by vm.theme.collectAsState()

    val dark = when (theme) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true
    }

    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors
    ) {
        Surface(
            Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Root(vm)
        }
    }
}

@Composable
private fun Root(vm: AppViewModel) {
    val ctx = LocalContext.current

    val notif = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notif.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }

    val current by vm.current.collectAsState()
    val cur = current

    if (showPlayer && cur != null) {
        BackHandler { showPlayer = false }
        PlayerScreen(vm, cur) { showPlayer = false }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                if (cur != null) {
                    MiniPlayer(vm, cur) { showPlayer = true }
                }

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    listOf(
                        Icons.Filled.Home to "Accueil",
                        Icons.Filled.Search to "Recherche",
                        Icons.Filled.Favorite to "Favoris",
                        Icons.Filled.Settings to "Réglages",
                    ).forEachIndexed { i, (icon, label) ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(icon, label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
        },
    ) { pad ->
        Box(
            Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            when (tab) {
                0 -> HomeScreen(vm) { showPlayer = true }
                1 -> SearchScreen(vm) { showPlayer = true }
                2 -> FavoritesScreen(vm) { showPlayer = true }
                else -> SettingsScreen(vm)
            }
        }
    }
}

@Composable
fun Cover(url: String, modifier: Modifier = Modifier) {
    Box(
        modifier.background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (url.isBlank()) {
            Icon(
                Icons.Filled.MusicNote,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun TrackCard(t: Track, onClick: () -> Unit) {
    Column(
        Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Cover(
            t.image,
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
        )

        Spacer(Modifier.height(8.dp))

        Text(
            t.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            t.artist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TrackRow(
    t: Track,
    playing: Boolean,
    fav: Boolean,
    onFav: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(
            t.image,
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                t.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (playing) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )

            Text(
                t.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(onClick = onFav) {
            Icon(
                if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                if (fav) "Retirer des favoris" else "Ajouter aux favoris",
                tint = if (fav) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun Centered(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

@Composable
private fun Cards(
    state: Load<List<Track>>,
    onRetry: () -> Unit,
    onPlay: (List<Track>, Int) -> Unit
) {
    when (state) {
        is Load.Idle, is Load.Loading -> {
            Centered {
                CircularProgressIndicator(Modifier.size(28.dp))
            }
        }

        is Load.Err -> {
            Centered {
                Text(
                    state.message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onRetry) {
                    Text("Réessayer")
                }
            }
        }

        is Load.Ok -> {
            if (state.data.isEmpty()) {
                Centered {
                    Text(
                        "Aucun résultat.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.data.size, key = { state.data[it].id }) { i ->
                        TrackCard(state.data[i]) {
                            onPlay(state.data, i)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(vm: AppViewModel, openPlayer: () -> Unit) {
    val popular by vm.popular.collectAsState()
    val newest by vm.newest.collectAsState()
    val genreState by vm.genreTracks.collectAsState()
    val genre by vm.genre.collectAsState()

    LaunchedEffect(Unit) {
        vm.loadHome()
    }

    val retry = { vm.loadHome(force = true) }

    val play: (List<Track>, Int) -> Unit = { l, i ->
        vm.play(l, i)
        openPlayer()
    }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                "JamendoPlayer",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item {
            Text(
                "Populaires cette semaine",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(10.dp))
            Cards(popular, retry, play)
        }

        item {
            Text(
                "Nouveautés",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(10.dp))
            Cards(newest, retry, play)
        }

        item {
            Text(
                "Par genre",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(10.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(GENRES, key = { it.first }) { (tag, label) ->
                    FilterChip(
                        selected = genre == tag,
                        onClick = { vm.selectGenre(tag) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Cards(genreState, { vm.selectGenre(genre) }, play)
        }
    }
}

@Composable
private fun SearchScreen(vm: AppViewModel, openPlayer: () -> Unit) {
    val results by vm.results.collectAsState()
    val favs by vm.favorites.collectAsState()
    val current by vm.current.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Titres, artistes, albums…") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { vm.search(query) }
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )

        when (val s = results) {
            is Load.Idle -> {
                Centered {
                    Text(
                        "Recherche une chanson pour commencer.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is Load.Loading -> {
                Centered {
                    CircularProgressIndicator()
                }
            }

            is Load.Err -> {
                Centered {
                    Text(s.message)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { vm.search(query) }) {
                        Text("Réessayer")
                    }
                }
            }

            is Load.Ok -> {
                if (s.data.isEmpty()) {
                    Centered {
                        Text(
                            "Aucun résultat pour « $query ».",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn {
                        items(s.data.size, key = { s.data[it].id }) { i ->
                            val t = s.data[i]

                            TrackRow(
                                t,
                                t.id == current?.id,
                                favs.any { it.id == t.id },
                                { vm.toggleFavorite(t) }
                            ) {
                                vm.play(s.data, i)
                                openPlayer()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesScreen(vm: AppViewModel, openPlayer: () -> Unit) {
    val favs by vm.favorites.collectAsState()
    val current by vm.current.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Text(
            "Favoris",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        if (favs.isEmpty()) {
            Centered {
                Text(
                    "Aucun favori. Appuie sur le cœur d'un titre pour l'ajouter.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn {
                items(favs.size, key = { favs[it].id }) { i ->
                    val t = favs[i]

                    TrackRow(
                        t,
                        t.id == current?.id,
                        true,
                        { vm.toggleFavorite(t) }
                    ) {
                        vm.play(favs, i)
                        openPlayer()
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: AppViewModel) {
    val theme by vm.theme.collectAsState()
    val saved by vm.clientId.collectAsState()

    var cid by rememberSaveable { mutableStateOf(saved) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Réglages",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        item {
            Text("Thème", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "dark" to "Sombre",
                    "light" to "Clair",
                    "system" to "Système"
                ).forEach { (v, label) ->
                    FilterChip(
                        selected = theme == v,
                        onClick = { vm.setTheme(v) },
                        label = { Text(label) }
                    )
                }
            }
        }

        item {
            Text(
                "Client ID Jamendo",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = cid,
                onValueChange = { cid = it },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                placeholder = { Text("Optionnel si intégré au build") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            Button(onClick = { vm.setClientId(cid) }) {
                Text("Enregistrer")
            }
        }

        item {
            Text("À propos", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Text(
                "Musique fournie par Jamendo (jamendo.com), sous licences Creative Commons, via son API " +
                    "officielle. Les titres sont lus en streaming, sans téléchargement. Une connexion " +
                    "Internet est nécessaire.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MiniPlayer(vm: AppViewModel, t: Track, onOpen: () -> Unit) {
    val playing by vm.isPlaying.collectAsState()

    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(
            t.image,
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                t.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                t.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(onClick = { vm.togglePlay() }) {
            Icon(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                if (playing) "Pause" else "Lecture"
            )
        }

        IconButt