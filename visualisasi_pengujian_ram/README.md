# Data Hasil Pengujian Performa & Visualisasi (Skripsi)

Repositori ini berisi data mentah log benchmark, skrip pemrosesan data, serta hasil visualisasi performa aplikasi Android pada dua perangkat:
1. **Redmi Note 10 Pro** (Xiaomi M2101K6G, Android 12, API 31, RAM 6 GB, Snapdragon 732G)
2. **Redmi 9A** (Xiaomi M2006C3LG, Android 10, API 29, RAM 4 GB, Helio G25)

Pengujian dilakukan masing-masing sebanyak **3 kali sesi pengujian (runs)** untuk mengukur parameter RAM PSS, waktu startup, switch kamera, inferensi model deep learning, dan response time navigasi antar layar.

---

## 📁 Struktur Direktori

```text
├── dataset/                           # Folder data log mentah
│   ├── redmi 9a/                      # Log mentah Redmi 9A (Run 1 - 3)
│   └── redmi note 10 pro/             # Log mentah Redmi Note 10 Pro (Run 1 - 3)
│
├── output/                            # Folder hasil visualisasi & ekspor tabel
│   ├── Gambar_4.35_RAM.png            # Grafik Perbandingan Penggunaan RAM PSS
│   ├── Gambar_4.36_Response_Time.png  # Grafik Perbandingan Response Time
│   ├── Tabel_4.14_Perbandingan_Performa.csv # Tabel Perbandingan Performa (CSV)
│   └── Tabel_4.14_Perbandingan_Performa.md  # Tabel Perbandingan Performa (Markdown)
│
├── requirements.txt                   # Daftar pustaka Python yang dibutuhkan
└── Visualisasi_Performa_Skripsi.ipynb # Jupyter Notebook utama untuk analisis interaktif
```

---

## 🚀 Cara Menjalankan Notebook

Anda dapat langsung membuka file `Visualisasi_Performa_Skripsi.ipynb` di editor pilihan Anda (seperti VS Code atau Jupyter Notebook) dan memilih kernel Python Anda.

### Otomatisasi Instalasi Pustaka
Notebook ini telah dilengkapi dengan fitur **instalasi dependensi otomatis** pada sel pertama. Ketika dijalankan, notebook akan memeriksa keberadaan pustaka yang diperlukan (`pandas`, `numpy`, `matplotlib`, `seaborn`, `tabulate`) dan menginstalnya secara otomatis jika belum terpasang. Anda tidak perlu membuat atau mengaktifkan Virtual Environment (`.venv`) secara manual.

---

## 📊 Hasil Ringkasan Kinerja Utama

### Tabel Perbandingan Performa antar Perangkat (Tabel 4.14)
| No | Perangkat | Android | Chipset | RAM | Startup | Inference | Idle RAM | Peak RAM | Status |
| :---: | :--- | :--- | :--- | :---: | :---: | :--- | :---: | :---: | :---: |
| 1 | Redmi Note 10 Pro | Android 12 (API 31) | Snapdragon 732G | 6 GB | 3.53 s | 69.10 ms (Live) / 57.63 ms (Mock) | 202.93 MB | 215.86 MB | Lancar |
| 2 | Redmi 9A | Android 10 (API 29) | Helio G25 | 4 GB | 12.15 s | 73.33 ms (Live) / 73.97 ms (Mock) | 146.28 MB | 146.28 MB | Lancar |

*   **Peak RAM** didefinisikan sebagai alokasi RAM PSS tertinggi yang tercatat sepanjang seluruh pengoperasian aplikasi (termasuk saat transisi halaman).
*   **Inference** membandingkan metode aliran preview kamera langsung (Live) dengan pengujian bitmap di memori (Mock).
