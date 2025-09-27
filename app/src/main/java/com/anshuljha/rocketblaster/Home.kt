package com.anshuljha.rocketblaster

import android.R.attr.onClick
import android.R.attr.textColor
import android.content.Context
import android.media.MediaPlayer
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import kotlin.text.Typography.ellipsis


var coins = 0
var highScore = 0
var currentLevel = 1
var sharedPreferences : android.content.SharedPreferences? = null
var selectedRocket = 0
var rocket2Unlocked = false
var rocket3Unlocked = false
var rocket4Unlocked = false
var rocket5Unlocked = false
var unlockedRockets = mutableListOf<Int>()
var music : Boolean = true
var lastRewardTime = 0L
var member = "Basic"

fun getData(context : Context){
    sharedPreferences = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
    coins = sharedPreferences!!.getInt("coins", 0)
    highScore = sharedPreferences!!.getInt("highScore", 0)
    currentLevel = sharedPreferences!!.getInt("currentLevel", 1)
    selectedRocket = sharedPreferences!!.getInt("selectedRocket", 0)
    rocket2Unlocked = sharedPreferences!!.getBoolean("unlockedRocket2", false)
    rocket3Unlocked = sharedPreferences!!.getBoolean("unlockedRocket3", false)
    rocket4Unlocked = sharedPreferences!!.getBoolean("unlockedRocket4", false)
    member = sharedPreferences!!.getString("member", "Basic").toString()
    rocket5Unlocked = sharedPreferences!!.getBoolean("unlockedRocket5", false)
    music = sharedPreferences!!.getBoolean("music", true)
    lastRewardTime = sharedPreferences!!.getLong("lastRewardTime", 0L)
    unlockedRockets.clear()
    if(rocket2Unlocked) unlockedRockets.add(1)
    if(rocket3Unlocked) unlockedRockets.add(2)
    if(rocket4Unlocked) unlockedRockets.add(3)
    if(rocket5Unlocked) unlockedRockets.add(4)
    unlockedRockets.add(0)




}

@Composable
fun Home(navController: NavController?, context: Context?){
    getData(context!!)

    val lifecycleOwner = LocalLifecycleOwner.current

    val musicStatus = remember { mutableStateOf(music) }
    val lastRewarded = remember { mutableStateOf(lastRewardTime) }

    if(musicStatus.value) {
        val mediaPlayer = remember {
            try {
                MediaPlayer.create(context, R.raw.chill)?.apply {
                    isLooping = true
                    setVolume(0.5f, 0.5f) // Set volume to 50%
                }
            } catch (e: Exception) {
                Log.e("Game", "Error creating MediaPlayer: ${e.message}")
                null
            }
        }

        DisposableEffect(lifecycleOwner) {

            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        mediaPlayer?.start()
                    }

                    Lifecycle.Event.ON_STOP -> {
                        // You can choose to pause() here if you want music to resume
                        // when the app is back in the foreground.
                        mediaPlayer?.pause()
                    }

                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                mediaPlayer?.release()
            }

        }
    }



    val colors = remember { mutableListOf(
         Color(255, 0, 155, 120), Color(255, 10, 5, 120), Color(255, 255, 80, 120), Color(235, 0, 255, 120), Color(100, 200, 50, 120), Color(25, 20, 200, 120)
    ) }

    val width = LocalConfiguration.current.screenWidthDp.dp

    val rockets = remember {
        mutableListOf(
            (R.drawable.rocket1),
            (R.drawable.rocket2),
            (R.drawable.rocket3),
            (R.drawable.rocket4),
            (R.drawable.rocket5)
        )
    }

    val counter = remember{mutableStateOf(1)}
    val headerText = remember{mutableStateOf("Hi Companion !")}

    val selectedRocket = remember { mutableStateOf(selectedRocket) }

    val homeVisible = remember{ mutableStateOf(true) }

    val list = rememberLazyListState()
    val list2 = rememberLazyListState()
    val showDialog = remember { mutableStateOf(false) }


    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = Color.Black)
        .padding(0.dp)) {
        Image(painter = painterResource(R.drawable.bg3),
            contentDescription = "Background",
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth())
            Text(
                text = "Coins : $coins ",
                fontSize = 25.sp,
                color = Color.Cyan,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.TopStart)
                    .size(width=width/2, height = 50.dp)
            )

            Text(
                text = "High Score : $highScore ",
                fontSize = 25.sp,
                overflow = TextOverflow.Ellipsis,
                color = Color.Cyan,
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.TopEnd)
                    .size(width=width/2, height = 50.dp)
            )


        Column(modifier = Modifier
            .fillMaxSize()
            .align(Alignment.Center)
            .padding(top = 50.dp, bottom = 100.dp)) {

            Text(
                text = headerText.value,
                fontSize = 40.sp,
                fontFamily = FontFamily.Serif,
                style = TextStyle(shadow = Shadow(offset = Offset(5.0f, 10.0f), blurRadius = 3f)),
                color = Color.Yellow,
                modifier = Modifier
                    .padding(5.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Image(painter = painterResource(R.drawable.hi_kitty), contentDescription = "Kitty",
                modifier = Modifier.size(100.dp).align (Alignment.CenterHorizontally),
                contentScale = ContentScale.Fit)

            if(homeVisible.value) {
                LazyColumn(
                    modifier = Modifier
                        .padding(bottom = 20.dp)
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally),
                    state = list
                ) {

                    items(currentLevel+50) { i ->

                        Button(
                            onClick = {
                                if(i+1<=currentLevel)
                                navController?.navigate("GameScreen/${i+1}")
                            },
                            modifier = Modifier
                                .offset(x = (i % 2 * (width / (1.8).dp) + 50).dp, y = 10.dp)
                                .size(70.dp),
                            colors = ButtonColors(
                                containerColor = colors.get(i % 6),
                                contentColor = Color.White,
                                disabledContainerColor = Color.Gray,
                                disabledContentColor = Color.Black
                            )

                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                if(i+1 > currentLevel) {
                                    Image(
                                        painter = painterResource(R.drawable.lock),
                                        contentDescription = "Locked",
                                        modifier = Modifier.fillMaxSize().size(70.dp)
                                            .align(Alignment.Center),
                                        contentScale = ContentScale.Crop,
                                        colorFilter = ColorFilter.tint(Color.Black)
                                    )
                                }
                                else {
                                    Text(
                                        text = " ${i + 1}",
//                                fontSize = 25.sp,
                                        fontFamily = FontFamily.Serif,
                                        modifier = Modifier.size(70.dp).align(Alignment.Center),
                                        style = TextStyle(
                                            shadow = Shadow(
                                                offset = androidx.compose.ui.geometry.Offset(
                                                    5.0f,
                                                    10.0f
                                                ), blurRadius = 3f
                                            )
                                        )
                                    )
                                    if(i+1 == currentLevel){
                                        Image(
                                            painter = painterResource(R.drawable.levelflag),
                                            contentDescription = "flag",
                                            modifier = Modifier.fillMaxSize().size(80.dp)
                                                .align(Alignment.Center),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                            }
                        }

                    }


                }
            }

        }

        Button(onClick = {
            homeVisible.value = !homeVisible.value
            if(homeVisible.value) headerText.value = "Hi Companion !"
            else headerText.value = "Select your Rocket"
        },
            modifier = Modifier.align(Alignment.BottomCenter),
            colors = ButtonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
                disabledContainerColor = Color.Gray,
                disabledContentColor = Color.Black
            )){
            Image(painter = painterResource(rockets.get(selectedRocket.value)), contentDescription = "Rocket",
                modifier = Modifier.size(120.dp))
        }

        if(!homeVisible.value) {

            val currentTime = System.currentTimeMillis()

            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 200.dp)
            ) {

                if(currentTime - lastRewarded.value > 72000000)
                    {

                    Button(
                        onClick = {
                            showDialog.value = true
                        },
                        modifier = Modifier.padding(15.dp).fillMaxWidth(),
                        colors = ButtonColors(
                            containerColor = Color(0xFFE84C4C),
                            contentColor = Color.White,
                            disabledContainerColor = Color.Gray,
                            disabledContentColor = Color.Black
                        )
                    ) {
                        if(showDialog.value)
                            dialog(homeVisible, lastRewarded, context, showDialog)

                        Text(
                            text = "Daily Reward : !0 Coins",
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }else{
                    Spacer(Modifier.size(60.dp))
                }


                    Image(
                        painter = painterResource(if (musicStatus.value) R.drawable.music_on else R.drawable.music_off_foreground),
                        contentDescription = "Music Status",
                        modifier = Modifier.size(100.dp).background(Color.Cyan, shape = CircleShape)
                            .clip(CircleShape).align(Alignment.CenterHorizontally)
                            .clickable(
                                onClick = {
                                    musicStatus.value = !musicStatus.value
                                    val editor = sharedPreferences!!.edit()
                                    editor.putBoolean("music", musicStatus.value)
                                    editor.apply()
                                })
                    )

                if(member=="Basic"){
                    Spacer(Modifier.size(60.dp))
                }else{
                    Text(text = "You are a ${member} member.",
                        modifier = Modifier.padding(10.dp).align(Alignment.CenterHorizontally),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Yellow)
                }


            }


            LazyRow(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 150.dp, start = 20.dp, end = 20.dp),
                state = list2
            ) {
                items(5) { i ->


                    val selcolor = if (selectedRocket.value == i) Color.White else Color.Gray

                    Box(
                        modifier = Modifier
                            .size(width - 100.dp)
                            .padding(20.dp)
                            .background(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Transparent
                            )
                            .border(
                                width = 5.dp,
                                color = selcolor,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                if (i in unlockedRockets) {
                                    selectedRocket.value = i
                                    val editor = sharedPreferences!!.edit()
                                    editor.putInt("selectedRocket", i)
                                    editor.apply()
                                }
                            }, contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(rockets.get(i)),
                            contentDescription = "Rocket $i",
                            modifier = Modifier
                                .size(width - 150.dp)
                                .align(Alignment.Center)
                        )
                        if (!(i in unlockedRockets)) {
                            val cost = i * 125 + 5
                            Image(
                                painter = painterResource(R.drawable.lock),
                                contentDescription = "Locked",
                                modifier = Modifier
                                    .size(width)
                                    .align(Alignment.Center),
                                colorFilter = ColorFilter.tint(Color.Black)
                            )
                            Button(
                                onClick = {
                                    if (coins >= cost) {
                                        coins -= cost
                                        val editor = sharedPreferences!!.edit()
                                        editor.putInt("coins", coins)
                                        val key = when (i) {
                                            1 -> "unlockedRocket2"
                                            2 -> "unlockedRocket3"
                                            3 -> "unlockedRocket4"
                                            4 -> "unlockedRocket5"
                                            else -> null // Should not happen for purchasable rockets
                                        }
                                        editor.putBoolean(key, true)
                                        unlockedRockets.add(i)
                                        editor.apply()
                                        editor.commit()
                                        getData()
                                        homeVisible.value = !homeVisible.value
                                        Toast.makeText(
                                            context,
                                            "Congratulations ! Kitty got a new rocket.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Not Enough Coins",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                colors = ButtonColors(
                                    containerColor = Color.Green,
                                    contentColor = Color.White,
                                    disabledContainerColor = Color.Gray,
                                    disabledContentColor = Color.Black
                                )
                            ) {

                                Row {

                                    Text(
                                        text = "Unlock : $cost coins",
                                        fontSize = 20.sp,
                                        fontFamily = FontFamily.Serif,
                                        color = Color.Red,
                                        style = TextStyle(
                                            shadow = Shadow(
                                                offset = Offset(
                                                    2.0f,
                                                    2.0f
                                                ), blurRadius = 1f
                                            )
                                        )
                                    )

                                }

                            }
                        }
                    }
                }

            }
        }

//        navController?.navigate("GameScreen")
    }
    LaunchedEffect(Unit) {
        list.animateScrollToItem(currentLevel-1)
        list2.animateScrollToItem(selectedRocket.value)
    }
}


@Composable
fun dialog(homeVisible : MutableState<Boolean>, lastRewarded : MutableState<Long>, context: Context, dialogVisible : MutableState<Boolean>){
//    val dialogVisible = remember { mutableStateOf(true) }
    val done = remember { mutableStateOf(false) }

    val activity : MainActivity = LocalActivity.current as MainActivity

    if(dialogVisible.value){
        Dialog(
            onDismissRequest = { dialogVisible.value = false },
            properties = DialogProperties(true, true,true),
            content = {
                Column(
                    modifier = Modifier.padding(10.dp).fillMaxWidth().background(Color.Green, shape = RoundedCornerShape(15.dp)),
                ) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Loading...", Toast.LENGTH_SHORT).show()

                            val rewardEarned = mutableStateOf(false)
                            activity.showAd { status ->
                                rewardEarned.value = status
                                if(rewardEarned.value) {
                                    dialogVisible.value = false
                                    homeVisible.value = !homeVisible.value
                                    coins += 20
                                    val editor = sharedPreferences!!.edit()
                                    editor.putInt("coins", coins)
                                    editor.putLong("lastRewardTime", System.currentTimeMillis())
                                    lastRewarded.value = System.currentTimeMillis()
                                    editor.apply()
                                    getData()
                                    Toast.makeText(context, "Yippee! Kitty earned 20 coins !", Toast.LENGTH_SHORT).show()

                                }

                            }

                        },
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        colors = ButtonColors(
                            containerColor = Color.White,
                            contentColor = Color.Blue,
                            disabledContainerColor = Color.Gray,
                            disabledContentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = "Double the reward? x2",
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Serif,
                            color = Color.Blue
                        )
                    }
                    Button(
                        onClick = {
                            dialogVisible.value = false
                            homeVisible.value = !homeVisible.value
                            coins += 10
                            val editor = sharedPreferences!!.edit()
                            editor.putInt("coins", coins)
                            editor.putLong("lastRewardTime", System.currentTimeMillis())
                            lastRewarded.value = System.currentTimeMillis()
                            editor.apply()
                            getData()
                            Toast.makeText(context, "Woohoo! Kitty earned 10 coins !", Toast.LENGTH_SHORT).show()

                        },
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        colors = ButtonColors(
                            containerColor = Color.White,
                            contentColor = Color.Red,
                            disabledContainerColor = Color.Gray,
                            disabledContentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = "Keep x1",
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Serif,
                            color = Color.Red
                        )
                    }
                }
            }
        )
    }
}

@Preview
@Composable
fun HomePreview(){

//    dialog()
}