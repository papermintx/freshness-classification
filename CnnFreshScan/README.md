<p align="center">
  <img src="docs/icon_cnnfreshscan.svg" alt="CnnFreshScan Logo" width="120" />
</p>

<h1 align="center">CnnFreshScan</h1>

<p align="center">
  <strong>Aplikasi Klasifikasi Kesegaran Buah dan Sayur Berbasis CNN (MobileNetV2) pada Platform Android</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Min%20SDK-29%20(Android%2010)-blue" alt="Min SDK" />
  <img src="https://img.shields.io/badge/Target%20SDK-36-blue" alt="Target SDK" />
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/TensorFlow%20Lite-Play%20Services-FF6F00?logo=tensorflow&logoColor=white" alt="TFLite" />
  <img src="https://img.shields.io/badge/License-Academic%20Use-orange" alt="License" />
</p>

---

## 📋 Daftar Isi

- [Tentang Proyek](#-tentang-proyek)
- [Informasi Skripsi](#-informasi-skripsi)
- [Fitur Utama](#-fitur-utama)
- [Klasifikasi yang Didukung](#-klasifikasi-yang-didukung)
- [Arsitektur Aplikasi](#-arsitektur-aplikasi)
- [Struktur Modul](#-struktur-modul)
- [Tech Stack](#-tech-stack)
- [Alur Kerja Aplikasi](#-alur-kerja-aplikasi)
- [Arsitektur CNN (MobileNetV2)](#-arsitektur-cnn-mobilenetv2)
- [Struktur Direktori](#-struktur-direktori)
- [Prasyarat](#-prasyarat)
- [Instalasi & Setup](#-instalasi--setup)
- [Menjalankan Aplikasi](#-menjalankan-aplikasi)
- [Pengujian](#-pengujian)
- [Konfigurasi ROI](#-konfigurasi-roi)
- [Lisensi](#-lisensi)

---

## 📖 Tentang Proyek

**CnnFreshScan** adalah aplikasi Android yang dikembangkan sebagai bagian dari penelitian tugas akhir (skripsi) untuk mengklasifikasikan tingkat kesegaran buah dan sayur menggunakan model **Convolutional Neural Network (CNN)** dengan arsitektur **MobileNetV2**.

Aplikasi ini memanfaatkan kamera perangkat Android untuk mendeteksi dan mengklasifikasikan objek buah/sayur secara **real-time** ke dalam dua kategori kondisi: **Segar (Fresh)** atau **Busuk (Rotten)**. Model machine learning dijalankan secara on-device menggunakan **TensorFlow Lite** melalui **Google Play Services**, sehingga tidak memerlukan koneksi internet untuk melakukan inferensi.

### Tujuan Penelitian

1. Merancang dan mengembangkan aplikasi mobile berbasis Android yang mampu mengklasifikasikan kesegaran buah dan sayur menggunakan CNN
2. Mengimplementasikan model MobileNetV2 yang telah di-training ke dalam platform Android menggunakan TensorFlow Lite
3. Mengukur akurasi dan performa model dalam melakukan klasifikasi secara real-time pada perangkat mobile

---

## 🎓 Informasi Skripsi

| Atribut | Detail |
|---|---|
| **Judul** | Klasifikasi Kesegaran Buah dan Sayur Berbasis CNN (MobileNetV2) pada Platform Android |
| **Penulis** | Muhamad Muslih |
| **NIM** | 3337220025 |
| **Program Studi** | Informatika |
| **Institusi** | Universitas Sultan Ageng Tirtayasa |
| **Pembimbing I** | Royan Habibie Sukarna, S.Kom., M.Kom. |
| **Pembimbing II** | Czidni Sika Azkia, M.Kom. |
| **Versi** | 1.0 (Penelitian Tugas Akhir) |

---

## ✨ Fitur Utama

- 🔍 **Klasifikasi Real-Time** — Analisis frame kamera secara langsung dengan prediksi live dan confidence score
- 📸 **Pengambilan Gambar** — Capture gambar dari kamera atau pilih dari galeri untuk dianalisis
- 🎯 **Region of Interest (ROI)** — Area pindai yang terkalibrasi antara tampilan kamera dan area crop model
- 🗳️ **Majority Voting** — Sistem voting dari 20 frame terakhir untuk menghasilkan prediksi yang stabil
- 📊 **Hasil Klasifikasi Detail** — Menampilkan label, confidence score, waktu inferensi, dan saran penanganan
- 💾 **Simpan ke Galeri** — Menyimpan hasil klasifikasi ke galeri dengan watermark label dan confidence
- 📱 **Kamera Depan/Belakang** — Dukungan switch kamera
- 🔬 **Benchmark Tool** — Fitur pengujian (debug build) untuk riset akurasi model pada kamera
- 🎨 **Material Design 3** — UI modern dengan Jetpack Compose dan tema Material3
- ⚡ **Inferensi On-Device** — Tidak memerlukan koneksi internet, model berjalan langsung di perangkat

---

## 🏷️ Klasifikasi yang Didukung

Aplikasi mampu mengklasifikasikan **6 jenis buah dan sayur** ke dalam **2 kondisi**, menghasilkan total **12 kelas**:

### Buah (Fruits)

| Buah | Segar (Fresh) | Busuk (Rotten) |
|---|---|---|
| 🍌 Pisang (Banana) | `fruits_fresh_banana` | `fruits_rotten_banana` |
| 🥭 Mangga (Mango) | `fruits_fresh_mango` | `fruits_rotten_mango` |
| 🍊 Jeruk (Orange) | `fruits_fresh_orange` | `fruits_rotten_orange` |

### Sayur (Vegetables)

| Sayur | Segar (Fresh) | Busuk (Rotten) |
|---|---|---|
| 🥕 Wortel (Carrot) | `vegetables_fresh_carrot` | `vegetables_rotten_carrot` |
| 🥒 Timun (Cucumber) | `vegetables_fresh_cucumber` | `vegetables_rotten_cucumber` |
| 🍅 Tomat (Tomato) | `vegetables_fresh_tomato` | `vegetables_rotten_tomato` |

---

## 🏗️ Arsitektur Aplikasi

Aplikasi ini dibangun menggunakan pendekatan **Clean Architecture** dengan pola **MVVM (Model-View-ViewModel)** yang dipadukan dengan **Use Case** dan **Repository Pattern**. Arsitektur ini memisahkan aplikasi ke dalam 3 modul independen:

```
┌──────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│                       (Module: app)                          │
│                                                              │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────┐  │
│  │   Screens   │  │  ViewModels  │  │   Navigation       │  │
│  │  (Compose)  │◄─┤  (StateFlow) │  │   (NavGraph)       │  │
│  └─────────────┘  └──────┬───────┘  └────────────────────┘  │
│                          │                                   │
├──────────────────────────┼───────────────────────────────────┤
│                    DOMAIN LAYER                              │
│                     (Module: core)                           │
│                          │                                   │
│  ┌──────────────┐  ┌─────▼──────┐  ┌──────────────────┐    │
│  │    Models     │  │  Use Cases │  │   Repository     │    │
│  │  (Entities)   │  │            │  │   (Interface)    │    │
│  └──────────────┘  └─────┬──────┘  └───────┬──────────┘    │
│                          │                  │                │
│                    DATA LAYER               │                │
│                          │                  │                │
│  ┌──────────────┐  ┌─────▼──────────────────▼─────────┐    │
│  │  ROI Cropper │  │      Repository Implementation   │    │
│  │  + Geometry   │  │     (ProduceRepositoryImpl)      │    │
│  └──────────────┘  └─────────────┬────────────────────┘    │
│                                  │                          │
├──────────────────────────────────┼──────────────────────────┤
│                   INFRASTRUCTURE LAYER                       │
│                  (Module: tflite_engine)                     │
│                                  │                          │
│  ┌─────────────────────┐  ┌──────▼──────────────────┐      │
│  │  ProduceClassifier  │  │   TFLiteClassifier      │      │
│  │    (Interface)      │◄─┤  (Implementation)       │      │
│  └─────────────────────┘  └──────┬──────────────────┘      │
│                                  │                          │
│                           ┌──────▼──────────────────┐      │
│                           │    mobilenetv2_int8     │      │
│                           │     .tflite (2.60 MB)   │      │
│                           └─────────────────────────┘      │
└──────────────────────────────────────────────────────────────┘
```

### Alur Data

```
User Input (Camera/Gallery)
    │
    ▼
CameraViewModel / ResultViewModel
    │
    ▼
AnalyzeProduceUseCase
    │
    ▼
ProduceRepository (Interface)
    │
    ▼
ProduceRepositoryImpl
    ├── RoiCropper (crop bitmap ke area ROI)
    ├── ImagePreprocessor (resize 224×224, normalize [-1,1])
    └── TFLiteClassifier (inferensi model)
            │
            ▼
    ProduceClassificationResult (label + confidence)
            │
            ▼
    UI State → Compose Screen
```

---

## 📁 Struktur Modul

Proyek ini terdiri dari **3 modul Gradle**:

### 1. `:app` — Presentation Layer

Modul utama yang menangani seluruh UI, navigasi, dan interaksi pengguna.

| Package | File | Deskripsi |
|---|---|---|
| `navigation` | `Destinations.kt` | Definisi route navigasi (Splash, Camera, Result, About) |
| `navigation` | `MainNavGraph.kt` | NavHost dengan konfigurasi semua route |
| `presentation.screen` | `SplashScreen.kt` | Splash screen dengan animasi logo |
| `presentation.screen` | `CameraScreen.kt` | Kamera real-time dengan ROI overlay dan live prediction |
| `presentation.screen` | `ResultScreen.kt` | Tampilan hasil klasifikasi lengkap |
| `presentation.screen` | `AboutScreen.kt` | Halaman informasi aplikasi dan pembuat |
| `presentation.viewmodel` | `CameraViewModel.kt` | ViewModel untuk analisis kamera real-time & majority voting |
| `presentation.viewmodel` | `ResultViewModel.kt` | ViewModel untuk analisis gambar dari capture/galeri |
| `presentation.viewmodel` | `CameraBenchmarkViewModel.kt` | ViewModel untuk fitur benchmarking (debug) |
| `presentation.component` | `CameraPermissionScreen.kt` | Layar permintaan izin kamera |
| `presentation.util` | `PredictionUiFormatter.kt` | Konversi label mentah ke label tampilan (Bahasa Indonesia) |
| `presentation.util` | `CameraRoi.kt` | Kalkulasi ROI untuk overlay kamera |
| `ui.theme` | `Color.kt`, `Theme.kt`, `Type.kt` | Konfigurasi tema Material3 |
| root | `MainActivity.kt` | Single Activity (entry point Compose) |
| root | `MyApplication.kt` | Hilt Application class |

### 2. `:core` — Domain + Data Layer

Modul yang berisi business logic, model domain, dan implementasi data.

| Package | File | Deskripsi |
|---|---|---|
| `domain.model` | `ProduceClassificationResult.kt` | Data class hasil klasifikasi (name + accuracyScore) |
| `domain.model` | `RoiConfiguration.kt` | Konfigurasi ROI (sizeFraction + verticalBias) |
| `domain.model` | `CameraRoiBenchmarkSummary.kt` | Model ringkasan benchmark |
| `domain.model` | `CameraRoiInferenceResult.kt` | Model hasil inferensi benchmark |
| `domain.repository` | `ProduceRepository.kt` | Interface repository |
| `domain.usecase` | `AnalyzeProduceUseCase.kt` | Use case utama untuk analisis gambar |
| `domain.usecase` | `GetRoiConfigurationUseCase.kt` | Use case mendapatkan konfigurasi ROI |
| `domain.usecase` | `RunCameraRoiBenchmarkUseCase.kt` | Use case menjalankan benchmark |
| `domain.usecase` | `ExportCameraBenchmarkReportUseCase.kt` | Use case export laporan benchmark |
| `data.repository` | `ProduceRepositoryImpl.kt` | Implementasi repository: ROI crop → preprocess → classify |
| `data.image` | `RoiCropper.kt` | Memotong bitmap sesuai area ROI |
| `data.image` | `RoiGeometry.kt` | Kalkulasi geometri ROI (bounds, posisi, ukuran) |
| `data.image` | `ImagePreprocessor.kt` | Resize ke 224×224 dan normalisasi [-1, 1] |
| `data.benchmark` | `CameraRoiBenchmarkRunner.kt` | Runner untuk benchmark kamera |
| `data.benchmark` | `CameraRoiBenchmarkReportExporter.kt` | Exporter laporan benchmark |
| `data.benchmark` | `BenchmarkSummaryCalculator.kt` | Kalkulator ringkasan benchmark |
| `di` | `CoreModule.kt` | Hilt DI module untuk seluruh dependency |

### 3. `:tflite_engine` — ML Infrastructure Layer

Modul yang menangani seluruh operasi machine learning menggunakan TensorFlow Lite.

| File | Deskripsi |
|---|---|
| `ProduceClassifier.kt` | Interface abstraksi untuk classifier (kontrak) |
| `TFLiteClassifier.kt` | Implementasi classifier menggunakan TFLite Play Services |
| `TFLiteResult.kt` | Data class hasil inferensi TFLite (label + confidence) |
| `assets/mobilenetv2_int8.tflite` | Model MobileNetV2 INT8 (2.60 MB) |
| `assets/labels.txt` | File label 12 kelas klasifikasi |

---

## 🛠️ Tech Stack

### Platform & Bahasa

| Teknologi | Versi | Keterangan |
|---|---|---|
| Android | SDK 29–36 | Min SDK 29 (Android 10), Target SDK 36 |
| Kotlin | 2.0.21 | Bahasa pemrograman utama |
| Gradle (AGP) | 9.0.1 | Build system |
| Java Compatibility | 11 | Source & target compatibility |

### UI & Presentation

| Library | Keterangan |
|---|---|
| Jetpack Compose (BOM) | UI toolkit deklaratif modern |
| Material Design 3 | Sistem desain Google terbaru |
| Material Icons Extended | Ikon tambahan untuk UI |
| Navigation Compose `2.9.8` | Navigasi antar-screen |
| Activity Compose `1.12.4` | Integrasi Activity dengan Compose |
| Lifecycle Runtime Compose `2.8.7` | State lifecycle-aware |

### Kamera

| Library | Versi | Keterangan |
|---|---|---|
| CameraX Core | 1.6.1 | Abstraksi kamera |
| CameraX Camera2 | 1.6.1 | Implementasi Camera2 |
| CameraX Lifecycle | 1.6.1 | Integrasi lifecycle |
| CameraX View | 1.6.1 | PreviewView |
| ExifInterface | 1.4.2 | Membaca metadata orientasi gambar |

### Machine Learning

| Library | Versi | Keterangan |
|---|---|---|
| TFLite Java (Play Services) | 16.5.0 | Runtime TFLite via Google Play Services |
| TFLite Support (Play Services) | 16.5.0 | Library pendukung (ImageProcessor, TensorImage) |
| MobileNetV2 INT8 | — | Model CNN (2.60 MB), input 224×224 |

### Dependency Injection

| Library | Versi | Keterangan |
|---|---|---|
| Dagger Hilt | 2.59.2 | DI framework |
| Hilt Navigation Compose | 1.3.0 | Integrasi Hilt dengan Navigation Compose |
| KSP | 2.2.10-2.0.2 | Kotlin Symbol Processing untuk code generation |

### Asynchronous

| Library | Versi | Keterangan |
|---|---|---|
| Kotlin Coroutines Android | 1.8.1 | Coroutines untuk Android |
| Kotlin Coroutines Play Services | 1.8.1 | Integrasi coroutines dengan Play Services |

### Testing

| Library | Versi | Jenis |
|---|---|---|
| JUnit | 4.13.2 | Unit test |
| MockK | 1.13.8 | Mocking framework |
| Turbine | 1.0.0 | Testing StateFlow/SharedFlow |
| Robolectric | 4.13 | Unit test Android tanpa emulator |
| Espresso | 3.7.0 | UI instrumented test |
| UIAutomator | 2.3.0 | End-to-end UI test |
| Compose UI Test | — | Testing Compose UI |

---

### Penjelasan Alur

1. **Splash Screen** — Aplikasi memuat dan menginisialisasi model TFLite saat pertama kali dijalankan
2. **Camera Screen** — Kamera aktif dengan analisis real-time:
   - Setiap frame diproses melalui pipeline: ROI crop → resize 224×224 → normalize [-1,1] → inferensi
   - Interval analisis adaptif (70–180ms) berdasarkan waktu inferensi sebelumnya
   - Hasil dikumpulkan dalam **sliding window 20 frame** dengan majority voting untuk stabilitas
   - Prediksi ditampilkan setelah minimal **5 votes** terkumpul
3. **Result Screen** — Gambar yang di-capture/dipilih dianalisis ulang dan ditampilkan dengan detail lengkap
4. **About Screen** — Informasi tentang aplikasi dan pembuat

---

## 🧠 Arsitektur CNN (MobileNetV2)

### Spesifikasi Model

| Parameter | Nilai |
|---|---|
| **Arsitektur** | MobileNetV2 |
| **Format** | TensorFlow Lite (INT8 quantized) |
| **Ukuran File** | 2.60 MB |
| **Input Shape** | `[1, 224, 224, 3]` (batch, height, width, RGB) |
| **Output Shape** | `[1, 12]` (probabilitas 12 kelas) |
| **Normalisasi** | `(pixel - 127.5) / 127.5` → range [-1, 1] |
| **Resize Method** | Bilinear Interpolation |
| **Runtime** | Google Play Services TFLite |
| **Thread Count** | 4 |

### Pipeline Preprocessing

```
Input Bitmap
    │
    ▼
ROI Crop (80% center square, vertical bias -0.14)
    │
    ▼
Resize ke 224×224 (Bilinear Interpolation)
    │
    ▼
Normalize: (pixel - 127.5) / 127.5 → [-1, 1]
    │
    ▼
TensorImage (FLOAT32)
    │
    ▼
InterpreterApi.run()
    │
    ▼
Output: 12 probabilitas → sorted descending
    │
    ▼
Top-1 Prediction (label + confidence)
```

---

## 📂 Struktur Direktori

```
CnnFreshScan/
├── app/                                    # Modul Presentation Layer
│   └── src/
│       ├── main/
│       │   ├── java/com/skripsi/cnnfreshscan/
│       │   │   ├── MainActivity.kt
│       │   │   ├── MyApplication.kt
│       │   │   ├── navigation/
│       │   │   │   ├── Destinations.kt
│       │   │   │   └── MainNavGraph.kt
│       │   │   ├── presentation/
│       │   │   │   ├── component/
│       │   │   │   │   └── CameraPermissionScreen.kt
│       │   │   │   ├── screen/
│       │   │   │   │   ├── AboutScreen.kt
│       │   │   │   │   ├── CameraScreen.kt
│       │   │   │   │   ├── ResultScreen.kt
│       │   │   │   │   └── SplashScreen.kt
│       │   │   │   ├── util/
│       │   │   │   │   ├── CameraRoi.kt
│       │   │   │   │   └── PredictionUiFormatter.kt
│       │   │   │   └── viewmodel/
│       │   │   │       ├── CameraBenchmarkViewModel.kt
│       │   │   │       ├── CameraViewModel.kt
│       │   │   │       └── ResultViewModel.kt
│       │   │   └── ui/theme/
│       │   │       ├── Color.kt
│       │   │       ├── Theme.kt
│       │   │       └── Type.kt
│       │   ├── res/                        # Resources (layouts, drawables, strings)
│       │   └── AndroidManifest.xml
│       ├── test/                           # Unit Tests
│       │   └── java/com/skripsi/cnnfreshscan/
│       │       ├── presentation/util/CameraRoiTest.kt
│       │       ├── presentation/viewmodel/ResultViewModelTest.kt
│       │       └── util/MainDispatcherRule.kt
│       └── androidTest/                    # Instrumented Tests
│           └── java/com/skripsi/cnnfreshscan/
│               ├── CnnFreshScanTestRunner.kt
│               └── research/CnnFreshScanBlackBoxTest.kt
│
├── core/                                   # Modul Domain + Data Layer
│   └── src/
│       ├── main/java/com/skripsi/core/
│       │   ├── data/
│       │   │   ├── benchmark/
│       │   │   │   ├── BenchmarkSummaryCalculator.kt
│       │   │   │   ├── CameraRoiBenchmarkReportExporter.kt
│       │   │   │   └── CameraRoiBenchmarkRunner.kt
│       │   │   ├── image/
│       │   │   │   ├── ImagePreprocessor.kt
│       │   │   │   ├── RoiCropper.kt
│       │   │   │   └── RoiGeometry.kt
│       │   │   └── repository/
│       │   │       └── ProduceRepositoryImpl.kt
│       │   ├── di/
│       │   │   └── CoreModule.kt
│       │   └── domain/
│       │       ├── model/
│       │       │   ├── CameraRoiBenchmarkSummary.kt
│       │       │   ├── CameraRoiInferenceResult.kt
│       │       │   ├── ProduceClassificationResult.kt
│       │       │   └── RoiConfiguration.kt
│       │       ├── repository/
│       │       │   └── ProduceRepository.kt
│       │       └── usecase/
│       │           ├── AnalyzeProduceUseCase.kt
│       │           ├── ExportCameraBenchmarkReportUseCase.kt
│       │           ├── GetRoiConfigurationUseCase.kt
│       │           └── RunCameraRoiBenchmarkUseCase.kt
│       └── test/java/com/skripsi/core/     # Unit Tests
│           ├── data/benchmark/BenchmarkSummaryCalculatorTest.kt
│           ├── data/image/RoiGeometryTest.kt
│           └── domain/usecase/AnalyzeProduceUseCaseTest.kt
│
├── tflite_engine/                          # Modul ML Infrastructure
│   └── src/main/
│       ├── java/com/skripsi/tflite_engine/
│       │   ├── ProduceClassifier.kt        # Interface classifier
│       │   ├── TFLiteClassifier.kt         # Implementasi TFLite
│       │   └── TFLiteResult.kt             # Data class hasil inferensi
│       └── assets/
│           ├── mobilenetv2_int8.tflite     # Model CNN (2.60 MB)
│           └── labels.txt                  # 12 label kelas
│
├── docs/                                   # Dokumentasi
│   ├── arsitektur_cnnfreshscan_final.svg
│   ├── arsitektur_release_cnnfreshscan.svg
│   ├── arsitektur_release_cnnfreshscan_sederhana.svg
│   ├── icon_cnnfreshscan.svg
│   └── ROI_CONFIGURATION.md
│
├── gradle/
│   └── libs.versions.toml                  # Version catalog
│
├── build.gradle.kts                        # Root build configuration
├── settings.gradle.kts                     # Module settings
├── gradle.properties                       # Gradle properties
└── README.md                               # Dokumentasi ini
```

---

## 📋 Prasyarat

Sebelum menjalankan proyek ini, pastikan telah menginstal:

| Software | Versi Minimum | Keterangan |
|---|---|---|
| **Android Studio** | Ladybug (2024.2.x) atau lebih baru | IDE untuk pengembangan Android |
| **JDK** | 17 | Java Development Kit |
| **Android SDK** | API 36 | Compile SDK |
| **Kotlin Plugin** | 2.0.21 | Termasuk dalam Android Studio |
| **Git** | Terbaru | Version control |

### Perangkat untuk Pengujian

- Perangkat Android fisik atau emulator dengan **API 29+** (Android 10 ke atas)
- **Google Play Services** terinstal (diperlukan untuk TFLite runtime)
- Kamera (untuk fitur analisis real-time)

---

## 🚀 Instalasi & Setup

### 1. Clone Repository

```bash
git clone https://github.com/papermintx/CnnFreshScan.git
cd CnnFreshScan
```

### 2. Buka di Android Studio

1. Buka **Android Studio**
2. Pilih **File → Open**
3. Arahkan ke direktori `CnnFreshScan` yang telah di-clone
4. Tunggu hingga Gradle sync selesai

### 3. Konfigurasi SDK

Pastikan `local.properties` sudah mengarah ke lokasi Android SDK yang benar:

```properties
sdk.dir=C\:\\Users\\<username>\\AppData\\Local\\Android\\Sdk
```

### 4. Sync & Build

```bash
# Sync Gradle
./gradlew.bat --refresh-dependencies

# Build debug APK
./gradlew.bat :app:assembleDebug
```

---

## ▶️ Menjalankan Aplikasi

### Melalui Android Studio

1. Hubungkan perangkat Android fisik via USB (pastikan USB Debugging aktif) atau jalankan emulator
2. Pilih device target di toolbar Android Studio
3. Klik tombol **Run** (▶) atau tekan `Shift+F10`

### Melalui Command Line

```bash
# Build dan install ke device
./gradlew.bat :app:installDebug

# Jalankan aplikasi
adb shell am start -n com.skripsi.cnnfreshscan/.MainActivity
```

### Izin yang Diperlukan

Aplikasi akan meminta izin berikut saat pertama kali dijalankan:

| Izin | Kegunaan |
|---|---|
| `CAMERA` | Mengakses kamera untuk analisis real-time |

---

## 🧪 Pengujian

### Unit Tests

```bash
# Jalankan semua unit test
./gradlew.bat test

# Unit test modul app
./gradlew.bat :app:testDebugUnitTest

# Unit test modul core
./gradlew.bat :core:testDebugUnitTest

# Test spesifik
./gradlew.bat :core:testDebugUnitTest --tests com.skripsi.core.data.image.RoiGeometryTest
./gradlew.bat :core:testDebugUnitTest --tests com.skripsi.core.domain.usecase.AnalyzeProduceUseCaseTest
./gradlew.bat :app:testDebugUnitTest --tests com.skripsi.cnnfreshscan.presentation.util.CameraRoiTest
./gradlew.bat :app:testDebugUnitTest --tests com.skripsi.cnnfreshscan.presentation.viewmodel.ResultViewModelTest
```

### Instrumented Tests (Android Device)

```bash
# Jalankan semua instrumented test
./gradlew.bat :app:connectedDebugAndroidTest
```

### Daftar Test

| Modul | Test File | Cakupan |
|---|---|---|
| `:app` | `CameraRoiTest.kt` | Kalkulasi ROI untuk overlay kamera |
| `:app` | `ResultViewModelTest.kt` | Alur klasifikasi pada ResultViewModel |
| `:core` | `RoiGeometryTest.kt` | Kalkulasi geometri dan bounds ROI |
| `:core` | `AnalyzeProduceUseCaseTest.kt` | Logika use case analisis gambar |
| `:core` | `BenchmarkSummaryCalculatorTest.kt` | Kalkulasi ringkasan benchmark |
| `:app` | `CnnFreshScanBlackBoxTest.kt` | Black-box testing end-to-end (instrumented) |

---

## 🎯 Konfigurasi ROI

Region of Interest (ROI) menentukan area gambar yang di-crop dan diproses oleh model. Konfigurasi utama berada di:

```
core/src/main/java/com/skripsi/core/domain/model/RoiConfiguration.kt
```

### Parameter

| Parameter | Default | Deskripsi |
|---|---|---|
| `sizeFraction` | `0.8f` | Ukuran ROI sebagai fraksi dari sisi terpendek (80%) |
| `verticalBias` | `-0.14f` | Posisi vertikal ROI (-0.45 = atas, 0 = tengah, 0.45 = bawah) |

### Mengubah ROI

```kotlin
// RoiConfiguration.kt
val Default = RoiConfiguration(
    sizeFraction = 0.8f,     // Ubah untuk mengatur ukuran area pindai
    verticalBias = -0.14f    // Ubah untuk mengatur posisi vertikal
)
```

> ⚠️ **Penting**: Setelah mengubah konfigurasi ROI, jalankan test terkait untuk memastikan konsistensi:
> ```bash
> ./gradlew.bat :core:testDebugUnitTest --tests com.skripsi.core.data.image.RoiGeometryTest
> ./gradlew.bat :app:testDebugUnitTest --tests com.skripsi.cnnfreshscan.presentation.util.CameraRoiTest
> ```

Untuk panduan lengkap konfigurasi ROI, lihat [ROI_CONFIGURATION.md](docs/ROI_CONFIGURATION.md).

---

## 📄 Lisensi

Proyek ini dikembangkan untuk keperluan **akademik** sebagai bagian dari penelitian tugas akhir (skripsi) di **Program Studi Informatika, Universitas Sultan Ageng Tirtayasa**.

Penggunaan kode dan model dalam proyek ini ditujukan untuk keperluan pendidikan dan penelitian.

---

<p align="center">
  <sub>Dibuat dengan ❤️ oleh <strong>Muhamad Muslih</strong> — Universitas Sultan Ageng Tirtayasa</sub>
</p>
