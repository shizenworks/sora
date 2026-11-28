// TransferViewModel.kt

package com.mochyy.mochymusic.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** exporting everything to one zip and bringing a zip back in, for onboarding and the storage page */
class TransferViewModel(
    private val platform: TransferPlatform,
) : ViewModel() {
    private val _export = MutableStateFlow<TransferStep>(TransferStep.Idle)
    val export: StateFlow<TransferStep> = _export.asStateFlow()

    private val _import = MutableStateFlow<TransferStep>(TransferStep.Idle)
    val import: StateFlow<TransferStep> = _import.asStateFlow()

    private val _manifest = MutableStateFlow<TransferManifestInfo?>(null)

    /** what the zip being imported holds, for the progress page to show */
    val manifest: StateFlow<TransferManifestInfo?> = _manifest.asStateFlow()

    /** the permission that lets mochy find local songs again */
    val audioPermission: String get() = platform.audioPermission

    /** the name the export suggests, so it's easy to spot in downloads later */
    fun exportFileName(): String = platform.exportFileName()

    fun exportTo(target: String) {
        if (_export.value is TransferStep.Working) return
        _export.value = TransferStep.Working(0f)
        viewModelScope.launch {
            platform.export(target) { _export.value = TransferStep.Working(it) }
                .onSuccess { _export.value = TransferStep.Done }
                .onFailure { _export.value = TransferStep.Failed(it.message ?: "Couldn't export. Try again.") }
        }
    }

    /** checks a picked zip before anything is unpacked, the failure message is ready to show */
    suspend fun check(source: String): Result<TransferManifestInfo> =
        platform.peek(source).onSuccess { _manifest.value = it }

    /** true when the zip has local songs and mochy can't see the phone's music yet, so it's worth asking first */
    fun wantsAudioPermission(manifest: TransferManifestInfo): Boolean = platform.wantsAudioPermission(manifest)

    /** unpacks the zip, then restarts the app so the next start swaps it in */
    fun importFrom(source: String) {
        if (_import.value is TransferStep.Working) return
        _import.value = TransferStep.Working(0f)
        viewModelScope.launch {
            platform.stage(source) { _import.value = TransferStep.Working(it) }
                .onSuccess {
                    _import.value = TransferStep.Done
                    // a beat on the finished page, so the restart doesn't look like a crash
                    delay(1_200)
                    platform.restartToFinish()
                }
                .onFailure { _import.value = TransferStep.Failed(it.message ?: "Couldn't import that file. Try again.") }
        }
    }

    /** back to the start after a failed import, so the page can be tried again */
    fun clearImport() {
        if (_import.value !is TransferStep.Working) _import.value = TransferStep.Idle
    }
}
