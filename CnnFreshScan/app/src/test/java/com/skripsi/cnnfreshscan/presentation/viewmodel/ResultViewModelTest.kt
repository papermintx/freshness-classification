package com.skripsi.cnnfreshscan.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.skripsi.cnnfreshscan.navigation.IMAGE_URI_ARG
import com.skripsi.cnnfreshscan.util.MainDispatcherRule
import com.skripsi.core.domain.usecase.AnalyzeProduceUseCase
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ResultViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val analyzeProduceUseCase: AnalyzeProduceUseCase = mockk(relaxed = true)
    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    @Test
    fun `tanpa uri gambar menampilkan snackbar input tidak valid dan tidak loading`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf(IMAGE_URI_ARG to null))
        val viewModel = ResultViewModel(analyzeProduceUseCase, savedStateHandle, context)

        viewModel.uiState.test {
            val state = awaitItem()

            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals("Input tidak valid. Silakan pilih gambar lain.", state.snackbarMessage)
            assertEquals("Menunggu Hasil", state.displayLabel)
            assertEquals(0f, state.confidence, 0.001f)
        }
    }

    @Test
    fun `consume snackbar aman dipanggil meskipun belum ada pesan`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf(IMAGE_URI_ARG to null))
        val viewModel = ResultViewModel(analyzeProduceUseCase, savedStateHandle, context)

        viewModel.consumeSnackbar()

        viewModel.uiState.test {
            val state = awaitItem()

            assertNull(state.snackbarMessage)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `gambar rusak menampilkan snackbar input tidak valid dan aplikasi tidak crash`() = runTest {
        val corruptImage = createCorruptImageFile()
        val savedStateHandle = SavedStateHandle(
            mapOf(IMAGE_URI_ARG to Uri.fromFile(corruptImage).toString())
        )

        val viewModel = ResultViewModel(analyzeProduceUseCase, savedStateHandle, context)

        viewModel.uiState.test {
            var state = awaitItem()
            repeat(4) {
                if (state.isLoading) {
                    state = awaitItem()
                }
            }

            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals("Input tidak valid. Silakan pilih gambar lain.", state.snackbarMessage)
            assertEquals("Menunggu Hasil", state.displayLabel)
            assertEquals(0f, state.confidence, 0.001f)
            assertNull(state.imageBitmap)
        }
    }

    private fun createCorruptImageFile(): File {
        return temporaryFolder.newFile("gambar_rusak.jpg").apply {
            writeBytes("ini bukan data jpeg yang valid".toByteArray())
        }
    }
}
