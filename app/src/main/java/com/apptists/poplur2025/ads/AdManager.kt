package com.apptists.poplur2025.ads

import android.content.Context
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

object AdManager {
    fun initialize(context: Context) {
        MobileAds.initialize(context)
    }

    fun loadBanner(adView: AdView) {
        adView.loadAd(AdRequest.Builder().build())
    }

    fun loadNativeAd(
        context: Context,
        adUnitId: String,
        onLoaded: (NativeAd) -> Unit,
        onFailed: (LoadAdError) -> Unit = {}
    ) {
        AdLoader.Builder(context, adUnitId)
            .forNativeAd(onLoaded)
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY)
                    .build()
            )
            .build()
            .loadAd(AdRequest.Builder().build())
    }
}
