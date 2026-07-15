package com.skripsi.cnnfreshscan

import androidx.test.runner.AndroidJUnitRunner

/**
 * Custom Android Test Runner for CnnFreshScan.
 *
 * Black-box tests run the real app graph through MyApplication, so the runner
 * intentionally does not replace the Application with HiltTestApplication.
 */
class CnnFreshScanTestRunner : AndroidJUnitRunner()

