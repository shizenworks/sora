package com.mochyy.mochymusic.ui.screens.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mochyy.mochymusic.ui.component.HomeContentInset
import com.mochyy.mochymusic.ui.screens.setup.onboarding.idCardName
import com.mochyy.mochymusic.viewmodels.UserProfileViewModel

@Composable
fun ProfileMorphHost(
    state: ProfileMorphState,
    username: String,
    photo: Any?,
    cardNumber: String,
    navController: NavController,
    viewModel: UserProfileViewModel,
) {
    LaunchedEffect(viewModel) { viewModel.keepWarm() }
    // only showing up and going away recompose this; the steps in between are read while drawing
    val active by remember(state) { derivedStateOf { state.isActive } }
    if (!active) return

    ProfileBackHandler(state)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onPlaced { state.target.root = it }
            // the overlay takes every touch, so nothing reaches the Mochy page underneath
            .pointerInput(Unit) {},
    ) {
        val name = idCardName(username)
        val nameMaxWidth = maxWidth - HomeContentInset * 2
        val nameStyle = rememberProfileNameStyle(name, nameMaxWidth)
        ProfileBackdrop(state)
        UserProfilePage(state, username, photo, cardNumber, nameStyle, navController, viewModel)
        FlyingParts(state, name, photo, cardNumber, nameStyle, nameMaxWidth)
    }
}

// on its own so the phase it reads doesn't recompose the whole page
@Composable
private fun ProfileBackHandler(state: ProfileMorphState) {
    BackHandler(enabled = state.phase == ProfilePhase.Opening || state.phase == ProfilePhase.Open) { state.close() }
}

/** The big name: 60sp, smaller when it wouldn't fit on one line, never below 34sp (then it ends in "…"). */
@Composable
private fun rememberProfileNameStyle(name: String, maxWidth: Dp): TextStyle {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(name, maxWidth, density) {
        val maxPx = with(density) { maxWidth.roundToPx() }
        val width = measurer.measure(name, ProfileNameStyle, maxLines = 1, softWrap = false).size.width
        if (width <= maxPx) {
            ProfileNameStyle
        } else {
            val fitted = ProfileNameStyle.fontSize.value * maxPx / width * 0.98f
            ProfileNameStyle.copy(fontSize = fitted.coerceAtLeast(MinNameSp).sp)
        }
    }
}

private const val MinNameSp = 34f
