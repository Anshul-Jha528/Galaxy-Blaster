package com.anshuljha.rocketblaster

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anshuljha.rocketblaster.ui.theme.RocketBlasterTheme
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback


class MainActivity : ComponentActivity() {

    var activity : Activity = this
    var context : Context = this

    val rewardAdId = "ca-app-pub-9376656451768331/4818696264"
    var rewardedAd : RewardedAd? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.let{
            it.hide(WindowInsetsCompat.Type.statusBars())
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }


        context = getApplicationContext()
        activity = this
        setContent {
            RocketBlasterTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    control(modifier = Modifier.padding(padding), this)
                }
            }
        }
//        CoroutineScope(Dispatchers.IO).launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(this@MainActivity) {
                loadReward()
            }
//        }
    }

    fun loadReward() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            this@MainActivity,
            rewardAdId,
            adRequest,
            object : RewardedAdLoadCallback() {

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    Log.d("error", "Ad failed to load: ${adError.message}")
//                    Toast.makeText(context, "Ad failed to load. Please try again later.", Toast.LENGTH_SHORT).show()
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d("success", "Ad ready to show!")
                }

            })

    }

    fun showAd(onRewardEarned : (Boolean) -> Unit){
        if(rewardedAd == null) {
            onRewardEarned(false)
            loadReward()
            return
        }else{
        rewardedAd?.let { ad ->
            ad.show(activity) { reward ->

                onRewardEarned(true)
                Log.d("reward1", "User earned the reward.")
            }

                loadReward()


        }
        }
    }


}


@Composable
fun control(modifier: Modifier, activity : MainActivity ){
    val context = LocalContext.current
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash"){
        composable("splash"){
            SplashScreen(navController)

        }
        composable("GameScreen/{level}") {
            val level = it.arguments?.getString("level")!!.toInt()
            GameScreen(navController, level, activity)

        }
        composable("Home"){
            Home(navController, context)
        }
    }


}




@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RocketBlasterTheme {

    }
}