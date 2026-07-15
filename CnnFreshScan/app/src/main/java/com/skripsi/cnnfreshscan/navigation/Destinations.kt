package com.skripsi.cnnfreshscan.navigation

import android.net.Uri

const val SPLASH_ROUTE = "splash_route"
const val CAMERA_ROUTE = "camera_route"
const val RESULT_ROUTE = "result_route"
const val ABOUT_ROUTE = "about_route"
const val IMAGE_URI_ARG = "imageUri"

fun buildResultRoute(imageUri: String): String = "$RESULT_ROUTE/${Uri.encode(imageUri)}"

