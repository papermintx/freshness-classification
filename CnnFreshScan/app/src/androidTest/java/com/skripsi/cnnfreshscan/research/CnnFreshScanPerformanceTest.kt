package com.skripsi.cnnfreshscan.research

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.os.Debug
import android.provider.MediaStore
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import dagger.hilt.EntryPoints
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

import com.skripsi.core.di.PerformanceTestEntryPoint

@RunWith(AndroidJUnit4::class)
class CnnFreshScanPerformanceTest {

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private lateinit var device: UiDevice
    private lateinit var context: Context
    private val packageName = "com.skripsi.cnnfreshscan"

    // Spesifikasi Perangkat
    private val deviceModel = android.os.Build.MODEL
    private val deviceBrand = android.os.Build.MANUFACTURER
    private val androidVersion = android.os.Build.VERSION.RELEASE

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun runFullAutomatedPerformanceTest() {
        val results = StringBuilder()
        results.append("====================================================\n")
        results.append("      LAPORAN LENGKAP PENGUJIAN RAM & RESPONSE TIME\n")
        results.append("====================================================\n")
        results.append("Tanggal Pengujian : ${SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        results.append("Merk / Model HP   : $deviceBrand $deviceModel\n")
        results.append("Versi Android     : Android $androidVersion (API ${android.os.Build.VERSION.SDK_INT})\n")
        results.append("====================================================\n\n")

        // 1. PENGUJIAN STARTUP APLIKASI
        Log.i("PERF_TEST", "[1/7] Menguji Waktu Startup Aplikasi...")
        device.pressHome()
        val appStartupStart = System.currentTimeMillis()

        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: error("Aplikasi tidak ditemukan: $packageName")
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)

        // Tunggu hingga camera screen terbuka
        val mainScreenLoaded = device.wait(Until.hasObject(By.desc("Tentang aplikasi")), 15_000)
        val appStartupTime = System.currentTimeMillis() - appStartupStart
        assertNotNull("Gagal memuat Halaman Utama (Camera Screen) aplikasi", mainScreenLoaded)
        Thread.sleep(1500L) // Stabilitas awal

        val mainJvm = getUsedJvmHeapMemory()
        val mainNative = getUsedNativeHeapMemory()
        val mainPss = getAppTotalPssMemory()

        results.append("A. WAKTU STARTUP & RAM AWAL\n")
        results.append("   - Durasi App Startup   : $appStartupTime ms\n")
        results.append("   - RAM JVM Heap awal    : ${String.format(Locale.US, "%.2f", mainJvm)} MB\n")
        results.append("   - RAM Native Heap awal : ${String.format(Locale.US, "%.2f", mainNative)} MB\n")
        results.append("   - Total RAM (PSS) awal : ${String.format(Locale.US, "%.2f", mainPss)} MB\n\n")

        // 2. PENGUJIAN GANTI KAMERA (LENS FACING SWITCH)
        Log.i("PERF_TEST", "[2/7] Menguji Waktu Respon Switch Kamera...")
        val switchCameraBtn = device.findObject(By.desc("Ganti kamera"))
        assertNotNull("Tombol Ganti kamera tidak ditemukan", switchCameraBtn)

        // Kamera belakang ke depan
        val switch1Start = System.currentTimeMillis()
        switchCameraBtn.click()
        Thread.sleep(800L) // Jeda stabilisasi camera provider
        val switch1Time = System.currentTimeMillis() - switch1Start

        // Kamera depan kembali ke belakang
        val switch2Start = System.currentTimeMillis()
        switchCameraBtn.click()
        Thread.sleep(800L) // Jeda stabilisasi camera provider
        val switch2Time = System.currentTimeMillis() - switch2Start

        results.append("B. RESPONSE TIME SWITCH KAMERA (LENS FACING)\n")
        results.append("   - Kamera Belakang -> Depan : $switch1Time ms\n")
        results.append("   - Kamera Depan -> Belakang : $switch2Time ms\n\n")

        // 3. PENGUJIAN 10 X INFERENSI TFLITE & LOAD MODEL SECARA LANGSUNG
        Log.i("PERF_TEST", "[3/7] Menjalankan Pengujian 10 Inferensi TFLite...")

        val entryPoint = EntryPoints.get(context.applicationContext, PerformanceTestEntryPoint::class.java)
        val repository = entryPoint.produceRepository()

        // Buat bitmap mock 224x224 untuk input TFLite
        val mockBitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)

        val beforeLoopJvm = getUsedJvmHeapMemory()
        val beforeLoopNative = getUsedNativeHeapMemory()
        val beforeLoopPss = getAppTotalPssMemory()

        val inferenceDurations = mutableListOf<Long>()

        // Loop inferensi 10 kali berturut-turut secara langsung
        for (i in 1..10) {
            val startSingle = System.currentTimeMillis()
            // Menjalankan inferensi gambar secara synchronous
            val classification = kotlinx.coroutines.runBlocking {
                repository.analyzeImage(mockBitmap)
            }
            val singleTime = System.currentTimeMillis() - startSingle
            inferenceDurations.add(singleTime)
            Log.i("PERF_TEST", "   - Inferensi Mock #$i: $singleTime ms, Terdeteksi: ${classification.firstOrNull()?.name ?: "Kosong"}")
        }

        val afterLoopJvm = getUsedJvmHeapMemory()
        val afterLoopNative = getUsedNativeHeapMemory()
        val afterLoopPss = getAppTotalPssMemory()

        // 3b. MEMBACA INFERENSI DARI KAMERA AKTIF SECARA REAL-TIME (10 FRAME)
        Log.i("PERF_TEST", "Membaca 10 data waktu inferensi real-time dari kamera aktif...")
        val cameraInferenceTimes = mutableListOf<Long>()
        val liveCameraJvm = getUsedJvmHeapMemory()
        val liveCameraNative = getUsedNativeHeapMemory()
        val liveCameraPss = getAppTotalPssMemory()

        for (i in 1..10) {
            val inferenceTextObj = device.wait(
                Until.findObject(By.text(Pattern.compile("Inferensi \\d+ ms"))),
                3000
            )
            if (inferenceTextObj != null) {
                val text = inferenceTextObj.text
                val msVal = text.substringAfter("Inferensi ").substringBefore(" ms").toLongOrNull()
                if (msVal != null) {
                    cameraInferenceTimes.add(msVal)
                    Log.i("PERF_TEST", "   - Kamera Live Frame #$i: $msVal ms")
                }
            } else {
                Log.w("PERF_TEST", "   - Kamera Live Frame #$i: Gagal mendeteksi teks inferensi di UI")
            }
            Thread.sleep(200L) // Jeda antar pembacaan frame preview
        }

        val avgMock = inferenceDurations.average()
        val avgCamera = if (cameraInferenceTimes.isNotEmpty()) cameraInferenceTimes.average() else 0.0

        results.append("C. PENGUJIAN KINERJA INFERENSI (10 KALI RUN)\n")
        results.append("   1. METODE A: MOCK BITMAP IN-MEMORY (224x224)\n")
        inferenceDurations.forEachIndexed { index, time ->
            results.append("      * Run #${index + 1} : $time ms\n")
        }
        results.append("      * Total Durasi  : ${inferenceDurations.sum()} ms\n")
        results.append("      * Rata-rata     : ${String.format(Locale.US, "%.2f", avgMock)} ms\n")
        results.append("      * RAM Sebelum   : ${String.format(Locale.US, "%.2f", beforeLoopJvm)} MB (JVM), ${String.format(Locale.US, "%.2f", beforeLoopNative)} MB (Native), ${String.format(Locale.US, "%.2f", beforeLoopPss)} MB (PSS)\n")
        results.append("      * RAM Setelah   : ${String.format(Locale.US, "%.2f", afterLoopJvm)} MB (JVM), ${String.format(Locale.US, "%.2f", afterLoopNative)} MB (Native), ${String.format(Locale.US, "%.2f", afterLoopPss)} MB (PSS)\n")
        results.append("      * RAM Selisih   : ${String.format(Locale.US, "%.2f", afterLoopJvm - beforeLoopJvm)} MB (JVM), ${String.format(Locale.US, "%.2f", afterLoopNative - beforeLoopNative)} MB (Native), ${String.format(Locale.US, "%.2f", afterLoopPss - beforeLoopPss)} MB (PSS)\n\n")

        results.append("   2. METODE B: ALIRAN PREVIEW KAMERA LANGSUNG (LIVE FEED)\n")
        if (cameraInferenceTimes.isNotEmpty()) {
            cameraInferenceTimes.forEachIndexed { index, time ->
                results.append("      * Frame #${index + 1} : $time ms\n")
            }
            results.append("      * Rata-rata     : ${String.format(Locale.US, "%.2f", avgCamera)} ms\n")
        } else {
            results.append("      * (Gagal membaca data inferensi kamera dari UI, pastikan kamera mengarah ke objek/gambar)\n")
        }
        results.append("      * RAM Saat Live : ${String.format(Locale.US, "%.2f", liveCameraJvm)} MB (JVM), ${String.format(Locale.US, "%.2f", liveCameraNative)} MB (Native), ${String.format(Locale.US, "%.2f", liveCameraPss)} MB (PSS)\n\n")

        results.append("   3. PERBANDINGAN KINERJA (MOCK VS KAMERA)\n")
        results.append("      * Selisih Rata-rata Waktu : ${String.format(Locale.US, "%.2f", avgCamera - avgMock)} ms (Kamera vs Mock)\n")
        results.append("      * Selisih RAM PSS Aktif   : ${String.format(Locale.US, "%.2f", liveCameraPss - afterLoopPss)} MB (Kamera vs Mock)\n\n")

        // 4. TRANSISI KE HALAMAN TENTANG (AboutScreen) & RAM
        Log.i("PERF_TEST", "[4/7] Menguji Transisi Halaman Tentang...")
        val transitionCameraToAboutStart = System.currentTimeMillis()
        device.findObject(By.desc("Tentang aplikasi")).click()

        val aboutScreenLoaded = device.wait(Until.hasObject(By.text("Tentang Aplikasi")), 8000)
        val transitionCameraToAboutTime = System.currentTimeMillis() - transitionCameraToAboutStart
        assertNotNull("Gagal berpindah ke AboutScreen", aboutScreenLoaded)
        Thread.sleep(1000L) // Stabilitas rendering

        val aboutJvm = getUsedJvmHeapMemory()
        val aboutNative = getUsedNativeHeapMemory()
        val aboutPss = getAppTotalPssMemory()

        // Kembali ke Kamera
        val transitionAboutToCameraStart = System.currentTimeMillis()
        device.findObject(By.desc("Kembali")).click()
        val cameraScreenReturned = device.wait(Until.hasObject(By.desc("Tentang aplikasi")), 8000)
        val transitionAboutToCameraTime = System.currentTimeMillis() - transitionAboutToCameraStart
        assertNotNull("Gagal kembali ke Camera Screen dari Halaman Tentang", cameraScreenReturned)
        Thread.sleep(1000L)

        results.append("D. TRANSISI NAVIGASI HALAMAN TENTANG (ABOUT SCREEN)\n")
        results.append("   - Response Time (Kamera -> Tentang) : $transitionCameraToAboutTime ms\n")
        results.append("   - Response Time (Tentang -> Kamera) : $transitionAboutToCameraTime ms\n")
        results.append("   - RAM saat di Halaman Tentang:\n")
        results.append("     * JVM Heap                        : ${String.format(Locale.US, "%.2f", aboutJvm)} MB\n")
        results.append("     * Native Heap                     : ${String.format(Locale.US, "%.2f", aboutNative)} MB\n")
        results.append("     * Total RAM (PSS)                 : ${String.format(Locale.US, "%.2f", aboutPss)} MB\n\n")

        // 5. PILIH GAMBAR DARI GALERI & TRANSISI KE HASIL
        Log.i("PERF_TEST", "[5/7] Menguji Transisi Memilih Gambar dari Galeri...")
        val testFileName = "perf_test_gallery_${System.currentTimeMillis()}.jpg"
        insertTestImageIntoGallery(testFileName, Color.RED)

        val galleryBtn = device.findObject(By.desc("Pilih dari galeri"))
        assertNotNull("Tombol Pilih dari galeri tidak ditemukan", galleryBtn)

        galleryBtn.click()
        val transitionGalleryToResultStart = pickNewestImageFromSystemGallery(testFileName)

        val resultFromGalleryLoaded = device.wait(Until.hasObject(By.text("Hasil Pemindaian")), 20_000)
        val transitionGalleryToResultTime = System.currentTimeMillis() - transitionGalleryToResultStart
        assertNotNull("Gagal memuat Halaman Hasil setelah memilih dari galeri", resultFromGalleryLoaded)
        Thread.sleep(1500L) // Stabilitas rendering gambar

        val galleryResultJvm = getUsedJvmHeapMemory()
        val galleryResultNative = getUsedNativeHeapMemory()
        val galleryResultPss = getAppTotalPssMemory()

        // Kembali ke Kamera dari halaman Hasil
        val transitionResult1ToCameraStart = System.currentTimeMillis()
        device.findObject(By.desc("Kembali")).click()
        val cameraReturnedFromGallery = device.wait(Until.hasObject(By.desc("Tentang aplikasi")), 8000)
        val transitionResult1ToCameraTime = System.currentTimeMillis() - transitionResult1ToCameraStart
        assertNotNull("Gagal kembali ke Camera Screen dari Halaman Hasil", cameraReturnedFromGallery)
        Thread.sleep(1500L)

        results.append("E. TRANSISI NAVIGASI GALERI -> HALAMAN HASIL\n")
        results.append("   - Response Time (Pilih Galeri -> Hasil) : $transitionGalleryToResultTime ms\n")
        results.append("   - Response Time (Hasil -> Kamera Utama) : $transitionResult1ToCameraTime ms\n")
        results.append("   - RAM saat di Halaman Hasil (Galeri):\n")
        results.append("     * JVM Heap                            : ${String.format(Locale.US, "%.2f", galleryResultJvm)} MB\n")
        results.append("     * Native Heap                         : ${String.format(Locale.US, "%.2f", galleryResultNative)} MB\n")
        results.append("     * Total RAM (PSS)                     : ${String.format(Locale.US, "%.2f", galleryResultPss)} MB\n\n")

        // 6. SHUTTER CAPTURE & TRANSISI KE HASIL PEMINDAIAN
        Log.i("PERF_TEST", "[6/7] Menguji Transisi Shutter Capture Kamera...")
        val captureBtn = device.findObject(By.desc("Ambil Gambar"))
        assertNotNull("Tombol Ambil Gambar tidak ditemukan", captureBtn)

        val transitionCaptureToResultStart = System.currentTimeMillis()
        captureBtn.click()

        val resultFromCaptureLoaded = device.wait(Until.hasObject(By.text("Hasil Pemindaian")), 20_000)
        val transitionCaptureToResultTime = System.currentTimeMillis() - transitionCaptureToResultStart
        assertNotNull("Gagal memuat Halaman Hasil setelah memotret", resultFromCaptureLoaded)
        Thread.sleep(1500L)

        val captureResultJvm = getUsedJvmHeapMemory()
        val captureResultNative = getUsedNativeHeapMemory()
        val captureResultPss = getAppTotalPssMemory()

        // Respons Tombol Simpan
        swipeUp()
        Thread.sleep(800L)
        val saveBtn = device.findObject(By.res(Pattern.compile(".*save_to_gallery_button")))
            ?: device.findObject(By.text("Simpan"))
        val saveClickTime = if (saveBtn != null) {
            val saveStart = System.currentTimeMillis()
            saveBtn.click()
            val snackbarAppeared = device.wait(Until.hasObject(By.textContains("Gambar berhasil disimpan")), 15_000)
            val duration = System.currentTimeMillis() - saveStart
            Log.i("PERF_TEST", "Respons simpan galeri: $duration ms, Status: $snackbarAppeared")
            duration
        } else {
            -1L
        }

        // Kembali ke Kamera dari halaman Hasil
        val transitionResult2ToCameraStart = System.currentTimeMillis()
        val backBtnResult = device.findObject(By.desc("Kembali"))
        backBtnResult?.click()
        val cameraReturnedFromCapture = device.wait(Until.hasObject(By.desc("Tentang aplikasi")), 8000)
        val transitionResult2ToCameraTime = System.currentTimeMillis() - transitionResult2ToCameraStart

        results.append("F. TRANSISI NAVIGASI CAPTURE (SHUTTER) -> HALAMAN HASIL\n")
        results.append("   - Response Time (Shutter Capture -> Hasil) : $transitionCaptureToResultTime ms\n")
        results.append("   - Response Time (Hasil -> Kamera Utama)   : ${if (cameraReturnedFromCapture) "$transitionResult2ToCameraTime ms" else "Gagal"}\n")
        results.append("   - Response Time Tombol Simpan Galeri      : ${if (saveClickTime >= 0) "$saveClickTime ms" else "Tombol Tidak Ditemukan"}\n")
        results.append("   - RAM saat di Halaman Hasil (Capture):\n")
        results.append("     * JVM Heap                               : ${String.format(Locale.US, "%.2f", captureResultJvm)} MB\n")
        results.append("     * Native Heap                            : ${String.format(Locale.US, "%.2f", captureResultNative)} MB\n")
        results.append("     * Total RAM (PSS)                        : ${String.format(Locale.US, "%.2f", captureResultPss)} MB\n\n")

        results.append("====================================================\n")
        results.append("                 PENGUJIAN SELESAI\n")
        results.append("====================================================\n")

        // Cetak Laporan Ke Logcat
        val finalReport = results.toString()
        Log.i("PERF_TEST_REPORT", "\n" + finalReport)

        // Simpan Hasil Laporan Teks dan Gambar ke folder publik (Downloads & Pictures) agar mudah diakses via USB / MTP
        saveReportToDownloads(context, "Hasil_Laporan_Kinerja_RAM_Respons.txt", finalReport)
        saveScreenshotToPictures(context, "screenshot_pengujian.png", device)
    }
    private fun saveReportToDownloads(context: Context, fileName: String, content: String) {
        runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${android.os.Environment.DIRECTORY_DOWNLOADS}/CnnFreshScanPerformance"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching
            resolver.openOutputStream(uri)?.use { output ->
                output.write(content.toByteArray())
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            Log.i("PERF_TEST", "✓ Laporan berhasil disimpan ke folder Downloads/CnnFreshScanPerformance/$fileName")
        }.onFailure { e ->
            Log.e("PERF_TEST", "❌ Gagal menyimpan laporan ke folder Downloads", e)
        }
    }
    private fun getAppTotalPssMemory(): Double {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val pids = intArrayOf(android.os.Process.myPid())
        val memoryInfo = am.getProcessMemoryInfo(pids)
        return memoryInfo[0].totalPss / 1024.0
    }

    private fun saveScreenshotToPictures(context: Context, fileName: String, device: UiDevice) {
        runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${android.os.Environment.DIRECTORY_PICTURES}/CnnFreshScanPerformance"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching

            // Simpan sementara di cache
            val tempFile = File(context.cacheDir, "temp_screenshot.png")
            device.takeScreenshot(tempFile)

            resolver.openOutputStream(uri)?.use { output ->
                tempFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            tempFile.delete()

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            Log.i("PERF_TEST", "✓ Screenshot berhasil disimpan ke folder Pictures/CnnFreshScanPerformance/$fileName")
        }.onFailure { e ->
            Log.e("PERF_TEST", "❌ Gagal menyimpan screenshot ke folder Pictures", e)
        }
    }

    private fun insertTestImageIntoGallery(fileName: String, color: Int) {
        val resolver = context.contentResolver
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${android.os.Environment.DIRECTORY_PICTURES}/CnnFreshScanTest")
            put(MediaStore.Images.Media.DATE_TAKEN, now)
            put(MediaStore.Images.Media.DATE_ADDED, now / 1000)
            put(MediaStore.Images.Media.DATE_MODIFIED, now / 1000)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Gagal membuat gambar test di galeri")

        val bitmap = createFruitLikeTestBitmap(color)

        resolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
        } ?: error("Output stream gambar test tidak tersedia")

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        values.put(MediaStore.Images.Media.DATE_TAKEN, now)
        values.put(MediaStore.Images.Media.DATE_ADDED, now / 1000)
        values.put(MediaStore.Images.Media.DATE_MODIFIED, now / 1000)
        resolver.update(uri, values, null, null)

        // Dapatkan path absolut untuk media scanner agar terindeks instan di galeri HP
        runCatching {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            resolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    val realPath = cursor.getString(dataIndex)
                    MediaScannerConnection.scanFile(context, arrayOf(realPath), arrayOf("image/jpeg"), null)
                    Log.i("PERF_TEST", "✓ Berhasil men-scan berkas baru ke galeri: $realPath")
                }
            }
        }
        Thread.sleep(2000L)
    }

    private fun createFruitLikeTestBitmap(fruitColor: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.LTGRAY)
        val paint = Paint().apply {
            color = fruitColor
            isAntiAlias = true
        }
        canvas.drawCircle(320f, 320f, 200f, paint)
        return bitmap
    }

    private fun pickNewestImageFromSystemGallery(fileName: String): Long {
        device.wait(Until.gone(By.pkg(packageName)), 10_000)
        Thread.sleep(800L)

        dismissSystemPermissionDialogs()
        selectPhotosTabIfPresent()

        clickLatestGalleryThumbnail(fileName)
        var clickTime = System.currentTimeMillis()

        val confirmButton = device.wait(
            Until.findObject(
                By.text(Pattern.compile("(Add|Select|Pilih|Tambah|Tambahkan|Selesai|Done|OK|Open|Buka|Gunakan)", Pattern.CASE_INSENSITIVE))
            ),
            1500L
        )
        if (confirmButton != null) {
            confirmButton.click()
            clickTime = System.currentTimeMillis()
        }

        device.wait(Until.hasObject(By.pkg(packageName)), 20_000L)
        return clickTime
    }

    private fun dismissSystemPermissionDialogs() {
        val permissionPatterns = Pattern.compile(
            "(Allow|Izinkan|While using the app|Saat aplikasi digunakan|Only this time|Hanya kali ini)",
            Pattern.CASE_INSENSITIVE
        )
        val button = device.findObject(By.text(permissionPatterns))
        if (button != null) {
            Log.i("PERF_TEST", "Mendeteksi dialog izin sistem, mengeklik: ${button.text}")
            button.click()
            Thread.sleep(600L)
        }
    }

    private fun selectPhotosTabIfPresent() {
        val tabPatterns = Pattern.compile(
            "(Photos|Foto|Recent|Terbaru|Semua)",
            Pattern.CASE_INSENSITIVE
        )
        val tab = device.findObject(By.text(tabPatterns))
        if (tab != null) {
            Log.i("PERF_TEST", "Mengeklik tab galeri: ${tab.text}")
            tab.click()
            Thread.sleep(600L)
        }
    }

    private fun clickLatestGalleryThumbnail(fileName: String) {
        val baseName = fileName.substringBeforeLast(".")
        val matchingObjects = device.findObjects(By.descContains(baseName)) +
                              device.findObjects(By.textContains(baseName)) +
                              device.findObjects(By.descContains("CnnFreshScanTest"))

        val validThumbnail = matchingObjects.firstOrNull { node ->
            val desc = node.contentDescription?.lowercase().orEmpty()
            val res = node.resourceName?.lowercase().orEmpty()
            !desc.contains("preview") && !desc.contains("pratinjau") && !desc.contains("zoom") &&
            !res.contains("preview") && !res.contains("pratinjau")
        }

        if (validThumbnail != null) {
            validThumbnail.click()
            return
        }

        findGalleryThumbnailCandidate()?.click() ?: clickLikelyFirstGridCells()
    }

    private fun findGalleryThumbnailCandidate(): UiObject2? {
        val knownIds = listOf(
            "com.android.providers.media.module:id/thumbnail_image",
            "com.google.android.providers.media.module:id/thumbnail_image",
            "com.android.documentsui:id/icon_thumb",
            "com.google.android.documentsui:id/icon_thumb",
            "com.google.android.apps.photos:id/thumbnail",
            "com.miui.gallery:id/thumbnail"
        )
        for (id in knownIds) {
            val elements = device.findObjects(By.res(id))
            val validElements = elements.filter { node ->
                val desc = node.contentDescription?.lowercase().orEmpty()
                val res = node.resourceName?.lowercase().orEmpty()
                !desc.contains("preview") && !desc.contains("pratinjau") && !desc.contains("zoom") &&
                !res.contains("preview") && !res.contains("pratinjau")
            }
            if (validElements.isNotEmpty()) {
                Log.i("PERF_TEST", "✓ Menemukan thumbnail dengan ID: $id")
                return validElements.first()
            }
        }

        val topLimit = (device.displayHeight * 0.14f).toInt()
        val bottomLimit = (device.displayHeight * 0.90f).toInt()
        val candidates = device.findObjects(By.clickable(true))
            .filter { node ->
                val bounds = node.visibleBounds
                val width = bounds.width()
                val height = bounds.height()
                val desc = node.contentDescription?.lowercase().orEmpty()
                val text = node.text?.lowercase().orEmpty()
                val res = node.resourceName?.lowercase().orEmpty()

                val hasThumbnailSize = width >= 100 && height >= 100 &&
                                       width <= device.displayWidth / 2 &&
                                       height <= device.displayHeight / 2

                val isPreviewButton = desc.contains("preview") || desc.contains("pratinjau") || desc.contains("zoom") ||
                                      res.contains("preview") || res.contains("pratinjau")

                val isExternalApp = text.contains("bug") || text.contains("drive") || text.contains("report") ||
                                     desc.contains("bug") || desc.contains("drive")

                width >= 80 && height >= 80 &&
                bounds.top >= topLimit &&
                bounds.bottom <= bottomLimit &&
                hasThumbnailSize && !isPreviewButton && !isExternalApp
            }
            .sortedWith(compareBy<UiObject2> { it.visibleBounds.top }.thenBy { it.visibleBounds.left })

        Log.i("PERF_TEST", "Kandidat thumbnail fallback: ${candidates.size}")
        return candidates.firstOrNull()
    }

    private fun clickLikelyFirstGridCells() {
        val possiblePoints = listOf(
            0.22f to 0.39f,
            0.50f to 0.39f,
            0.78f to 0.39f
        )
        for ((xf, yf) in possiblePoints) {
            val px = (device.displayWidth * xf).toInt()
            val py = (device.displayHeight * yf).toInt()
            device.click(px, py)
            Thread.sleep(1000L)
            if (device.hasObject(By.pkg(packageName))) {
                return
            }
        }
    }

    private fun getUsedJvmHeapMemory(): Double {
        val runtime = Runtime.getRuntime()
        val usedBytes = runtime.totalMemory() - runtime.freeMemory()
        return usedBytes / (1024.0 * 1024.0)
    }

    private fun getUsedNativeHeapMemory(): Double {
        val allocatedBytes = Debug.getNativeHeapAllocatedSize()
        return allocatedBytes / (1024.0 * 1024.0)
    }


    private fun swipeUp() {
        val centerX = device.displayWidth / 2
        val startY = (device.displayHeight * 0.75f).toInt()
        val endY = (device.displayHeight * 0.25f).toInt()
        device.swipe(centerX, startY, centerX, endY, 15)
    }
}



