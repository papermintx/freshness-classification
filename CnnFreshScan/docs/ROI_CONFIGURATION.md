# Panduan Konfigurasi ROI Kamera

Dokumen ini menjelaskan bagian kode yang mengatur ROI atau area pindai kamera pada CnnFreshScan. Tujuannya agar ROI yang terlihat di layar pengguna sama dengan area gambar yang dipotong dan diproses di data layer.

## Sumber Konfigurasi Utama

File utama:

```text
core/src/main/java/com/skripsi/core/domain/model/RoiConfiguration.kt
```

Variabel yang paling sering diubah:

```kotlin
val Default = RoiConfiguration(
    sizeFraction = 0.75f,
    verticalBias = -0.22f
)
```

## Arti Variabel

`sizeFraction`

Mengatur ukuran ROI berdasarkan sisi terpendek gambar atau layar.

Contoh:

```text
0.75f = ROI memakai 75% dari sisi terpendek
0.70f = ROI lebih kecil
0.80f = ROI lebih besar
```

`verticalBias`

Mengatur posisi ROI secara vertikal.

```text
0f      = ROI tepat di tengah
-0.22f  = ROI digeser sedikit ke atas
0.22f   = ROI digeser sedikit ke bawah
```

Nilai dibatasi oleh kode ke rentang `-0.45f` sampai `0.45f` supaya ROI tetap berada di dalam gambar.

## File yang Terpengaruh

Data layer:

```text
core/src/main/java/com/skripsi/core/data/image/RoiGeometry.kt
core/src/main/java/com/skripsi/core/data/image/RoiCropper.kt
```

Domain layer:

```text
core/src/main/java/com/skripsi/core/domain/model/RoiConfiguration.kt
core/src/main/java/com/skripsi/core/domain/usecase/GetRoiConfigurationUseCase.kt
```

Presentation layer:

```text
app/src/main/java/com/skripsi/cnnfreshscan/presentation/viewmodel/CameraViewModel.kt
app/src/main/java/com/skripsi/cnnfreshscan/presentation/util/CameraRoi.kt
app/src/main/java/com/skripsi/cnnfreshscan/presentation/screen/CameraScreen.kt
```

`RoiCropper` memakai `RoiGeometry` untuk crop bitmap. `CameraViewModel` mengambil konfigurasi ROI melalui `GetRoiConfigurationUseCase`, lalu `CameraScreen` memakai nilai dari state ViewModel untuk menggambar ROI. Dengan begitu, Presentation Layer tidak mengakses class Data Layer secara langsung.

## Cara Mengubah Ukuran ROI

Edit:

```text
core/src/main/java/com/skripsi/core/domain/model/RoiConfiguration.kt
```

Ubah:

```kotlin
sizeFraction = 0.75f
```

Saran nilai:

```text
0.70f untuk ROI lebih kecil
0.75f untuk ukuran saat ini
0.80f untuk ROI lebih besar
```

## Cara Mengubah Posisi ROI

Edit:

```text
core/src/main/java/com/skripsi/core/domain/model/RoiConfiguration.kt
```

Ubah:

```kotlin
verticalBias = -0.22f
```

Saran nilai:

```text
-0.30f jika ROI masih terlalu bawah
-0.22f untuk posisi saat ini
-0.10f jika ROI terlalu atas
0f jika ingin ROI tepat di tengah
```

## Testing Setelah Mengubah ROI

Jalankan test berikut:

```powershell
.\gradlew.bat :core:testDebugUnitTest --tests com.skripsi.core.data.image.RoiGeometryTest
.\gradlew.bat :app:testDebugUnitTest --tests com.skripsi.cnnfreshscan.presentation.util.CameraRoiTest
.\gradlew.bat :core:compileDebugAndroidTestKotlin
```

Jika mengubah nilai `sizeFraction` atau `verticalBias`, sesuaikan expected value pada:

```text
core/src/test/java/com/skripsi/core/data/image/RoiGeometryTest.kt
app/src/test/java/com/skripsi/cnnfreshscan/presentation/util/CameraRoiTest.kt
```

## Catatan Penting

Jangan membuat rumus crop baru di presentation layer. Presentation layer hanya boleh menggambar ROI. Proses crop untuk inferensi tetap dilakukan di data layer melalui:

```text
core/src/main/java/com/skripsi/core/data/image/RoiCropper.kt
```

Dengan pola ini, area yang dilihat pengguna dan area yang diproses model tetap konsisten.
