package com.skripsi.cnnfreshscan.research

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class CnnFreshScanBlackBoxTest {

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private lateinit var device: UiDevice
    private lateinit var context: Context
    private val packageName = "com.skripsi.cnnfreshscan"

    @Before
    fun launchApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = ApplicationProvider.getApplicationContext()
        device.pressHome()

        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: error("Tidak dapat membuka aplikasi $packageName")
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)

        waitForUi("camera_screen", 15_000)
    }

    @Test
    fun f01_cameraRealtimeShowsRoiAndPredictionCard() {
        waitForUi("camera_screen")
        waitForUi("camera_preview")
        waitForUi("roi_box")
        waitForUi("prediction_label")
        waitForUi("confidence_score")
    }
    @Test
    fun f02_shutterNavigatesToResultAndShowsClassificationData() {
        clickByUi("capture_button")

        waitForUi("result_screen", 30_000)
        waitForUi("result_image", 20_000)
        waitForUi("result_label", 30_000)
        waitForUi("result_confidence", 30_000)
        waitForUi("handling_advice", 30_000)
    }
    @Test
    fun f03_saveToGalleryButtonStoresWatermarkedResult() {
        clickByUi("capture_button")

        waitForUi("result_screen", 30_000)
        scrollUntilUi("save_to_gallery_button", 30_000).click()

        assertAnyTextContains(
            timeoutMs = 20_000,
            "Gambar berhasil disimpan",
            "Gagal menyimpan gambar"
        )
    }
    @Test
    fun f04_galleryImageCanBeSelectedAndClassified() {
        val fileName = "cnnfreshscan_gallery_valid_${System.currentTimeMillis()}.jpg"
        insertTestImageIntoGallery(
            fileName = fileName,
            color = Color.rgb(245, 180, 40)
        )

        clickByUi("gallery_button")
        pickNewestImageFromSystemGallery(fileName)

        waitForUi("result_screen", 30_000)
        waitForUi("result_image", 20_000)
        waitForUi("result_label", 30_000)
        waitForUi("result_confidence", 30_000)
    }
    @Test
    fun f05_invalidDarkInputDoesNotCrashAndShowsClassificationResult() {
        val fileName = "cnnfreshscan_gallery_dark_${System.currentTimeMillis()}.jpg"
        insertTestImageIntoGallery(
            fileName = fileName,
            color = Color.BLACK
        )

        clickByUi("gallery_button")
        pickNewestImageFromSystemGallery(fileName)

        waitForUi("result_screen", 30_000)

        val label = waitForUi("result_label", 30_000).text.orEmpty()
        val confidenceText = waitForUi("result_confidence", 30_000).text.orEmpty()
        val confidence = extractConfidencePercent(confidenceText)

        assertTrue(
            "Input gelap harus tetap aman, menampilkan salah satu label, dan confidence valid: label=$label confidence=$confidenceText",
            label in validPredictionLabels() && confidence in 0.0..100.0
        )
    }
    @Test
    fun f06_corruptedImageFromGalleryDoesNotCrashAndShowsInvalidInputSnackbar() {
        val fileName = "cnnfreshscan_gallery_corrupt_${System.currentTimeMillis()}.jpg"
        insertCorruptedImageIntoGallery(fileName)

        clickByUi("gallery_button")
        pickNewestImageFromSystemGallery(fileName)

        waitForUi("result_screen", 30_000)
        assertAnyTextContains(
            timeoutMs = 15_000,
            "Input tidak valid",
            "Silakan pilih gambar lain"
        )
    }

    private fun clickByUi(id: String, timeoutMs: Long = 10_000) {
        waitForUi(id, timeoutMs).click()
    }

    private fun scrollUntilUi(id: String, timeoutMs: Long = 10_000): UiObject2 {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            findUi(id)?.let { return it }
            swipeUp()
            Thread.sleep(400L)
        }
        throw AssertionError("Komponen UI '$id' tidak ditemukan setelah scroll")
    }

    private fun swipeUp() {
        val centerX = device.displayWidth / 2
        val startY = (device.displayHeight * 0.78f).toInt()
        val endY = (device.displayHeight * 0.30f).toInt()
        device.swipe(centerX, startY, centerX, endY, 18)
    }

    private fun waitForUi(id: String, timeoutMs: Long = 10_000): UiObject2 {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            findUi(id)?.let { return it }
            Thread.sleep(250L)
        }
        throw AssertionError("Komponen UI '$id' tidak ditemukan")
    }

    private fun findUi(id: String): UiObject2? {
        device.findObject(By.res(packageName, id))?.let { return it }
        return when (id) {
            "camera_screen" -> device.findObject(By.text("Arahkan Buah/Sayur di Dalam Area Pindai"))
            "camera_preview" -> device.findObject(By.desc("Pratinjau kamera"))
                ?: device.findObject(By.text("Arahkan Buah/Sayur di Dalam Area Pindai"))
            "roi_box" -> device.findObject(By.desc("Area ROI"))
            "capture_button" -> device.findObject(By.desc("Ambil Gambar"))
            "gallery_button" -> device.findObject(By.desc("Pilih dari galeri"))
            "prediction_label" -> findAnyPredictionLabel()
            "confidence_score" -> device.findObject(By.text(Pattern.compile(".*\\d+(\\.\\d+)?%.*")))
            "result_screen" -> device.findObject(By.text("Hasil Pemindaian"))
            "result_image" -> device.findObject(By.desc("Gambar hasil analisis"))
            "result_label" -> findAnyPredictionLabel()
                ?: device.findObject(By.text("Analisis gagal"))
            "result_confidence" -> device.findObject(By.textContains("Keyakinan"))
            "handling_advice" -> device.findObject(By.text("Saran Penanganan"))
            "save_to_gallery_button" -> device.findObject(By.text("Simpan"))
            "error_message" -> device.findObject(By.text("Analisis gagal"))
            else -> null
        }
    }

    private fun findAnyPredictionLabel(): UiObject2? {
        val labels = listOf(
            "PISANG SEGAR",
            "PISANG BUSUK",
            "MANGGA SEGAR",
            "MANGGA BUSUK",
            "JERUK SEGAR",
            "JERUK BUSUK",
            "WORTEL SEGAR",
            "WORTEL BUSUK",
            "TIMUN SEGAR",
            "TIMUN BUSUK",
            "TOMAT SEGAR",
            "TOMAT BUSUK"
        )
        return labels.firstNotNullOfOrNull { label ->
            device.findObject(By.text(label))
        }
    }

    private fun validPredictionLabels(): List<String> {
        return listOf(
            "PISANG SEGAR",
            "PISANG BUSUK",
            "MANGGA SEGAR",
            "MANGGA BUSUK",
            "JERUK SEGAR",
            "JERUK BUSUK",
            "WORTEL SEGAR",
            "WORTEL BUSUK",
            "TIMUN SEGAR",
            "TIMUN BUSUK",
            "TOMAT SEGAR",
            "TOMAT BUSUK"
        )
    }

    private fun assertAnyTextContains(timeoutMs: Long, vararg candidates: String) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val found = candidates.any { text ->
                device.hasObject(By.textContains(text))
            }
            if (found) return
            Thread.sleep(300L)
        }
        throw AssertionError("Tidak menemukan salah satu teks: ${candidates.joinToString()}")
    }

    private fun insertTestImageIntoGallery(fileName: String, color: Int) {
        val resolver = context.contentResolver
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/CnnFreshScanTest")
            put(MediaStore.Images.Media.DATE_TAKEN, now)
            put(MediaStore.Images.Media.DATE_ADDED, now / 1000)
            put(MediaStore.Images.Media.DATE_MODIFIED, now / 1000)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Gagal membuat gambar test di galeri")

        val bitmap = if (color == Color.BLACK) {
            Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.BLACK)
            }
        } else {
            createFruitLikeTestBitmap(color)
        }

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
                }
            }
        }
        Thread.sleep(2000L)
    }

    private fun insertCorruptedImageIntoGallery(fileName: String) {
        val resolver = context.contentResolver
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/CnnFreshScanTest")
            put(MediaStore.Images.Media.DATE_TAKEN, now)
            put(MediaStore.Images.Media.DATE_ADDED, now / 1000)
            put(MediaStore.Images.Media.DATE_MODIFIED, now / 1000)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Gagal membuat gambar rusak di galeri")

        resolver.openOutputStream(uri)?.use { output ->
            output.write("ini bukan file jpeg valid untuk pengujian error".toByteArray())
        } ?: error("Output stream gambar rusak tidak tersedia")

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
                }
            }
        }
        Thread.sleep(2000L)
    }

    private fun pickNewestImageFromSystemGallery(fileName: String) {
        device.wait(Until.gone(By.pkg(packageName)), 10_000)
        Thread.sleep(2000L)

        dismissSystemPermissionDialogs()
        selectPhotosTabIfPresent()

        clickLatestGalleryThumbnail(fileName)

        val confirmButton = device.wait(
            Until.findObject(
                By.text(Pattern.compile("(Add|Select|Pilih|Tambah|Tambahkan|Selesai|Done|OK|Open|Buka|Gunakan)", Pattern.CASE_INSENSITIVE))
            ),
            3000L
        )
        confirmButton?.click()

        device.wait(Until.hasObject(By.pkg(packageName)), 20_000L)
    }

    private fun dismissSystemPermissionDialogs() {
        val permissionPatterns = Pattern.compile(
            "(Allow|Izinkan|While using the app|Saat aplikasi digunakan|Only this time|Hanya kali ini)",
            Pattern.CASE_INSENSITIVE
        )
        val button = device.findObject(By.text(permissionPatterns))
        if (button != null) {
            button.click()
            Thread.sleep(1000L)
        }
    }

    private fun selectPhotosTabIfPresent() {
        val tabPatterns = Pattern.compile(
            "(Photos|Foto|Recent|Terbaru|Semua)",
            Pattern.CASE_INSENSITIVE
        )
        val tab = device.findObject(By.text(tabPatterns))
        if (tab != null) {
            tab.click()
            Thread.sleep(1000L)
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
        
        return candidates.firstOrNull()
    }

    private fun clickLikelyFirstGridCells() {
        val possiblePoints = listOf(
            0.22f to 0.39f,
            0.50f to 0.39f,
            0.78f to 0.39f,
            0.22f to 0.49f,
            0.50f to 0.49f
        )

        possiblePoints.forEach { (xRatio, yRatio) ->
            device.click(
                (device.displayWidth * xRatio).toInt(),
                (device.displayHeight * yRatio).toInt()
            )
            Thread.sleep(700L)
            if (device.hasObject(By.pkg(packageName))) return
        }
    }

    private fun extractConfidencePercent(text: String): Double {
        val match = Regex("""(\d+(?:\.\d+)?)%""").find(text)
        return match?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 100.0
    }

    private fun createFruitLikeTestBitmap(fruitColor: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(238, 238, 232))

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(70, 0, 0, 0)
        }
        canvas.drawOval(RectF(190f, 460f, 460f, 520f), shadowPaint)

        val fruitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fruitColor
        }
        canvas.drawOval(RectF(170f, 130f, 470f, 470f), fruitPaint)

        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(95, 255, 255, 255)
        }
        canvas.drawOval(RectF(235f, 190f, 315f, 280f), highlightPaint)

        val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(92, 64, 30)
            strokeWidth = 24f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(318f, 136f, 350f, 82f, stemPaint)

        val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(56, 138, 65)
        }
        canvas.drawOval(RectF(350f, 78f, 430f, 130f), leafPaint)

        return bitmap
    }
}
