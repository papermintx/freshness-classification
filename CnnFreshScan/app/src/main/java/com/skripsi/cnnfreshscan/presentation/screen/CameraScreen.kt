package com.skripsi.cnnfreshscan.presentation.screen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.skripsi.cnnfreshscan.BuildConfig
import com.skripsi.cnnfreshscan.navigation.ABOUT_ROUTE
import com.skripsi.cnnfreshscan.navigation.buildResultRoute
import com.skripsi.cnnfreshscan.presentation.component.CameraPermissionScreen
import com.skripsi.cnnfreshscan.presentation.util.formatAccuracy
import com.skripsi.cnnfreshscan.presentation.util.mapCenterCropRoiToPreviewSide
import com.skripsi.cnnfreshscan.presentation.viewmodel.CameraBenchmarkStatus
import com.skripsi.cnnfreshscan.presentation.viewmodel.CameraBenchmarkUiState
import com.skripsi.cnnfreshscan.presentation.viewmodel.CameraBenchmarkViewModel
import com.skripsi.cnnfreshscan.presentation.viewmodel.CameraViewModel
import com.skripsi.cnnfreshscan.ui.theme.CameraConfidence
import com.skripsi.cnnfreshscan.ui.theme.CameraGlass
import com.skripsi.cnnfreshscan.ui.theme.CameraGlassSoft
import com.skripsi.cnnfreshscan.ui.theme.CameraOverlayTint
import com.skripsi.cnnfreshscan.ui.theme.CnnFreshScanTheme
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = hiltViewModel(),
    benchmarkViewModel: CameraBenchmarkViewModel = hiltViewModel(),
    navController: NavHostController,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val screenView = androidx.compose.ui.platform.LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val benchmarkUiState by benchmarkViewModel.uiState.collectAsState()
    val benchmarkCameraMetadata = remember(context, uiState.isFacingFront) {
        resolveCameraBenchmarkMetadata(context, uiState.isFacingFront)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val coroutineScope = rememberCoroutineScope()
    var showResearchBenchmarkSheet by remember { mutableStateOf(false) }
    var lastBenchmarkStatus by remember { mutableStateOf(benchmarkUiState.status) }
    val isBenchmarkInteractionLocked =
        BuildConfig.DEBUG &&
            (benchmarkUiState.status == CameraBenchmarkStatus.Countdown ||
                benchmarkUiState.status == CameraBenchmarkStatus.Running)

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            viewModel.setError("Izin kamera diperlukan untuk memindai objek.")
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val cacheFile = java.io.File(context.cacheDir, "picked_gallery_image.jpg")
                    cacheFile.outputStream().use { output ->
                        inputStream.use { input ->
                            input.copyTo(output)
                        }
                    }
                    val fileUri = Uri.fromFile(cacheFile).toString()
                    navController.navigate(buildResultRoute(fileUri)) {
                        launchSingleTop = true
                    }
                } else {
                    viewModel.setError("Gagal membuka gambar dari galeri.")
                }
            } catch (e: Exception) {
                android.util.Log.e("CameraScreen", "Error copying gallery image", e)
                viewModel.setError("Gagal memproses gambar galeri: ${e.localizedMessage}")
            }
        }
    }

    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS)
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            controller = cameraController
        }
    }

    DisposableEffect(cameraExecutor, cameraController) {
        cameraController.setImageAnalysisAnalyzer(cameraExecutor) { imageProxy ->
            viewModel.processImageProxy(imageProxy)
        }
        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(hasCameraPermission, lifecycleOwner) {
        if (hasCameraPermission) {
            cameraController.bindToLifecycle(lifecycleOwner)
        }
    }

    LaunchedEffect(uiState.isFacingFront) {
        cameraController.cameraSelector = if (uiState.isFacingFront) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    LaunchedEffect(uiState.isFlashEnabled, uiState.isFacingFront) {
        cameraController.enableTorch(uiState.isFlashEnabled && !uiState.isFacingFront)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            viewModel.setError(null)
        }
    }

    LaunchedEffect(benchmarkUiState.message) {
        benchmarkUiState.message?.let { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            benchmarkViewModel.consumeMessage()
        }
    }

    LaunchedEffect(benchmarkUiState.sessionId, benchmarkUiState.warmUpCompleted) {
        if (!BuildConfig.DEBUG || !benchmarkUiState.warmUpCompleted) return@LaunchedEffect

        var captureIndex = benchmarkUiState.currentCaptureIndex + 1
        while (benchmarkViewModel.shouldContinueCapture()) {
            val bitmap = viewModel.latestAnalyzedFrameCopy()
            if (bitmap == null) {
                delay(120L)
                continue
            }
            benchmarkViewModel.processCapturedBitmap(
                captureIndex = captureIndex,
                bitmap = bitmap,
                cameraLensFacing = benchmarkCameraMetadata.lensFacing,
                cameraFpsRange = benchmarkCameraMetadata.fpsRange,
                cameraMaxFps = benchmarkCameraMetadata.maxFps,
                cameraMegapixels = benchmarkCameraMetadata.megapixels,
                cameraResolution = benchmarkCameraMetadata.resolution
            )

            captureIndex += 1
        }
    }

    LaunchedEffect(benchmarkUiState.status) {
        val previousStatus = lastBenchmarkStatus
        if (
            BuildConfig.DEBUG &&
            previousStatus == CameraBenchmarkStatus.Running &&
            (benchmarkUiState.status == CameraBenchmarkStatus.Completed ||
             benchmarkUiState.status == CameraBenchmarkStatus.Error)
        ) {
            showResearchBenchmarkSheet = true
        }
        lastBenchmarkStatus = benchmarkUiState.status
    }

    DisposableEffect(screenView) {
        val previousKeepScreenOn = screenView.keepScreenOn
        val window = context.findActivity()?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, screenView) }
        val previousLightStatusBars = insetsController?.isAppearanceLightStatusBars
        val previousLightNavigationBars = insetsController?.isAppearanceLightNavigationBars

        screenView.keepScreenOn = true
        insetsController?.isAppearanceLightStatusBars = false
        insetsController?.isAppearanceLightNavigationBars = false

        onDispose {
            screenView.keepScreenOn = previousKeepScreenOn
            if (previousLightStatusBars != null) {
                insetsController.isAppearanceLightStatusBars = previousLightStatusBars
            }
            if (previousLightNavigationBars != null) {
                insetsController.isAppearanceLightNavigationBars = previousLightNavigationBars
            }
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionScreen(
            onGrantPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            isDenied = true
        )
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .testTag("camera_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { previewView },
                update = { it.controller = cameraController }
            )

            // Background Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.22f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.10f),
                                Color.Black.copy(alpha = 0.28f)
                            )
                        )
                    )
            )

            // Scanner Overlay (ROI dengan blur di area luar)
            ScannerOverlay(
                modifier = Modifier.fillMaxSize(),
                analysisFrameWidth = uiState.analysisFrameWidth,
                analysisFrameHeight = uiState.analysisFrameHeight,
                roiSizeFraction = uiState.roiSizeFraction,
                roiVerticalBias = uiState.roiVerticalBias
            )

            // Top Bar
            CameraTopBar(
                showDebugButton = BuildConfig.DEBUG,
                enabled = !isBenchmarkInteractionLocked,
                onInfoClick = {
                    navController.navigate(ABOUT_ROUTE) {
                        launchSingleTop = true
                    }
                },
                onDebugClick = { showResearchBenchmarkSheet = true },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            )

            // Center Section: Text Arahan + ROI + Live Prediction (dalam satu kolom)
            CenterScannerSection(
                roiSizeFraction = uiState.roiSizeFraction,
                roiVerticalBias = uiState.roiVerticalBias,
                label = uiState.livePredictionLabel,
                confidence = uiState.livePredictionConfidence,
                predictionTimeMs = uiState.livePredictionTimeMs,
                modifier = Modifier.fillMaxSize()
            )

            if (
                BuildConfig.DEBUG &&
                benchmarkUiState.status == CameraBenchmarkStatus.Countdown &&
                benchmarkUiState.countdownValue > 0
            ) {
                BenchmarkCountdownOverlay(
                    countdownValue = benchmarkUiState.countdownValue,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Bottom Bar
            CameraBottomBar(
                isCapturing = uiState.isCapturing,
                enabled = !isBenchmarkInteractionLocked,
                onGalleryClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onCaptureClick = {
                    if (uiState.isCapturing) return@CameraBottomBar

                    val latestFrame = viewModel.latestAnalyzedFrameCopy()
                    if (latestFrame != null) {
                        viewModel.onCaptureStarted()
                        coroutineScope.launch {
                            try {
                                val photoFile = withContext(Dispatchers.IO) {
                                    saveBitmapToCache(context, latestFrame)
                                }
                                viewModel.onCaptureFinished()
                                navController.navigate(buildResultRoute(Uri.fromFile(photoFile).toString())) {
                                    launchSingleTop = true
                                }
                            } catch (exception: Exception) {
                                viewModel.onCaptureFinished()
                                viewModel.setError(
                                    exception.localizedMessage ?: "Gagal mengambil frame kamera."
                                )
                            }
                        }
                        return@CameraBottomBar
                    }

                    val photoFile = File.createTempFile(
                        "cnn_fresh_capture_",
                        ".jpg",
                        context.cacheDir
                    )

                    viewModel.onCaptureStarted()
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    cameraController.takePicture(
                        outputOptions,
                        mainExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                viewModel.onCaptureFinished()
                                navController.navigate(buildResultRoute(Uri.fromFile(photoFile).toString())) {
                                    launchSingleTop = true
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                photoFile.delete()
                                viewModel.onCaptureFinished()
                                viewModel.setError(
                                    exception.localizedMessage ?: "Gagal mengambil foto."
                                )
                            }
                        }
                    )
                },
                onSwitchCameraClick = { viewModel.setFacingFront(!uiState.isFacingFront) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 34.dp, vertical = 22.dp)
            )
        }
    }

    if (BuildConfig.DEBUG && showResearchBenchmarkSheet) {
        ResearchBenchmarkSheet(
            uiState = benchmarkUiState,
            onDismiss = { showResearchBenchmarkSheet = false },
            onLabelSelected = benchmarkViewModel::setActualLabel,
            onDistanceSelected = benchmarkViewModel::setDistanceCm,
            onCaptureCountSelected = benchmarkViewModel::setTargetCaptures,
            onStart = {
                showResearchBenchmarkSheet = false
                benchmarkViewModel.startBatchTest()
            },
            onStop = benchmarkViewModel::stopBatchTest,
            onExport = benchmarkViewModel::exportReport,
            onClear = benchmarkViewModel::clear
        )
    }
}

@Composable
private fun CameraTopBar(
    onInfoClick: () -> Unit,
    onDebugClick: () -> Unit = {},
    showDebugButton: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showDebugButton) {
            GlassIconButton(
                onClick = { if (enabled) onDebugClick() },
                enabled = enabled,
                icon = {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Buka fitur pengujian",
                        tint = Color.White
                    )
                }
            )
        }
        GlassIconButton(
            onClick = { if (enabled) onInfoClick() },
            enabled = enabled,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Tentang aplikasi",
                    tint = Color.White
                )
            }
        )
    }
}

@Composable
private fun BenchmarkCountdownOverlay(
    countdownValue: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CameraGlass)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = countdownValue.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Arahkan objek ke ROI",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun CenterScannerSection(
    modifier: Modifier = Modifier,
    instructionText: String = "Arahkan Buah/Sayur di Dalam Area Pindai",
    roiSizeFraction: Float = 0.8f,
    roiVerticalBias: Float = -0.14f,
    label: String,
    confidence: Float,
    predictionTimeMs: Long?,
) {
    val safeRoiFraction = roiSizeFraction.coerceIn(0.1f, 1f)
    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
        Text(
            text = instructionText,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )

        Box(
            modifier = Modifier.aspectRatio(1f)
        )

        LivePredictionCard(
            label = label,
            confidence = confidence,
            predictionTimeMs = predictionTimeMs,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
        }
    ) { measurables, constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val roiSidePx = (minOf(layoutWidth, layoutHeight) * safeRoiFraction)
            .roundToInt()
            .coerceAtLeast(1)
        val maxRoiTop = (layoutHeight - roiSidePx).coerceAtLeast(0)
        val centeredTop = (maxRoiTop / 2f).roundToInt().toFloat()
        val maxShift = maxRoiTop / 2f
        val roiTop = (centeredTop + maxShift * roiVerticalBias.coerceIn(-0.45f, 0.45f))
            .roundToInt()
            .coerceIn(0, maxRoiTop)
        val gap = 12.dp.roundToPx()
        val minimumTextTop = 86.dp.roundToPx()

        val textPlaceable = measurables[0].measure(
            constraints.copy(minWidth = 0, minHeight = 0)
        )
        val roiPlaceable = measurables[1].measure(
            Constraints.fixed(roiSidePx, roiSidePx)
        )
        val cardPlaceable = measurables[2].measure(
            constraints.copy(minWidth = 0, minHeight = 0)
        )

        val textY = (roiTop - textPlaceable.height - gap).coerceAtLeast(minimumTextTop)
        val roiX = (layoutWidth - roiSidePx) / 2
        val cardY = roiTop + roiSidePx + gap

        layout(layoutWidth, layoutHeight) {
            textPlaceable.placeRelative(0, textY)
            roiPlaceable.placeRelative(roiX, roiTop)
            cardPlaceable.placeRelative(0, cardY)
        }
    }

}

@Composable
private fun CameraBottomBar(
    isCapturing: Boolean,
    enabled: Boolean = true,
    onGalleryClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onSwitchCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(
            onClick = { if (enabled) onGalleryClick() },
            enabled = enabled,
            icon = {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Pilih dari galeri",
                    tint = Color.White
                )
            }
        )

        CaptureButton(
            isCapturing = isCapturing,
            enabled = enabled,
            onClick = onCaptureClick
        )

        GlassIconButton(
            onClick = { if (enabled) onSwitchCameraClick() },
            enabled = enabled,
            icon = {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Ganti kamera",
                    tint = Color.White
                )
            }
        )
    }
}

@Composable
private fun CaptureButton(
    isCapturing: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled && !isCapturing,
        modifier = Modifier.size(94.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(4.dp, Color.White.copy(alpha = 0.95f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color(0xFF6B767E), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else {
//                    Box(
//                        modifier = Modifier
//                            .size(26.dp)
//                            .background(Color.White, CircleShape)
//                    )
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Ambil Gambar",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassIconButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.size(54.dp),
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = CameraGlassSoft)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}

@Composable
private fun LivePredictionCard(
    label: String,
    confidence: Float,
    predictionTimeMs: Long?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CameraGlass)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = formatAccuracy(confidence),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = CameraConfidence
            )
            predictionTimeMs?.let {
                Text(
                    text = "Inferensi $it ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
private fun ScannerOverlay(
    modifier: Modifier = Modifier,
    analysisFrameWidth: Int = 0,
    analysisFrameHeight: Int = 0,
    roiSizeFraction: Float = 0.8f,
    roiVerticalBias: Float = -0.14f
) {
    Box(
        modifier = modifier
            .testTag("roi_box")
            .semantics { contentDescription = "Area ROI" }
            .graphicsLayer(alpha = 0.99f)
            .drawWithContent {
                drawContent()

                if (analysisFrameWidth <= 0 || analysisFrameHeight <= 0) {
                    return@drawWithContent
                }

                val boxSide = mapCenterCropRoiToPreviewSide(
                    viewWidth = size.width,
                    viewHeight = size.height,
                    imageWidth = analysisFrameWidth,
                    imageHeight = analysisFrameHeight,
                    sizeFraction = roiSizeFraction
                )

                val left = (size.width - boxSide) / 2f
                val centeredTop = ((size.height - boxSide) / 2f).roundToInt().toFloat()
                val maxShift = (size.height - boxSide) / 2f
                val shift = roiVerticalBias.coerceIn(-0.45f, 0.45f) * maxShift
                val top = (centeredTop + shift).coerceIn(0f, size.height - boxSide)
                val right = left + boxSide
                val bottom = top + boxSide
                val cornerRadius = 16.dp.toPx()  // Rounded corners seperti di gambar
                val strokeWidth = 5.dp.toPx()
                val frameColor = Color.White
                val bracketLength = 30.dp.toPx()

                drawRect(color = CameraOverlayTint.copy(alpha = 0.48f))
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = Size(boxSide, boxSide),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    blendMode = BlendMode.Clear
                )

                drawCornerBrackets(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    bracketLength = bracketLength,
                    strokeWidth = strokeWidth,
                    color = frameColor
                )
            }
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCornerBrackets(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    bracketLength: Float,
    strokeWidth: Float,
    color: Color
) {
    val inset = strokeWidth / 2f
    val startX = left + inset
    val endX = right - inset
    val startY = top + inset
    val endY = bottom - inset
    val bracket = bracketLength.coerceAtMost(minOf((endX - startX) / 2f, (endY - startY) / 2f))
    val stroke = Stroke(
        width = strokeWidth,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )

    val radius = (bracket * 0.42f).coerceAtLeast(strokeWidth * 2f)

    drawLine(
        color = color,
        start = Offset(startX, startY + bracket),
        end = Offset(startX, startY + radius),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(startX, startY),
        size = Size(radius * 2f, radius * 2f),
        style = stroke
    )
    drawLine(
        color = color,
        start = Offset(startX + radius, startY),
        end = Offset(startX + bracket, startY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    drawLine(
        color = color,
        start = Offset(endX - bracket, startY),
        end = Offset(endX - radius, startY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawArc(
        color = color,
        startAngle = 270f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(endX - radius * 2f, startY),
        size = Size(radius * 2f, radius * 2f),
        style = stroke
    )
    drawLine(
        color = color,
        start = Offset(endX, startY + radius),
        end = Offset(endX, startY + bracket),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    drawLine(
        color = color,
        start = Offset(startX, endY - bracket),
        end = Offset(startX, endY - radius),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawArc(
        color = color,
        startAngle = 90f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(startX, endY - radius * 2f),
        size = Size(radius * 2f, radius * 2f),
        style = stroke
    )
    drawLine(
        color = color,
        start = Offset(startX + radius, endY),
        end = Offset(startX + bracket, endY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    drawLine(
        color = color,
        start = Offset(endX - bracket, endY),
        end = Offset(endX - radius, endY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawArc(
        color = color,
        startAngle = 0f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(endX - radius * 2f, endY - radius * 2f),
        size = Size(radius * 2f, radius * 2f),
        style = stroke
    )
    drawLine(
        color = color,
        start = Offset(endX, endY - radius),
        end = Offset(endX, endY - bracket),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

private data class CameraBenchmarkMetadata(
    val lensFacing: String = "Tidak diketahui",
    val fpsRange: String = "Tidak diketahui",
    val maxFps: Int = 0,
    val megapixels: Double = 0.0,
    val resolution: String = "Tidak diketahui"
)

private fun resolveCameraBenchmarkMetadata(
    context: Context,
    isFacingFront: Boolean
): CameraBenchmarkMetadata {
    return runCatching {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val targetLens = if (isFacingFront) {
            CameraCharacteristics.LENS_FACING_FRONT
        } else {
            CameraCharacteristics.LENS_FACING_BACK
        }
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == targetLens
        } ?: cameraManager.cameraIdList.firstOrNull()
        if (cameraId == null) return CameraBenchmarkMetadata()

        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val lensFacing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
            CameraCharacteristics.LENS_FACING_FRONT -> "Depan"
            CameraCharacteristics.LENS_FACING_BACK -> "Belakang"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "Eksternal"
            else -> "Tidak diketahui"
        }
        val fpsRanges = characteristics.get(
            CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
        ).orEmpty()
        val maxFps = fpsRanges.maxOfOrNull { it.upper } ?: 0
        val fpsRange = fpsRanges
            .joinToString(separator = "|") { "${it.lower}-${it.upper}" }
            .ifBlank { "Tidak diketahui" }
        val pixelArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val resolution = pixelArraySize?.let { "${it.width}x${it.height}" } ?: "Tidak diketahui"
        val megapixels = pixelArraySize?.let { size ->
            (size.width.toDouble() * size.height.toDouble()) / 1_000_000.0
        } ?: 0.0

        CameraBenchmarkMetadata(
            lensFacing = lensFacing,
            fpsRange = fpsRange,
            maxFps = maxFps,
            megapixels = megapixels,
            resolution = resolution
        )
    }.getOrDefault(CameraBenchmarkMetadata())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResearchBenchmarkSheet(
    uiState: CameraBenchmarkUiState,
    onDismiss: () -> Unit,
    onLabelSelected: (String) -> Unit,
    onDistanceSelected: (Int?) -> Unit,
    onCaptureCountSelected: (Int) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit
) {
    var labelExpanded by remember { mutableStateOf(false) }
    val sheetScrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(Alignment.Bottom)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .verticalScroll(sheetScrollState)
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Pengujian ROI Kamera",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF102019)
            )
            Text(
                text = "Menggunakan kamera dan ROI yang sama dengan halaman utama.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF24352D)
            )

            Text(
                text = "Label aktual",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF102019)
            )
            Box {
                Card(
                    onClick = { labelExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF2EE))
                ) {
                    Text(
                        text = CameraBenchmarkViewModel.displayNameForLabel(uiState.actualLabel),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF102019),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }
                DropdownMenu(
                    expanded = labelExpanded,
                    onDismissRequest = { labelExpanded = false }
                ) {
                    CameraBenchmarkViewModel.LABELS.forEach { label ->
                        DropdownMenuItem(
                            text = { Text(CameraBenchmarkViewModel.displayNameForLabel(label)) },
                            onClick = {
                                onLabelSelected(label)
                                labelExpanded = false
                            }
                        )
                    }
                }
            }

            Text(
                text = "Mode jarak pengambilan gambar",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF102019)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = uiState.distanceCm == null,
                    onClick = { onDistanceSelected(null) },
                    label = { Text("Bebas") }
                )
                CameraBenchmarkViewModel.DISTANCE_OPTIONS_CM.forEach { distance ->
                    FilterChip(
                        selected = uiState.distanceCm == distance,
                        onClick = { onDistanceSelected(distance) },
                        label = { Text("$distance cm") }
                    )
                }
            }

            Text(
                text = "Jumlah pengambilan gambar",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF102019)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CameraBenchmarkViewModel.CAPTURE_OPTIONS.forEach { count ->
                    FilterChip(
                        selected = uiState.targetCaptures == count,
                        onClick = { onCaptureCountSelected(count) },
                        label = { Text(count.toString()) }
                    )
                }
            }

            uiState.summary?.let { summary ->
                Text(
                    text = "Progress ${uiState.currentCaptureIndex}/${uiState.targetCaptures} | Akurasi ${formatAccuracy(summary.accuracy.toFloat())} | Rata-rata ${"%.1f".format(summary.meanInferenceMs)} ms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF102019)
                )
            } ?: Text(
                text = "Progress ${uiState.currentCaptureIndex}/${uiState.targetCaptures}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5B6A63)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStart,
                    enabled = uiState.status != CameraBenchmarkStatus.Running,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Mulai")
                }
                OutlinedButton(
                    onClick = onStop,
                    enabled = uiState.status == CameraBenchmarkStatus.Running,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Berhenti")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onExport,
                    enabled = uiState.results.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ekspor")
                }
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Bersihkan")
                }
            }
        }
    }
}

@Preview(name = "Camera Top Bar", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CameraTopBarPreview() {
    CnnFreshScanTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            CameraTopBar(
                showDebugButton = true,
                onInfoClick = {},
                onDebugClick = {}
            )
        }
    }
}

@Preview(name = "Camera Bottom Bar", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CameraBottomBarPreview() {
    CnnFreshScanTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 34.dp, vertical = 22.dp)
        ) {
            CameraBottomBar(
                isCapturing = false,
                onGalleryClick = {},
                onCaptureClick = {},
                onSwitchCameraClick = {}
            )
        }
    }
}


@Preview(name = "Live Prediction Card", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun LivePredictionCardPreview() {
    CnnFreshScanTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            LivePredictionCard(
                label = "Tomat Segar",
                confidence = 0.93f,
                predictionTimeMs = 128L
            )
        }
    }
}

@Preview(
    name = "Center Scanner Section",
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 360,
    heightDp = 760
)
@Composable
private fun CenterScannerSectionPreview() {
    CnnFreshScanTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            CenterScannerSection(
                instructionText = "Arahkan Buah/Sayur di Dalam Area Pindai",
                roiSizeFraction = 0.75f,
                label = "Tomat Segar",
                confidence = 0.93f,
                predictionTimeMs = 128L,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Preview(
    name = "Scanner Overlay",
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 360,
    heightDp = 760
)
@Composable
private fun ScannerOverlayPreview() {
    CnnFreshScanTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.22f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.10f),
                            Color.Black.copy(alpha = 0.28f)
                        )
                    )
                )
        ) {
            ScannerOverlay(
                modifier = Modifier.fillMaxSize(),
                analysisFrameWidth = 1280,
                analysisFrameHeight = 720,
                roiSizeFraction = 0.75f,
                roiVerticalBias = -0.22f
            )
        }
    }
}

private fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun saveBitmapToCache(context: Context, bitmap: Bitmap): File {
    val photoFile = File.createTempFile(
        "cnn_fresh_realtime_frame_",
        ".jpg",
        context.cacheDir
    )
    photoFile.outputStream().use { output ->
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) {
            throw IllegalStateException("Gagal menyimpan frame kamera.")
        }
    }
    return photoFile
}
