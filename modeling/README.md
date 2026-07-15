# Klasifikasi Kesegaran Buah dan Sayur Menggunakan MobileNetV2

> [!NOTE]
> Project ini merupakan bagian dari penelitian skripsi yang bertujuan untuk mengklasifikasikan kesegaran (segar vs busuk) pada 6 jenis komoditas buah dan sayur menggunakan arsitektur Deep Learning **MobileNetV2** dengan metode K-Fold Cross Validation.

---

## 📂 Struktur Folder Project

```
modeling/
├── README.md                                    # Dokumentasi utama project
│
├── dataset/                                     # Dataset mentah (raw data)
│   ├── fruits/                                  # Gambar training buah
│   ├── vegetables/                              # Gambar training sayur
│   └── testings/                                # Gambar testing mentah
│
├── notebooks/                                   # Berkas Jupyter Notebook utama
│   ├── 01_local_processing.ipynb                # [Lokal] Scan data mentah, cleaning & augmentasi dataset
│   └── 02_colab_training_and_deployment.ipynb   # [Colab] Training K-Fold, evaluasi performa, & konversi TFLite
│
└── outputs/                                     # Berkas keluaran (di-generate otomatis)
    ├── data_scan/                               # Ringkasan data & plot distribusi awal
    ├── cleaned_data/                            # Citra training siap pakai (224x224, padded)
    ├── cleaned_testings/                        # Citra testing siap pakai (224x224, padded)
    └── contoh_augmentasi/                       # Contoh visualisasi augmentasi untuk dokumen skripsi
```

---

## 🛠️ Klasifikasi Kelas Dataset

Eksperimen ini menggunakan total **12 kelas** yang terbagi dalam buah dan sayur:

| Kategori Buah (Fruits) | Kategori Sayur (Vegetables) |
| :--- | :--- |
| `fresh banana` (pisang segar) | `fresh carrot` (wortel segar) |
| `fresh mango` (mangga segar) | `fresh cucumber` (timun segar) |
| `fresh orange` (jeruk segar) | `fresh tomato` (tomat segar) |
| `rotten banana` (pisang busuk) | `rotten carrot` (wortel busuk) |
| `rotten mango` (mangga busuk) | `rotten cucumber` (timun busuk) |
| `rotten orange` (jeruk busuk) | `rotten tomato` (tomat busuk) |

---

## 🚀 Alur Kerja CRISP-DM

Project ini mengikuti metodologi standar industri **CRISP-DM** (Cross-Industry Standard Process for Data Mining):

1. **Business Understanding**: Mengatasi masalah pemilahan kualitas kelayakan buah dan sayur di retail atau logistik secara otomatis.
2. **Data Understanding & Preparation (Lokal)**: Menganalisis sebaran kelas mentah, pra-pemrosesan citra dengan padding putih, augmentasi data asli, dan penyeimbangan kelas. (Jalankan [`01_local_processing.ipynb`](notebooks/01_local_processing.ipynb))
3. **Modeling, Evaluation & Deployment (Colab)**: Training model MobileNetV2 (K-Fold Cross Validation), evaluasi metrik performa (Loss, Accuracy, Confusion Matrix, Classification Report) pada test set, serta ekspor model hasil kuantisasi ke format TensorFlow Lite. (Jalankan [`02_colab_training_and_deployment.ipynb`](notebooks/02_colab_training_and_deployment.ipynb))

---

## 💻 Cara Menjalankan

### 1. Jalankan Secara Lokal (Notebook 01)

Notebook pertama dijalankan di komputer lokal untuk memproses dataset mentah.

1. Pastikan **Python 3.8 – 3.11** sudah terinstal.
2. Instal library yang dibutuhkan (disarankan menggunakan virtual environment):
   ```bash
   pip install jupyter tensorflow pillow numpy matplotlib scikit-learn
   ```
3. Jalankan Jupyter Notebook:
   ```bash
   jupyter notebook
   ```
4. Buka folder `notebooks/` dan jalankan **`01_local_processing.ipynb`** terlebih dahulu.
5. Output citra yang sudah diproses akan tersimpan otomatis di folder `outputs/`.

### 2. Training di Google Colab (Notebook 02)

Notebook kedua dirancang untuk dijalankan di **Google Colab** guna memanfaatkan GPU gratis.

1. Upload folder `outputs/cleaned_data/` dan `outputs/cleaned_testings/` ke Google Drive.
2. Buka [`02_colab_training_and_deployment.ipynb`](notebooks/02_colab_training_and_deployment.ipynb) di Google Colab.
3. Sesuaikan path Google Drive di dalam notebook, lalu jalankan seluruh sel secara berurutan.
4. Hasil training (model checkpoint & laporan evaluasi) akan tersimpan di Google Drive.
