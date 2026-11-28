package com.mochyy.mochymusic.shared.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mochyy.mochymusic.shared.media.SearchSource
import com.music.innertube.models.SongItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

// The search tab's query, results and in-flight request. The app shell builds it above the adaptive
// layout, so a tab switch or a resize across the rail breakpoint keeps what was typed and found.
internal sealed interface SearchUi {
    data object Idle : SearchUi
    data object Loading : SearchUi
    data class Results(val songs: List<SongItem>) : SearchUi
    data object Failed : SearchUi
}

/**
 * [scope] is the shell's scope, not the tab's: a search keeps running while another tab is open,
 * and leaving the search tab doesn't cancel it.
 */
internal class SearchState(
    private val source: SearchSource,
    private val scope: CoroutineScope,
) {
    var query by mutableStateOf("")
    internal var ui by mutableStateOf<SearchUi>(SearchUi.Idle)
    private var job: Job? = null

    // a search runs on the explicit action (Enter), not on every keystroke
    fun submit() {
        val q = query.trim()
        if (q.isEmpty()) return
        job?.cancel()
        ui = SearchUi.Loading
        job = scope.launch {
            val result = source.searchSongs(q)
            ensureActive()
            ui = result.fold(
                onSuccess = { SearchUi.Results(it) },
                // a cancelled search unwinds instead of showing "couldn't search"
                onFailure = { if (it is CancellationException) throw it else SearchUi.Failed },
            )
        }
    }
}
