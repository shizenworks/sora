// ForceProviderSheet.kt

package com.mochyy.mochymusic.ui.player.info

import androidx.compose.animation.AnimatedVisibility
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.mochyy.mochymusic.module.TrackQuery
import com.mochyy.mochymusic.module.SourceStream
import com.mochyy.mochymusic.module.StreamFormat
import com.mochyy.mochymusic.module.SourceModule
import com.mochyy.mochymusic.module.ResolveResult
import com.mochyy.mochymusic.module.ModuleFeature
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.size
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.LocalPlayerConnection
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.AudioQuality
import com.mochyy.mochymusic.constants.AudioQualityKey
import com.mochyy.mochymusic.module.StreamOption
import com.mochyy.mochymusic.modules.SourceModules
import com.mochyy.mochymusic.playback.ProviderSwitch
import com.mochyy.mochymusic.playback.ProviderSwitchState
import com.mochyy.mochymusic.ui.player.menu.LocalMenuSheetClose
import com.mochyy.mochymusic.ui.player.menu.MenuRowSurface
import com.mochyy.mochymusic.ui.player.menu.MenuSheetSurface
import com.mochyy.mochymusic.utils.rememberPreference
import kotlinx.coroutines.delay
import com.mochyy.mochymusic.ui.component.RemoteFocus

private val YouTubeQualities = listOf(AudioQuality.AUTO, AudioQuality.HIGH, AudioQuality.MEDIUM, AudioQuality.LOW)

private fun qualityLabelRes(quality: AudioQuality): Int = when (quality) {
    AudioQuality.HIGH -> R.string.aq_quality_high
    AudioQuality.MEDIUM -> R.string.aq_quality_medium
    AudioQuality.LOW -> R.string.aq_quality_low
    else -> R.string.aq_quality_auto
}

/** "force change": plays this song from another of the providers that are switched on, at the quality picked */
@Composable
fun ForceProviderSheet(currentProviderId: String?, onDismiss: () -> Unit) {
    val connection = LocalPlayerConnection.current ?: return
    val service = connection.service
    val modules = service.sourceModules
    val order by modules.order.collectAsState()
    val youTubeOn by modules.youTubeEnabled.collectAsState()
    val entries by modules.entries.collectAsState()
    val state by service.providerSwitch.state.collectAsState()
    val (youTubeQuality, setYouTubeQuality) = rememberPreference(AudioQualityKey, AudioQuality.AUTO.name)
    // every source the app has, in the play order; one that's off or signed out shows why and can still be searched
    val providers = remember(order, youTubeOn, entries) {
        (order + SourceModules.YouTubeId + entries.filter { ModuleFeature.AudioSource in it.module.adds }.map { it.module.info.id })
            .distinct()
            .filter { it == SourceModules.YouTubeId || modules.module(it) != null }
    }
    val metadata by connection.mediaMetadata.collectAsState()
    val searchScope = rememberCoroutineScope()
    val catalog = remember { mutableStateMapOf<String, CatalogCheck>() }

    // reading every module's account and settings again is real work; it waits until the sheet is in
    // and runs off the main thread, so the entrance never stalls on it
    LaunchedEffect(Unit) {
        delay(320)
        withContext(Dispatchers.Default) { modules.refresh() }
    }
    var resultsFor by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) { onDispose { service.providerSwitch.acknowledge() } }

    MenuSheetSurface(onDismiss = onDismiss, wrapHeight = true) { _ ->
        val close = LocalMenuSheetClose.current
        // once it plays from the new provider there's nothing left to choose
        LaunchedEffect(state) {
            if (state is ProviderSwitchState.Done) {
                delay(700)
                close()
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            // with every source installed the rows run past the screen; the sheet caps at its
            // height and the list scrolls inside it, so the last source (Muzo Data at the end
            // of a rearranged order) stays reachable instead of being cut off behind the bar
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            InfoColumn(modifier = Modifier.widthIn(max = 600.dp)) {
                InfoSheetTitle(stringResource(R.string.pc_force_title))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.pc_force_hint),
                    color = InfoMuted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(16.dp))
                SwitchStatus(state, nameOf = modules::nameOf)
                if (providers.isEmpty()) {
                    Text(text = stringResource(R.string.pc_switch_source_empty), color = InfoMuted, fontSize = 13.sp)
                }
                providers.forEachIndexed { index, id ->
                    val module = modules.module(id)
                    val usable = modules.isUsable(id)
                    val blocked = when {
                        usable -> null
                        !modules.isEnabled(id) -> stringResource(R.string.pc_force_off)
                        else -> stringResource(R.string.pc_force_sign_in)
                    }
                    val choice = module?.choices?.firstOrNull { it.id.contains("quality", ignoreCase = true) }
                    val picked = choice?.let { c -> modules.picks(id)[c.id] ?: c.options.firstOrNull()?.id }
                    // rows fade and rise in a layer rather than grow, so the sheet isn't measured again on every frame
                    val rise = remember(id) { androidx.compose.animation.core.Animatable(if (com.mochyy.mochymusic.ui.utils.AppMotion.enabled) 0f else 1f) }
                    LaunchedEffect(id) { rise.animateTo(1f, tween(280, delayMillis = index * 40, easing = SheetEasing)) }
                    androidx.compose.foundation.layout.Box(
                        Modifier.graphicsLayer {
                            alpha = rise.value
                            translationY = (1f - rise.value) * 12.dp.toPx()
                        },
                    ) {
                    val name = modules.nameOf(id)
                    ProviderChoiceRow(
                        providerId = id,
                        name = name,
                        isCurrent = id == currentProviderId,
                        busy = state is ProviderSwitchState.Working,
                        blocked = blocked,
                        check = catalog[id],
                        onSearch = {
                            if (module != null) {
                                resultsFor = id
                            } else {
                                catalog[id] = CatalogCheck.Looking
                                searchScope.launch {
                                    catalog[id] = lookUp(id, module, metadata, modules)
                                }
                            }
                        },
                        options = when {
                            choice != null -> choice.options
                            id == SourceModules.YouTubeId -> YouTubeQualities.map { StreamOption(it.name, stringResource(qualityLabelRes(it)), "") }
                            else -> emptyList()
                        },
                        selectedOption = if (id == SourceModules.YouTubeId) youTubeQuality else picked,
                        onPick = { option ->
                            if (option != null) {
                                if (id == SourceModules.YouTubeId) setYouTubeQuality(option) else choice?.let { modules.setPick(id, it.id, option) }
                            }
                            service.providerSwitch.force(id)
                        },
                    )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
    resultsFor?.let { id ->
        val module = modules.module(id)
        if (module != null) {
            CatalogResultsSheet(
                providerId = id,
                providerName = modules.nameOf(id),
                module = module,
                metadata = metadata,
                modules = modules,
                onPicked = { stream ->
                    resultsFor = null
                    service.providerSwitch.force(id, ready = stream)
                },
                onDismiss = { resultsFor = null },
            )
        }
    }
}

@Composable
private fun SwitchStatus(state: ProviderSwitchState, nameOf: (String) -> String) {
    AnimatedVisibility(
        visible = state !is ProviderSwitchState.Idle,
        enter = fadeIn(tween(200)) + expandVertically(tween(260)),
        exit = fadeOut(tween(160)) + shrinkVertically(tween(220)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MenuRowSurface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            when (state) {
                is ProviderSwitchState.Working -> {
                    Text(stringResource(R.string.pc_force_fetching, nameOf(state.providerId)), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.pc_force_try_of, state.attempt, state.of), color = InfoMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    val progress by animateFloatAsState(state.attempt / state.of.toFloat(), label = "switchTries")
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))) {
                        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(Color.White))
                    }
                }
                is ProviderSwitchState.Failed -> {
                    Text(stringResource(R.string.pc_force_unavailable), color = Color(0xFFFF7A7A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.pc_force_failed, nameOf(state.providerId), ProviderSwitch.MaxTries),
                        color = InfoMuted,
                        fontSize = 12.sp,
                    )
                }
                is ProviderSwitchState.Done -> {
                    Text(stringResource(R.string.pc_force_now_playing, nameOf(state.providerId)), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                ProviderSwitchState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun ProviderChoiceRow(
    providerId: String,
    name: String,
    isCurrent: Boolean,
    busy: Boolean,
    blocked: String?,
    check: CatalogCheck?,
    onSearch: () -> Unit,
    options: List<StreamOption>,
    selectedOption: String?,
    /** null when the row itself was tapped, keeping the quality as it is */
    onPick: (String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(tween(260, easing = SheetEasing))
            .clip(RoundedCornerShape(18.dp))
            .background(MenuRowSurface)
            .clickable(
                enabled = !busy && blocked == null,
                interactionSource = remember { MutableInteractionSource() },
                indication = RemoteFocus,
            ) { onPick(null) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.graphicsLayer { alpha = if (blocked == null) 1f else 0.45f }) {
                ProviderLogo(providerId, name, 44.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, color = Color.White.copy(alpha = if (blocked == null) 1f else 0.55f), fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    text = blocked ?: stringResource(if (isCurrent) R.string.pc_force_playing_here else R.string.pc_force_tap_here),
                    color = InfoMuted,
                    fontSize = 12.sp,
                )
            }
            IconButton(
                onClick = onSearch,
                enabled = check != CatalogCheck.Looking,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.search_outline),
                    contentDescription = stringResource(R.string.pc_force_search, name),
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (check != null) {
            AnimatedContent(
                targetState = check,
                transitionSpec = {
                    (fadeIn(tween(240, easing = SheetEasing)) + expandVertically(tween(260, easing = SheetEasing))) togetherWith
                        fadeOut(tween(160))
                },
                label = "catalog",
            ) { result -> CatalogResult(result) }
        }
        if (blocked == null && options.size > 1) {
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                options.forEach { option ->
                    QualityChip(
                        label = option.label,
                        selected = option.id == selectedOption,
                        enabled = !busy,
                        onClick = { onPick(option.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(28.dp)
            .clip(shape)
            .then(if (selected) Modifier.background(Color.White) else Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), shape))
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = RemoteFocus, onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Text(text = label, color = if (selected) Color.Black else Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private val SheetEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

private val Figures = TextStyle(fontFeatureSettings = "tnum")

/** what the search beside a source found for this song in its catalogue */
private sealed interface CatalogCheck {
    data object Looking : CatalogCheck
    data class Found(val stream: SourceStream) : CatalogCheck
    data class Missing(val reason: String) : CatalogCheck
    data object YouTube : CatalogCheck
}

private suspend fun lookUp(
    id: String,
    module: SourceModule?,
    metadata: com.mochyy.mochymusic.models.MediaMetadata?,
    modules: SourceModules,
): CatalogCheck {
    if (id == SourceModules.YouTubeId) return CatalogCheck.YouTube
    if (module == null || metadata == null) return CatalogCheck.Missing("")
    val track = TrackQuery(
        title = metadata.title,
        artists = metadata.artists.map { it.name },
        album = metadata.album?.title,
        durationMs = metadata.duration.takeIf { it > 0 }?.times(1000L),
        videoId = metadata.id,
    )
    val result = withContext(Dispatchers.IO) {
        withTimeoutOrNull(15_000) { runCatching { module.resolve(track, modules.playbackPicks(id, module)) }.getOrNull() }
    }
    return when (result) {
        is ResolveResult.Found -> CatalogCheck.Found(result.stream)
        is ResolveResult.NotFound -> CatalogCheck.Missing(result.reason)
        is ResolveResult.Unavailable -> CatalogCheck.Missing(result.reason)
        null -> CatalogCheck.Missing("")
    }
}

@Composable
private fun CatalogResult(result: CatalogCheck) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        when (result) {
            CatalogCheck.Looking -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.pc_force_looking), color = InfoMuted, fontSize = 12.sp)
            }
            CatalogCheck.YouTube -> Text(stringResource(R.string.pc_force_youtube_catalog), color = InfoMuted, fontSize = 12.sp)
            is CatalogCheck.Missing -> {
                Text(stringResource(R.string.pc_force_not_found), color = Color(0xFFFF7A7A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (result.reason.isNotBlank()) Text(result.reason, color = InfoMuted, fontSize = 12.sp)
            }
            is CatalogCheck.Found -> {
                val stream = result.stream
                Text(
                    text = stringResource(R.string.pc_force_found, stream.matched.title, stream.matched.artist),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                val lossless = stream.codec?.uppercase() in setOf("FLAC", "ALAC")
                val facts = listOfNotNull(
                    stream.codec,
                    stream.sampleRateHz?.let { stringResource(R.string.pc_force_khz, it / 1000f) },
                    stream.qualityLabel.takeIf { it.isNotBlank() },
                    stream.format.takeIf { it != StreamFormat.Progressive }?.name?.uppercase(),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    if (lossless) Badge(stringResource(R.string.pc_force_lossless), filled = true)
                    facts.forEach { Badge(it, filled = false) }
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, filled: Boolean) {
    val shape = RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(24.dp)
            .clip(shape)
            .then(if (filled) Modifier.background(Color.White) else Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), shape))
            .padding(horizontal = 10.dp),
    ) {
        Text(text = text, style = Figures, color = if (filled) Color.Black else Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
