package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.ui.component.GlassState
import com.mochyy.mochymusic.ui.component.glassRoot
import com.mochyy.mochymusic.ui.screens.search.SearchAvatar
import com.mochyy.mochymusic.ui.screens.search.SearchField
import com.mochyy.mochymusic.ui.screens.search.SearchGlassTopBar
import com.mochyy.mochymusic.ui.screens.search.searchTopBarHeight

/**
 * The settings list header as Android has always drawn it: GlassKit's frosted backdrop, the Mo chy
 * mark and title folding into the bar, and the search field gliding up beside the profile avatar.
 * The list itself is shared; only this chrome and the rows it opens stay the app's.
 */
object AppMochySettingsChrome : MochySettingsChrome {
    @Composable
    override fun topBarHeight(titleBlockHeight: Dp): Dp = searchTopBarHeight(titleBlockHeight = titleBlockHeight)

    @Composable
    override fun rememberGlass(): SettingsListGlass = remember { AppSettingsListGlass(GlassState()) }

    override fun backdrop(glass: SettingsListGlass, isActive: () -> Boolean): Modifier =
        Modifier.glassRoot((glass as AppSettingsListGlass).state, isActive)

    @Composable
    override fun TopBar(
        glass: SettingsListGlass,
        collapseProgress: () -> Float,
        title: String,
        titleBlockHeight: Dp,
        titleContent: @Composable () -> Unit,
        profilePicUri: String?,
        onAvatarClick: () -> Unit,
        query: TextFieldValue,
        onQueryChange: (TextFieldValue) -> Unit,
        isSearchFocused: Boolean,
        onFocusChanged: (Boolean) -> Unit,
    ) {
        val state = (glass as AppSettingsListGlass).state
        SearchGlassTopBar(
            glassState = state,
            progressProvider = collapseProgress,
            pureBlack = true,
            title = title,
            titleBlockHeight = titleBlockHeight,
            titleContent = titleContent,
            trailing = {
                // at rest it sits beside the mark like the design, and drops to the search field's middle as the header folds
                SearchAvatar(
                    modifier = Modifier.graphicsLayer {
                        val lift = (titleBlockHeight / 2 - 16.dp).toPx()
                        translationY = -(1f - collapseProgress()) * lift
                    },
                    imageUrl = profilePicUri?.ifBlank { null },
                    onClick = onAvatarClick,
                )
            },
        ) {
            SearchField(
                value = query,
                onValueChange = onQueryChange,
                onSearch = {},
                placeholder = stringResource(R.string.settings_search_placeholder),
                focused = isSearchFocused,
                onFocusChanged = onFocusChanged,
                trailing = if (query.text.isNotEmpty()) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.settings_clear_search),
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { onQueryChange(TextFieldValue()) },
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

private class AppSettingsListGlass(val state: GlassState) : SettingsListGlass
