package com.example.consolelauncher.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.consolelauncher.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    val repo = remember { AppRepository(context) }
    var apps by remember { mutableStateOf(emptyList<LauncherApp>()) }
    var selected by remember { mutableIntStateOf(0) }
    var showOptions by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val focus = remember { FocusRequester() }
    val owner = LocalLifecycleOwner.current
    val controller by produceState<ControllerInfo?>(readController()) {
        while (true) { value = readController(); delay(3_000) }
    }
    val time by produceState(LocalTime.now()) {
        while (true) { value = LocalTime.now(); delay(10_000) }
    }

    // Ricarica la lista ogni volta che si torna alla Home (nuove app installate)
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            apps = withContext(Dispatchers.IO) { repo.load() }
            selected = selected.coerceIn(0, (apps.size - 1).coerceAtLeast(0))
        }
    }
    LaunchedEffect(selected) { if (apps.isNotEmpty()) listState.animateScrollToItem(selected) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BackHandler {} // una Home non si chiude

    val current = apps.getOrNull(selected)
    fun move(delta: Int) { selected = (selected + delta).coerceIn(0, (apps.size - 1).coerceAtLeast(0)) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Tokens.GradientTop, Tokens.GradientBottom)))
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionRight -> { move(+1); true }
                    Key.DirectionLeft -> { move(-1); true }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter, Key.ButtonA -> {
                        current?.let(repo::launch); true
                    }
                    Key.ButtonStart, Key.Menu, Key.ButtonY -> { showOptions = true; true }
                    else -> false
                }
            }
            .windowInsetsPadding(WindowInsets.displayCutout)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            TopBar(time)

            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(
                    current?.label ?: "",
                    color = Color.White,
                    fontFamily = Tokens.Archivo,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    lineHeight = 32.sp,
                    maxLines = 1,
                )
                LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(apps, key = { _, a -> a.packageName }) { i, app ->
                        AppTile(app, selected = i == selected) {
                            if (i == selected) repo.launch(app) else selected = i
                        }
                    }
                }
            }

            BottomBar(
                controller = controller,
                onOptions = { showOptions = true },
                onLaunch = { current?.let(repo::launch) },
                onGrid = { /* TODO: schermata "Tutte le app" */ },
                onSettings = { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
            )
        }
    }

    if (showOptions && current != null) {
        AlertDialog(
            onDismissRequest = { showOptions = false },
            title = { Text(current.label) },
            confirmButton = {
                TextButton(onClick = {
                    showOptions = false
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${current.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }) { Text("Info app") }
            },
            dismissButton = { TextButton(onClick = { showOptions = false }) { Text("Annulla") } },
        )
    }
}

@Composable
private fun TopBar(time: LocalTime) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 18.75.sp)) { append(time.format(DateTimeFormatter.ofPattern("h:mm"))) }
                withStyle(SpanStyle(fontSize = 10.sp)) { append(time.format(DateTimeFormatter.ofPattern("a")).uppercase()) }
            },
            color = Color.White, fontFamily = Tokens.Nanum, fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Glyph(Icons.Outlined.Wifi, "Wi-Fi", 24.dp)
            Glyph(Icons.Outlined.BatteryFull, "Batteria", 24.dp)
        }
    }
}

@Composable
private fun AppTile(app: LauncherApp, selected: Boolean, onTap: () -> Unit) {
    val size by animateDpAsState(if (selected) 156.dp else 145.dp, label = "tile")
    val shape = RoundedCornerShape(8.dp)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .pointerInput(selected) { detectTapGestures { onTap() } },
        ) {
            Image(app.icon, app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (selected) Box(Modifier.fillMaxSize().border(4.dp, Color.White, shape))
            else Box(Modifier.fillMaxSize().background(Tokens.TileDim))
        }
    }
}

@Composable
private fun BottomBar(
    controller: ControllerInfo?,
    onOptions: () -> Unit,
    onLaunch: () -> Unit,
    onGrid: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        // Sinistra: batteria + controller
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Glyph(Icons.Outlined.BatteryFull, "Batteria controller", 24.dp)
                Label(controller?.batteryPercent?.let { "$it%" } ?: "—")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Glyph(Icons.Outlined.SportsEsports, "Controller", 24.dp)
                Column {
                    Text(controller?.name ?: "Nessun controller", color = Color.White, fontFamily = Tokens.Archivo,
                        fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                    Text(if (controller != null) "Connesso" else "Non connesso", color = Tokens.White50,
                        fontFamily = Tokens.Nanum, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                }
            }
        }

        // Centro: now playing + griglia + impostazioni
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            NowPlayingCard(sampleNowPlaying)
            SquareButton(Icons.Outlined.GridView, "Tutte le app", onGrid)
            SquareButton(Icons.Outlined.Settings, "Impostazioni", onSettings)
        }

        // Destra: Opzioni + Avvia
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.pointerInput(Unit) { detectTapGestures { onOptions() } },
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(24.dp)) {
                    Text("OPTIONS", color = Color.White, fontSize = 6.sp, fontWeight = FontWeight.SemiBold)
                    Box(Modifier.offset(9.75.dp, 9.dp).size(6.dp, 12.dp).clip(RoundedCornerShape(6.dp)).background(Color.White))
                }
                Label("Opzioni")
            }
            Row(
                Modifier.pointerInput(Unit) { detectTapGestures { onLaunch() } },
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Glyph(Icons.Outlined.Close, "Avvia", 24.dp)
                Label("Avvia")
            }
        }
    }
}

@Composable
private fun NowPlayingCard(np: NowPlaying) {
    Row(
        Modifier.height(56.dp).clip(RoundedCornerShape(12.dp)).background(Tokens.Glass).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(Tokens.White25)) // TODO: copertina
        Column(Modifier.padding(start = 12.dp, end = 4.dp).width(110.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(np.title, color = Color.White, fontFamily = Tokens.Archivo, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(np.position, color = Tokens.White50, fontFamily = Tokens.Archivo, fontSize = 12.sp)
                Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(4.dp)).background(Tokens.White25)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(np.progress).background(Color.White))
                }
            }
        }
    }
}

@Composable
private fun SquareButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Tokens.Glass)
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) { Glyph(icon, description, 22.dp) }
}

@Composable
private fun Glyph(icon: ImageVector, description: String, size: androidx.compose.ui.unit.Dp) =
    Icon(icon, description, Modifier.size(size), tint = Color.White)

@Composable
private fun Label(text: String) =
    Text(text, color = Color.White, fontFamily = Tokens.Nanum, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
