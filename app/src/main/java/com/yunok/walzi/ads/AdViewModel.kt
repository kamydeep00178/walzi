package com.yunok.walzi.ads

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
//import com.google.android.gms.ads.AdLoader
//import com.google.android.gms.ads.AdRequest
//import com.google.android.gms.ads.nativead.NativeAd
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Thin ViewModel that exposes isPremium to any screen.
 * Inject with hiltViewModel() wherever you need to show/hide ads.
 */
@HiltViewModel
class AdViewModel @Inject constructor(
) : ViewModel() {

    /*var nativeAd by mutableStateOf<NativeAd?>(null)
        private set
    fun loadAd(context: Context) {}*/

  var nativeAd by mutableStateOf<NativeAd?>(null)
        private set

    fun loadAd(context: Context) {
        if (nativeAd != null) return // prevent reload

        AdLoader.Builder(context, AdManager.NATIVE_AD_UNIT_ID)
            .forNativeAd { ad ->
                nativeAd = ad
            }
            .build()
            .loadAd(AdRequest.Builder().build())
    }


}