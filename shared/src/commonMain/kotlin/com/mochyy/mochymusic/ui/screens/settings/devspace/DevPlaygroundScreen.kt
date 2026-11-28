// the developer playground page: what the repo did since this build, its issues, and the experimental settings

package com.mochyy.mochymusic.ui.screens.settings.devspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.dev_commits
import com.mochyy.mochymusic.shared.resources.dev_in_total
import com.mochyy.mochymusic.shared.resources.dev_new_reported_issues
import com.mochyy.mochymusic.shared.resources.dev_new_resolved_issue
import com.mochyy.mochymusic.shared.resources.dev_playground_title
import com.mochyy.mochymusic.shared.resources.dev_releases
import com.mochyy.mochymusic.shared.resources.dev_reported_issues
import com.mochyy.mochymusic.shared.resources.dev_resolved_issue
import com.mochyy.mochymusic.shared.resources.dev_since_build_subtitle
import com.mochyy.mochymusic.shared.resources.dev_since_this_build
import com.mochyy.mochymusic.shared.resources.dev_spaces_back_label
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

const val DevPlaygroundRoute = "developer_playground"

@Composable
fun DevPlaygroundScreen(
    navController: NavController,
    /**
     * The host's row into its own experimental settings page, which only :app has (CategoryBubble +
     * the route). It sits at the bottom of the page, where that row always was.
     */
    experimentalSettingsEntry: @Composable () -> Unit,
    devSpace: DevSpaceRepository = koinInject(),
) {
    val stats by devSpace.current.collectAsState()
    val loading by devSpace.loading.collectAsState()
    val error by devSpace.error.collectAsState()
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) { devSpace.refresh(force = reload > 0) }
    val settled = stats != null || !loading

    SubSettingsScaffold(
        title = stringResource(Res.string.dev_playground_title),
        backLabel = stringResource(Res.string.dev_spaces_back_label),
        onBack = { navController.navigateUp() },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RepoFiguresCard(
                title = stringResource(Res.string.dev_since_this_build),
                subtitle = stringResource(Res.string.dev_since_build_subtitle),
                figures =
                    listOf(
                        stats?.newIssues to stringResource(Res.string.dev_new_reported_issues),
                        stats?.newResolved to stringResource(Res.string.dev_new_resolved_issue),
                        stats?.commits to stringResource(Res.string.dev_commits),
                        stats?.releases to stringResource(Res.string.dev_releases),
                    ),
                settled = settled,
            )
            RepoFiguresCard(
                title = stringResource(Res.string.dev_in_total),
                figures =
                    listOf(
                        stats?.reportedTotal to stringResource(Res.string.dev_reported_issues),
                        stats?.closedTotal to stringResource(Res.string.dev_resolved_issue),
                    ),
                settled = settled,
            )
            RepoIssuesSection(stats = stats, loading = loading, error = error, onRetry = { reload++ })
            Spacer(Modifier.height(6.dp))
            experimentalSettingsEntry()
        }
    }
}
