package com.apptists.poplur2025.ads

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

object AdManager { fun loadBanner(adView: AdView) { adView.loadAd(AdRequest.Builder().build()) } }
