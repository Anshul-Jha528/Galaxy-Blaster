package com.anshuljha.rocketblaster

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import kotlin.math.pow
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class myEnemy(val x : Float, var y : Float, val type : Int, val size : Int, val speed : Int, var xp : Int)

data class blast(val x : Float, val y : Float, val size : Int, val visibility : Float)

data class Boss(val x : Float, var y : Float, val type : Int, val size : Int, var xp : Double)

var rocketId = 4

fun getData(){
    coins = sharedPreferences!!.getInt("coins", 0)
    highScore = sharedPreferences!!.getInt("highScore", 0)
    rocketId = sharedPreferences!!.getInt("selectedRocket", 0)
    music = sharedPreferences!!.getBoolean("music", true)
}

@Composable
fun GameScreen(navController: NavController?, level : Int, mainActivity : MainActivity){

    getData()
    val editor = sharedPreferences!!.edit()

    val musicStatus = remember { mutableStateOf(music) }

    val lifecycleOwner = LocalLifecycleOwner.current

    val blastPlayer : MediaPlayer?

    if(musicStatus.value) {
        val mediaPlayer = remember {
            try {
                MediaPlayer.create(mainActivity, R.raw.space_ambience)?.apply {
                    isLooping = true
                    setVolume(0.3f, 0.3f) // Set volume to 30%
                }
            } catch (e: Exception) {
                Log.e("Game", "Error creating MediaPlayer: ${e.message}")
                null
            }
        }

        blastPlayer = remember {
            try {
                MediaPlayer.create(mainActivity, R.raw.erupter)?.apply {
                    isLooping = false
                    setVolume(0.8f, 0.8f) // Set volume to 50%
                }
            } catch (e: Exception) {
                Log.e("Blast", "Error creating MediaPlayer: ${e.message}")
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
                blastPlayer?.release()
            }

//        try {
//            mediaPlayer?.start()
//        } catch (e: Exception) {
//            Log.e("Home", "Error starting MediaPlayer: ${e.message}")
//        }
        }

    }else{
        blastPlayer = remember {null}
    }


    val score = remember{ mutableStateOf(0)}
    val lives = remember{ mutableStateOf(3)}

    val bg = remember{mutableListOf(
        (R.drawable.bg1),
        (R.drawable.bg6),
        (R.drawable.bg7),
        (R.drawable.bg4),
        (R.drawable.bg5)
    )}

    val rocket = remember{mutableListOf(
        (R.drawable.rocket1),
        (R.drawable.rocket2),
        (R.drawable.rocket3),
        (R.drawable.rocket4),
        (R.drawable.rocket5)
    )}

    val enemy = remember{mutableListOf(
        (R.drawable.rock1),
        (R.drawable.rock2),
        (R.drawable.meteor1),
        (R.drawable.meteor2),
        (R.drawable.ufo1),
        (R.drawable.ufo2)
    )}

    val isFiring = remember{mutableStateOf(true)}

    val bullet = remember{mutableListOf(
        (R.drawable.bullet1),
        (R.drawable.bullet3),
        (R.drawable.bullet2)
    )}
    var bulletSpeed = level/10 + score.value/10 + rocketId + 5
    var bulletId = remember{mutableStateOf(0)}
    var a = if(level <1000) level/10 else 100
    var bulletDelay = 350 - rocketId*20 - bulletId.value*10 - a
    var shotBullets = remember { mutableStateOf<List<Offset>>(emptyList()) }
    var lastFireTime = remember { mutableStateOf(0L) }
    var bulletType = remember { mutableStateOf(0) }
    var upgrade = remember { mutableStateOf(-1) }
    var upgraderX = remember { mutableStateOf(0) }
    var upgraderY = remember { mutableStateOf(-50) }

    val lifeVisibility = remember{mutableStateOf(0.0f)}

    var presentEnemy = remember{mutableStateOf<List<myEnemy>>(emptyList())}
    var enemyDelay = 3000L - (score.value*level/100)
    var enemySpeed = level/30 + score.value/50 + 5
    var enemyId = remember { mutableStateOf(0) }
    val isEnemyComing = remember{mutableStateOf(true)}
    val enemiesKilled = remember{mutableStateOf(0)}

    val bgId = if (level in 1..3 ) 0
              else if(level in 4..6 ) 1
        else if(level in 7..9)  2
        else if(level in 10..12)  3
        else if(level in 13..15)  4
        else  (level-1)%5

    val width = LocalConfiguration.current.screenWidthDp.dp
    val height = LocalConfiguration.current.screenHeightDp.dp

    val rocketTargetPosition = remember { mutableStateOf(Offset(width.value/2 - 75, height.value-100)) }
//    val rocketPosition = animateOffsetAsState(
//        targetValue = rocketTargetPosition.value,
//        label = "rocketPosition",
//        animationSpec = spring(stiffness = Spring.StiffnessHigh,
//            dampingRatio = Spring.DampingRatioNoBouncy)
//    )


//    rocketTargetPosition.value = pos

    val gameOver = remember{mutableStateOf(false)}
    val gameWon = remember{mutableStateOf(false)}
    val timesGameOver = remember{mutableStateOf(0)}
    val isPaused = remember{mutableStateOf(false)}

//    if(lives.value == 0){
//        isFiring.value = false
//        isEnemyComing.value = false
//        gameOver.value = true
//        timesGameOver.value += 1
//    }
    val blastList = remember{mutableStateOf<List<blast>>(emptyList())}

    val boss = remember{mutableListOf(
        R.drawable.boss3, R.drawable.boss4, R.drawable.cat, R.drawable.boss2, R.drawable.boss1
    )}

    val bossId = if(level in 1..6) 0
                else if(level in 7..12) 1
                else if(level in 13..18) 2
                else if(level in 19..24) 3
                else if(level in 25..30) 4
                else level%5

    val bossAttacks = remember{mutableListOf(
        R.drawable.bossattack1, R.drawable.bossattack2, R.drawable.bossattack3
    )}

    val resetBossTime = remember { mutableStateOf(false) }

    val bossAttackList = remember{mutableStateOf<List<myEnemy>>(emptyList())}

    val currentBoss = remember{ mutableStateOf<Boss>(
        Boss(width.value/2f, -(width.value/1.5f), bossId, (width.value/1.5 ).toInt(), 30.0+bossId*5+level/5.0)
    )}

    val currentBossAttackId = if(level in 1..21) 0
                                else if(level in 22..42) 1
                                else if(level in 43..63) 2
                                else level%3
    val currentBossAttackType = (((level-1)%21)/3).toInt()

    val bossReady = remember{ mutableStateOf(false)}

    val timerVisible = remember{mutableStateOf(true)}
    val timer = remember{mutableStateOf(3)}
    val bossgreet = remember{mutableStateOf(false)}

    
    if(!gameOver.value && !gameWon.value && isFiring.value) {

        LaunchedEffect(Unit) {
            isEnemyComing.value = false
            timerVisible.value = true
            for(i in 1..4){
                delay(1000)
                timer.value = timer.value  - 1

            }
            timerVisible.value = false
            timer.value = 3

            isEnemyComing.value = if(enemiesKilled.value >= 10+level) false
                                  else true
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black)
        ) {

            Image(
                painter = painterResource(bg.get(bgId)), contentDescription = "Background",
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
            
            Image(painter = painterResource(R.drawable.pause), contentDescription = "Pause",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = 40.dp)
                    .size(50.dp)
                    .clickable() {
                        isFiring.value = false
                        isEnemyComing.value = false
                        isPaused.value = true
                    }
                )

            Image(
                painter = painterResource(rocket.get(rocketId)), contentDescription = "Rocket",
                modifier = Modifier
                    .size(150.dp)
                    .offset(
                        x = rocketTargetPosition.value.x.dp,
                        y = rocketTargetPosition.value.y.dp
                    )
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
//                                change.consume()
                                val newX = (rocketTargetPosition.value.x + dragAmount.x / 2.5)
                                    .coerceIn(-60.0, (width.value - 30.dp.toPx()).toDouble())
                                val newY = (rocketTargetPosition.value.y + dragAmount.y / 2.5)
                                    .coerceIn(
                                        0.0,
                                        height.value.toDouble() - 25.dp.toPx()
                                    )// + 100.dp.toPx())
                                rocketTargetPosition.value =
                                    rocketTargetPosition.value.copy(
                                        x = newX.toFloat(),
                                        y = newY.toFloat()
                                    )
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                bulletId.value = (bulletId.value + 1) % 3
                                bulletDelay = 300 - rocketId * 20 - bulletId.value * 10
                            }
                        )
                    },
                contentScale = ContentScale.Fit
            )



            LaunchedEffect(Unit) {
                CoroutineScope(Dispatchers.Default).launch {
                    while (isFiring.value) {

                        if(score.value%50==0 && score.value!=0 && upgrade.value != score.value && upgraderY.value==-30){
                            upgraderX.value = Random.nextInt(0, width.value.toInt())
                            upgraderY.value = -10
                            upgrade.value = score.value
                        }
                        if(upgraderY.value!=-30)
                            upgraderY.value+=4

                        if(upgraderY.value >rocketTargetPosition.value.y.toInt() &&
                            upgraderX.value > rocketTargetPosition.value.x.toInt()-20 &&
                            upgraderX.value < rocketTargetPosition.value.x.toInt()+150 &&
                            upgraderY.value<=rocketTargetPosition.value.y.toInt()+100){
                            if(bulletId.value==0) bulletId.value = 1
                            else if(level<=30) bulletType.value = (bulletType.value + 1) % 3
                            else bulletType.value = (bulletType.value + 1) % 4
                            upgraderY.value=-30
                        }

                        if(upgraderY.value> height.value.toInt()){
                            upgraderY.value = -30
                        }


                        bulletSpeed = level/40 + score.value/90 + rocketId + 5
                        bulletDelay = 280 - rocketId*20 - bulletId.value*10 - a - score.value/90-level/12
                        enemyDelay = 2600L - (score.value*level/100)
                        enemySpeed = level/30 + score.value/100 + 5
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastFireTime.value >= bulletDelay) {
                            if(bulletId.value == 0) {
                                shotBullets.value =
                                    shotBullets.value + rocketTargetPosition.value.copy(y = rocketTargetPosition.value.y - bulletSpeed)
                            }
                            else{
                                if(bulletType.value == 0) {
                                    shotBullets.value =
                                        shotBullets.value + rocketTargetPosition.value.copy(y = rocketTargetPosition.value.y - bulletSpeed)
                                }
                                else if(bulletType.value == 1){
                                    shotBullets.value =
                                        shotBullets.value + rocketTargetPosition.value.copy(x=rocketTargetPosition.value.x - 10,y = rocketTargetPosition.value.y - bulletSpeed)
                                    shotBullets.value =
                                        shotBullets.value + rocketTargetPosition.value.copy(x=rocketTargetPosition.value.x + 10,y = rocketTargetPosition.value.y - bulletSpeed)
                                }else if(bulletType.value == 2){
                                    shotBullets.value =
                                        shotBullets.value + rocketTargetPosition.value.copy(x=rocketTargetPosition.value.x-15,y = rocketTargetPosition.value.y - bulletSpeed+10)
                                    shotBullets.value =
                                        shotBullets.value + rocketTargetPosition.value.copy(y = rocketTargetPosition.value.y - bulletSpeed)
                                    shotBullets.value =
                                        shotBullets.value + rocketTargetPosition.value.copy(x=rocketTargetPosition.value.x+15,y = rocketTargetPosition.value.y - bulletSpeed+10)
                                }else if(bulletType.value == 3){
                                    shotBullets.value =
                                        shotBullets.value + rocketTargetPosition.value.copy(x=rocketTargetPosition.value.x-20,y = rocketTargetPosition.value.y - bulletSpeed+20)
                                    shotBullets.value =
                                        shotBullets.value + rocketTargetPosition.value.copy(x=rocketTargetPosition.value.x+20,y = rocketTargetPosition.value.y - bulletSpeed+20)
                                    shotBullets.value =
                                        shotBullets.value + rocketTargetPosition.value.copy(y = rocketTargetPosition.value.y - bulletSpeed)
                                    shotBullets.value =
                                        shotBullets.value + rocketTargetPosition.value.copy(x= rocketTargetPosition.value.x-10,y = rocketTargetPosition.value.y - bulletSpeed+10)
                                    shotBullets.value =
                                        shotBullets.value + rocketTargetPosition.value.copy(x=rocketTargetPosition.value.x+10,y = rocketTargetPosition.value.y - bulletSpeed+10)
                                }
                            }


                            lastFireTime.value = currentTime
                        }

                        shotBullets.value = shotBullets.value.map { it.copy(y = it.y - bulletSpeed) }
                            .filter { it.y > 0f }
                        delay(16)
                        blastList.value =
                            blastList.value.map { it.copy(visibility = it.visibility - 0.02f) }
                                .filter { it.visibility > 0.0f }

                        if(lifeVisibility.value>0.0) lifeVisibility.value -= 0.01f

                        if(enemiesKilled.value>=10+level){
                            bossReady.value = true
                            isEnemyComing.value = false
                        }

                    }

                }
            }

            if(upgraderY.value !=-30){
                Image(painter = painterResource(R.drawable.upgrade), contentDescription = "Upgrade",
                    modifier = Modifier
                        .size(60.dp)
                        .offset(x = upgraderX.value.dp, y = upgraderY.value.dp),
                    contentScale = ContentScale.Fit)
            }

            shotBullets.value.forEach { position ->
                Image(
                    painter = painterResource(bullet.get(bulletId.value)),
                    contentDescription = "Bullet",
                    modifier = Modifier
                        .size(30.dp)
                        .offset(x = position.x.dp + 60.dp, y = position.y.dp),
                    contentScale = ContentScale.Crop
                )

            }

            var lastEnemyTime = remember { mutableStateOf(0L) }
            LaunchedEffect(Unit) {
                CoroutineScope(Dispatchers.Default).launch {
                    while(isFiring.value) {
                        while (isEnemyComing.value) {
                            delay(16)
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastEnemyTime.value >= enemyDelay) {
                                presentEnemy.value += (
                                        myEnemy(
                                            Random.nextInt(0, width.value.toInt()).toFloat(),
                                            -300f,
                                            enemyId.value,
                                            300,
                                            enemySpeed,
                                            5
                                        )
                                        )

                                enemyDelay = if(level>50) Random.nextLong(400L, 2600L-score.value/100)
                                    else 2600L-score.value/100
                                lastEnemyTime.value = currentTime
                            }

                            if (level in 1..5) enemyId.value = 0
                            else if (level in 6..10) {
                                if (score.value > 10) enemyId.value = Random.nextInt(0, 5) % 2
                            } else if (level in 11..15) {
                                if (score.value > 10) enemyId.value = Random.nextInt(0, 5) % 3
                            } else if (score.value > 5) {
                                enemyId.value = Random.nextInt(0, 6)
                            }


                            presentEnemy.value =
                                presentEnemy.value.map { it.copy(y = it.y + it.speed) }
                                    .filter {( it.xp > 0 && (it.y<(height.value -20)))}

                        }
                    }

                }
            }
            val bulletsToRemove = remember { mutableListOf<Offset>() }

            LaunchedEffect(Unit) {
                CoroutineScope(Dispatchers.Default).launch {
                    while (isFiring.value) {
                        val updatedEnemies = presentEnemy.value.map { e ->
                            val wasHit = shotBullets.value.any { b ->
                                if (isBulletHittingEnemy(b, e)) {
                                    bulletsToRemove += b
                                    true
                                } else false
                            }

                            if (wasHit) {
                                if (e.xp == 1) {
                                    enemiesKilled.value += 1
                                    blastList.value += blast(e.x, e.y, e.size, 1.0f)
                                    try {
                                        blastPlayer?.start()
                                    }catch (e:Exception){
                                        null
                                    }
                                }
                                score.value += 1
                                e.copy(xp = e.xp - 1)

                            } else e
                        }.filter { it.xp > 0 }

                        presentEnemy.value = updatedEnemies
                        shotBullets.value = shotBullets.value.filterNot { it in bulletsToRemove }
                        bulletsToRemove.clear()
                        delay(16)
                        presentEnemy.value.any { e ->
                            if(e.type == 0){
                                if (e.y + e.size/2.5 > rocketTargetPosition.value.y.toInt() && e.y<rocketTargetPosition.value.y+20 && e.x > rocketTargetPosition.value.x.toInt()  && e.x < rocketTargetPosition.value.x.toInt() + 130) {
                                    presentEnemy.value = presentEnemy.value.filterNot { it == e }
                                    blastList.value += blast(e.x, e.y, e.size, 1.0f)
                                    try {
                                        blastPlayer?.start()
                                    }catch (e:Exception){
                                        null
                                    }
                                    delay(100L)
                                    lives.value = lives.value - 1
                                    lifeVisibility.value = 1.0f

                                    if (lives.value <= 0) {
                                        gameOver.value = true
                                        isFiring.value = false
                                        timesGameOver.value += 1
                                        isEnemyComing.value = false
                                        presentEnemy.value = emptyList()
                                    }
                                }
                            }
                            else if (e.y + e.size/1.5 > rocketTargetPosition.value.y.toInt() && e.y<rocketTargetPosition.value.y+40 && e.x > rocketTargetPosition.value.x.toInt()  && e.x < rocketTargetPosition.value.x.toInt() + 130) {
                                presentEnemy.value = presentEnemy.value.filterNot { it == e }
                                blastList.value += blast(e.x, e.y, e.size, 1.0f)
                                try {
                                    blastPlayer?.start()
                                }catch (e:Exception){
                                    null
                                }
                                delay(100L)
                                lives.value = lives.value - 1
                                lifeVisibility.value = 1.0f

                                if (lives.value <= 0) {
                                    gameOver.value = true
                                    isFiring.value = false
                                    timesGameOver.value += 1
                                    isEnemyComing.value = false
                                    presentEnemy.value = emptyList()
                                }
                            }

                            false
                        }
                    }

                }
            }

            presentEnemy.value.forEach { e ->
                Image(
                    painter = painterResource(enemy.get(e.type)), contentDescription = "Enemy",
                    modifier = Modifier
                        .size(e.size.dp)
                        .offset(x = e.x.dp - e.size.dp / 2, y = e.y.dp)
                )
            }

            blastList.value.forEach { b ->
                Image(
                    painter = painterResource(R.drawable.collide), contentDescription = "Blast",
                    modifier = Modifier
                        .size(b.size.dp)
                        .offset(x = b.x.dp - b.size.dp / 2, y = b.y.dp)
                        .alpha(b.visibility)
                )
            }

            var lastBossAttackTime = remember { mutableStateOf(0L) }
            val bossAttackDelay = 900L - Math.min(level*2, 100)
            var counter = 0
            var lastMovetime = remember { mutableStateOf(0L) }
            val attackStartTime = remember { mutableStateOf(0L) }
            var shift=0

            LaunchedEffect(Unit) {
                CoroutineScope(Dispatchers.Default).launch {
                    while(isFiring.value) {
                        while (bossReady.value and !isPaused.value and !gameOver.value) {
                            if(resetBossTime.value){
                                attackStartTime.value = System.currentTimeMillis()-10000L
                                resetBossTime.value = false
                                bossAttackList.value = emptyList()
                            }
                            delay(16)
                            if (currentBoss.value.y < 0) {
                                currentBoss.value.y = currentBoss.value.y + 1f
                                bossgreet.value = true
                                attackStartTime.value = System.currentTimeMillis()
                            } else {
                                bossgreet.value = false
                                val isHit = shotBullets.value.any { b ->
                                    val dist = kotlin.math.sqrt(
                                        (width.value.toDouble()/2.0 - b.x.toDouble())
                                            .pow(2) + (currentBoss.value.y - b.y.toDouble()).pow(2)
                                    )
                                    if (dist < currentBoss.value.size/1.8) {
                                        shotBullets.value = shotBullets.value.filterNot { it == b }
                                        currentBoss.value.xp = currentBoss.value.xp - 1
                                        blastList.value+=blast(b.x, b.y, 10,0.5f)
                                    }
                                    dist < currentBoss.value.size/1.5f
                                }

                                val dist2= kotlin.math.sqrt((width.value.toDouble()/2.0-rocketTargetPosition.value.x).pow(2)+
                                        (rocketTargetPosition.value.y.toDouble()-currentBoss.value.y.toDouble()).pow(2))
                                if(dist2<currentBoss.value.size/1.8){
                                    delay(500)
                                    lives.value=0
                                    lifeVisibility.value = 1.0f
                                    try {
                                        blastPlayer?.start()
                                    }catch (e:Exception){
                                        null
                                    }
                                }

                                // type 0
                                if(currentBossAttackType==0){
                                    val size = 45

                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastBossAttackTime.value >= bossAttackDelay &&
                                        System.currentTimeMillis()-attackStartTime.value<60000
                                    ) {
                                        val space = width.value.toDouble()/10
                                        var i= space/7
                                        while(i<width.value.toDouble()){
                                            val attack = myEnemy(i.toFloat(), height.value.toFloat()/3, currentBossAttackId, size, 6,3)
                                            bossAttackList.value+=attack
                                            i+=space
                                        }
                                        counter++
                                        lastBossAttackTime.value = currentTime
                                    }
                                    if(System.currentTimeMillis()-lastMovetime.value>50) {
                                        val newBossAttackList =
                                            bossAttackList.value.map { it.copy(y = it.y + it.speed) }
                                                .filter { (it.xp > 0 && (it.y < (height.value))) }
                                        bossAttackList.value = newBossAttackList
                                        lastMovetime.value = System.currentTimeMillis()
                                    }

                                }

                                // type 1
                                if(currentBossAttackType==1){
                                    Log.d("AttackType", "1")
                                    val size = 45

                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastBossAttackTime.value >= bossAttackDelay-30 &&
                                        System.currentTimeMillis()-attackStartTime.value<60000
                                    ) {
                                        val space = width.value.toDouble()/10
                                        var i= if(shift==0) space/7 else space/7+space/2
                                        while(i<width.value.toDouble()){
                                            val attack = myEnemy(i.toFloat(), height.value.toFloat()/3, currentBossAttackId, size, 6,3)
                                            bossAttackList.value+=attack
                                            i+=space
                                        }
                                        shift = (shift+1)%2
                                        lastBossAttackTime.value = currentTime
                                    }
                                    if(System.currentTimeMillis()-lastMovetime.value>50) {
                                        val newBossAttackList =
                                            bossAttackList.value.map { it.copy(y = it.y + it.speed) }
                                                .filter { (it.xp > 0 && (it.y < (height.value))) }
                                        bossAttackList.value = newBossAttackList
                                        lastMovetime.value = System.currentTimeMillis()
                                    }

                                }

                                // type 2
                                if(currentBossAttackType==2){
                                    Log.d("AttackType", "2")
                                    val size = 45

                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastBossAttackTime.value >= bossAttackDelay-30 &&
                                        System.currentTimeMillis()-attackStartTime.value<60000
                                    ) {
                                        val space = width.value.toDouble()/10
                                        var i= space/7
                                        while(i<width.value.toDouble()/2-space){
                                            val attack = myEnemy(i.toFloat(), height.value.toFloat()/4+i.toFloat(), currentBossAttackId, size, 6,3)
                                            bossAttackList.value+=attack
                                            val attack2 = myEnemy(width.value-i.toFloat()-space.toFloat()-10.toFloat(), height.value/4+i.toFloat(), currentBossAttackId, size, 6,3)
                                            bossAttackList.value+=attack2
                                            i+=space
                                        }
                                        val attack = myEnemy(i.toFloat(), height.value.toFloat()/4+i.toFloat(), currentBossAttackId, size, 6,3)
                                        bossAttackList.value+=attack
                                        shift = (shift+1)%2
                                        lastBossAttackTime.value = currentTime
                                    }
                                    if(System.currentTimeMillis()-lastMovetime.value>50) {
                                        val newBossAttackList =
                                            bossAttackList.value.map { it.copy(y = it.y + it.speed) }
                                                .filter { (it.xp > 0 && (it.y < (height.value))) }
                                        bossAttackList.value = newBossAttackList
                                        lastMovetime.value = System.currentTimeMillis()
                                    }

                                }

                                // type 3
                                if(currentBossAttackType==3){
                                    Log.d("AttackType", "3")
                                    val size = 45

                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastBossAttackTime.value >= 200 &&
                                        System.currentTimeMillis()-attackStartTime.value<60000
                                    ) {
                                        val space = width.value.toDouble()/10
                                        var i= space/7
//                                        while(i<width.value.toDouble()/2-space){
                                            val attack = myEnemy(i.toFloat(), height.value.toFloat()/3, currentBossAttackId, size, 6,3)
                                            bossAttackList.value+=attack
                                            val attack2 = myEnemy(width.value-i.toFloat()-space.toFloat(), height.value/3+space.toFloat(), currentBossAttackId, size, 6,3)
                                            bossAttackList.value+=attack2
                                            i+=space
//                                        }
                                        lastBossAttackTime.value = currentTime
                                    }
                                    if(System.currentTimeMillis()-lastMovetime.value>10) {
                                        val newBossAttackList =
                                            bossAttackList.value.map { it.copy(
                                                x = it.x + it.speed*(if(it.y==height.value.toFloat()/3) 1 else -1)
                                            )
                                            }
                                                .filter { (it.xp > 0 && (it.x in -50.toFloat()..width.value+50)) }
                                        bossAttackList.value = newBossAttackList
                                        lastMovetime.value = System.currentTimeMillis()
                                    }

                                }

                                // type 4
                                if(currentBossAttackType==4){
                                    Log.d("AttackType", "4")
                                    val size = 45

                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastBossAttackTime.value >= bossAttackDelay-30 &&
                                        System.currentTimeMillis()-attackStartTime.value<60000
                                    ) {
                                        val space = width.value.toDouble()/8
                                        var i=  space/7 - width.value
                                        while(i<width.value.toDouble()){
                                            val attack = myEnemy(i.toFloat(), height.value.toFloat()/3, currentBossAttackId, size, 3,3)
                                            bossAttackList.value+=attack
                                            i+=space
                                        }
                                        shift = (shift+1)%2
                                        lastBossAttackTime.value = currentTime
                                    }
                                    if(System.currentTimeMillis()-lastMovetime.value>50) {
                                        val newBossAttackList =
                                            bossAttackList.value.map { it.copy(y = it.y + it.speed , x=it.x+it.speed) }
                                                .filter { (it.xp > 0 && (it.y < (height.value-120))) }
                                        bossAttackList.value = newBossAttackList
                                        lastMovetime.value = System.currentTimeMillis()
                                    }

                                }

                                // type 5
                                if(currentBossAttackType==5){
                                    Log.d("AttackType", "5")
                                    val size = 45

                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastBossAttackTime.value >= bossAttackDelay/2 &&
                                        System.currentTimeMillis()-attackStartTime.value<60000
                                    ) {
                                        val space = width.value.toDouble()/10
                                        var i= space/7
//                                        while(i<width.value.toDouble()/2-space){
                                        val attack = myEnemy(i.toFloat(), height.value.toFloat()/3, currentBossAttackId, size, 3,3)
                                        bossAttackList.value+=attack
                                        val attack2 = myEnemy(width.value-i.toFloat()-space.toFloat(), height.value.toFloat()/3, currentBossAttackId, size, 3,3)
                                        bossAttackList.value+=attack2
                                        i+=space
//                                        }
                                        lastBossAttackTime.value = currentTime
                                    }
                                    if(System.currentTimeMillis()-lastMovetime.value>10) {
                                        val newBossAttackList =
                                            bossAttackList.value.map { it.copy(
                                                x =if(it.x <width.value/2-size/2 || it.x>width.value/2+size/2){ it.x + it.speed*(if(it.x <width.value/2-size) 1 else -1)}
                                                        else it.x,
                                                        y= it.y+it.speed)

                                            }
                                                .filter { (it.xp > 0 && (it.x in -50.toFloat()..width.value+50)) }
                                        bossAttackList.value = newBossAttackList
                                        lastMovetime.value = System.currentTimeMillis()
                                    }

                                }

                                // type 6 - Rotating barrier around boss
                                if(currentBossAttackType == 6) {
                                    Log.d("AttackType", "13")
                                    val size = 45

                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastBossAttackTime.value >= 2.5*bossAttackDelay &&
                                        System.currentTimeMillis() - attackStartTime.value < 60000
                                    ) {
                                        val rotationAngle = (System.currentTimeMillis() / 20) % 360
                                        bossAttackList.value = emptyList()
                                        for(i in 0..17) {
                                            val angle = rotationAngle + i * 20
                                            val radians = Math.toRadians(angle.toDouble())
                                            val x = width.value/2 + 180 * cos(radians) -size/2
                                            val y = height.value/4 + 180 * sin(radians) -size/2

                                            val attack = myEnemy(x.toFloat(), y.toFloat(), currentBossAttackId, size, 0, 3)
                                            bossAttackList.value += attack
                                        }
                                        for(i in 0..17) {
                                            val angle = rotationAngle + i * 20
                                            val radians = Math.toRadians(angle.toDouble())
                                            val x = width.value/2 + 180 * cos(radians) -size/2
                                            val y = height.value/4 + 180 * sin(radians) -size/2

                                            val attack = myEnemy(x.toFloat(), y.toFloat(), currentBossAttackId, size, 0, 3)
                                            bossAttackList.value += attack
                                        }
                                        lastBossAttackTime.value = currentTime
                                    }

                                    if(System.currentTimeMillis() - lastMovetime.value > 16) {
                                        // Update rotation continuously
                                        val speed = Math.max(12, 20-2*level/10)
                                        val rotationAngle = (System.currentTimeMillis() / speed) % 360
                                        val newBossAttackList = bossAttackList.value.mapIndexed { index, attack ->
                                            val angle = rotationAngle + (index % 18) * 20
                                            val radians = Math.toRadians(angle.toDouble())
                                            val newX = width.value/2 + 180 * cos(radians) - size/2
                                            val newY = height.value/4 + 180 * sin(radians) - size/2

                                            attack.copy(x = newX.toFloat(), y = newY.toFloat())
                                        }.filter { it.xp > 0 }

                                        bossAttackList.value = newBossAttackList
                                        lastMovetime.value = System.currentTimeMillis()
                                    }
                                }


                                // bullet and attack interaction
                                val updatedEnemies = bossAttackList.value.map { e ->
                                    val wasHit = shotBullets.value.any { b ->
                                        if (
                                            b.y in e.y..e.y + e.size &&
                                            b.x+70 in e.x..e.x + e.size
                                        ) {
                                            Log.d("shot", "${b.x},${b.y}, ${e.x},${e.y}")
                                            shotBullets.value = shotBullets.value.filterNot {
                                                it == b
                                            }
                                            true
                                        } else false
                                    }

                                    if (wasHit) {
                                        if (e.xp == 1) {
                                            blastList.value += blast(e.x, e.y, e.size, 1.0f)
                                            score.value += 1
                                            try {
                                                blastPlayer?.start()
                                            }catch (e:Exception){
                                                null
                                            }
                                        }
                                        e.copy(xp = e.xp - 1)

                                    } else e
                                }.filter { it.xp > 0 }

//                                shotBullets.value = shotBullets.value.filterNot { it in bulletsToRemove }
//                                bulletsToRemove.clear()

                                bossAttackList.value = updatedEnemies

                                // attack and rocket interaction
                                bossAttackList.value.any { attack ->
                                    if (attack.y + attack.size > rocketTargetPosition.value.y.toInt() &&
                                        attack.y < rocketTargetPosition.value.y + 100 &&
                                        attack.x > rocketTargetPosition.value.x.toInt()+40 &&
                                        attack.x < rocketTargetPosition.value.x.toInt() + 80) {

                                        Log.d("hit", "${attack.x},${attack.y}, ${rocketTargetPosition.value.x},${rocketTargetPosition.value.y}")
                                        bossAttackList.value = bossAttackList.value.filterNot { it == attack }
                                        blastList.value += blast(attack.x, attack.y, attack.size, 1.0f)
                                        if(!isPaused.value and !gameOver.value) {
                                            lives.value -= 1
                                            lifeVisibility.value = 1.0f
                                        }
                                        if (lives.value <= 0) {
                                            bossAttackList.value = emptyList()
                                            if(!gameOver.value and !isPaused.value) {
                                                bossAttackList.value = emptyList()
                                                timesGameOver.value += 1
                                                blastList.value = emptyList()
                                                gameOver.value = true
                                                isFiring.value = false
                                            }
                                        }
                                        true
                                    } else false
                                }



//                                if (isHit) {
//                                    currentBoss.value.xp = currentBoss.value.xp - 1
//                                }
                                if (currentBoss.value.xp <= 0.0) {
                                    blastList.value += blast(width.value.toFloat()/2.0f, 10.0f, (width.value.toInt()).toInt(), 1.0f)
                                    currentBoss.value.y = -1000f
                                    try {
                                        blastPlayer?.start()
                                    }catch (e:Exception){
                                        null
                                    }
                                    delay (2000)
                                    gameWon.value = true
                                    isFiring.value = false
                                    isEnemyComing.value = false
                                }
                            }
                        }
                    }
                }
            }

            Image(
                painter = painterResource(boss.get(currentBoss.value.type)),
                contentDescription = "Boss",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(currentBoss.value.size.dp)
                    .offset(
                        x = currentBoss.value.x.dp - currentBoss.value.size.dp / 2,
                        y = currentBoss.value.y.dp
                    )
            )

            bossAttackList.value.forEach{ e ->
                Image(painter = painterResource(bossAttacks.get(e.type)), contentDescription = "Boss Attack",
                    modifier = Modifier
                        .size(e.size.dp)
                        .offset(x = e.x.dp, y = e.y.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text = "Score : ${score.value}",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp),
                color = Color.White,
                fontSize = 30.sp
            )

            Text(
                text = "Lives : ${lives.value}",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp),
                color = Color.White,
                fontSize = 30.sp
            )

            AnimatedVisibility(
                visible = lifeVisibility.value>0.1,
                modifier = Modifier.align(Alignment.Center),
                enter = fadeIn() + expandIn(),
                exit = fadeOut() + shrinkOut()
            ) {
                Image(painter = painterResource(R.drawable.heart), contentDescription = "Life",
                    modifier = Modifier
                        .size(180.dp)
                        .alpha(lifeVisibility.value),
                    contentScale = ContentScale.Fit)

            }

                AnimatedVisibility(
                    visible = bossgreet.value,
                    modifier = Modifier.align (Alignment.Center),
                    enter = fadeIn() + expandIn(),
                    exit = fadeOut() + shrinkOut()
                ) {
                    Text(
                        text = "Boss Fight",
                        color = Color.White,
                        fontSize = 50.sp,
                        modifier = Modifier.align(Alignment.Center),
                        fontFamily = FontFamily.Cursive
                    )
                }

            AnimatedVisibility(
                visible = timerVisible.value,
                modifier = Modifier.align(Alignment.Center),
                enter = fadeIn() + expandIn(),
                exit = fadeOut() + shrinkOut()
                ) {
                Text(text = "${timer.value}",
                    fontSize = 50.sp,
                    color = Color.Yellow)
            }

            val xpText = remember { mutableStateOf("Remaining enemies : ${10+level-enemiesKilled.value}") }
            if(enemiesKilled.value<10+level) xpText.value = "Remaining enemies : ${10+level-enemiesKilled.value}"
            if(bossReady.value){
                xpText.value = "XP : ${currentBoss.value.xp.toInt()}"
            }
            AnimatedVisibility(
                visible = true,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = if(bossReady.value)20.dp else 35.dp),
                enter = fadeIn() + expandIn(),
                exit = fadeOut() + shrinkOut()
            ) {
                Text(text = xpText.value,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    color = Color.Yellow,
                    modifier = Modifier.padding(2.dp))
            }

        }
    }

    val greetWin = remember{mutableListOf(
        "Well played", "You won", "Awesome", "Fantastic", "Fabulous", "Congrats"
    )}

    val greetLose = remember{mutableListOf(
        "Game over", "You lost", "Try again", "Better luck next time"
    )}
    var greetId=0
    LaunchedEffect(Unit){
        greetId = Random.nextInt(1, 20)

    }

    if(gameOver.value) {

        resetBossTime.value=true
        bossAttackList.value = emptyList()

        val reqCoins = timesGameOver.value * 5

//        mainActivity.loadReward()



        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(bg.get(bgId)), contentDescription = "background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Image(painter = painterResource(R.drawable.kitty_lose), contentDescription = "Lose",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(45.dp)
                    .size(180.dp))

            Text(
                text = greetLose.get(greetId % greetLose.size),
                fontSize = 60.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Cursive,
                color = Color.Yellow,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = -200.dp)
            )

            var text = remember { mutableStateOf("Play with ${reqCoins} coins ")}

            Button(onClick = {
                if(coins >= reqCoins) {
                    gameOver.value = false
                    isFiring.value = true
                    lives.value = 1
                    rocketTargetPosition.value = (Offset(width.value/2 - 75, height.value-100))
                    isEnemyComing.value = true
                    coins -= reqCoins
                    editor.putInt("coins", coins)
                    editor.apply()
                    bossAttackList.value = emptyList()
                }else{
                    text.value="Not enough coins"
                }
            },
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 30.dp),
                colors = ButtonColors(
                    containerColor = Color.Green,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.Black
                )
                ){

                Text(text = text.value,
                    color = Color.Red,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif)
            }

            Button(onClick = {

                Toast.makeText(mainActivity, "Loading...", Toast.LENGTH_SHORT).show()

                val rewardEarned = mutableStateOf(false)
                mainActivity.showAd { status ->
                    rewardEarned.value = status
                    Log.d("reward", "Reward earned: $status")
                    if(rewardEarned.value){
                        gameOver.value = false
                        isPaused.value = true
                        rocketTargetPosition.value = (Offset(width.value/2 - 75, height.value-100))
                        lives.value = 1
                        bossAttackList.value = emptyList()
                        isEnemyComing.value = true}
                }
//                if(rewardEarned.value){
//                    gameOver.value = false
//                    isPaused.value = true
//                    lives.value = 1
//                    rocketTargetPosition.value = (Offset(width.value/2 - 75, height.value-100))
//                    isEnemyComing.value = false
//
//            }

            },
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 100.dp),
                colors = ButtonColors(
                    containerColor = Color.Green,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.Black
                )
                ){

                Text(text = "Watch an ad to continue ",
                    color = Color.Red,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif)


            }

            Button(onClick = {
//                navController?.clearBackStack()
                    navController?.navigate("Home"){
                    popUpTo(0) {
                        inclusive = true
                    }
                }
            },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -30.dp)
                    .size(100.dp),
                colors = ButtonColors(
                    containerColor = Color.Blue,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.Black
                )
            ) {
                Image(painter = painterResource(R.drawable.home), contentDescription = "Home",
                    modifier = Modifier.size(80.dp))
            }

            
        }
    }

    if(gameWon.value) {

        val rewardText = remember { mutableStateOf("Reward Earned : 5 Coins") }

        if(score.value>highScore){
            highScore = score.value
            editor.putInt("highScore", highScore)
            editor.apply()
        }
        if(currentLevel == level){
            currentLevel += 1
            editor.putInt("currentLevel", currentLevel)

            if(currentLevel==25){
                coins+=20
                editor.putString("Member", "Bronze")
                rewardText.value = "Earned Bronze Badge & 20 Coins"
                Toast.makeText(mainActivity, "Interesting ! Kitty become a Bronze member.", Toast.LENGTH_LONG).show()
            }
            if(currentLevel==50){
                coins+=45
                editor.putString("Member", "Silver")
                rewardText.value = "Earned Silver Badge & 50 Coins"
                Toast.makeText(mainActivity, "Exciting ! Kitty become a Silver member.", Toast.LENGTH_LONG).show()
            }
            if(currentLevel==100){
                coins+=95
                editor.putString("Member", "Gold")
                rewardText.value = "Earned Gold Badge & 100 Coins"
                Toast.makeText(mainActivity, "Amazing ! Kitty become a Gold member.", Toast.LENGTH_LONG).show()
            }
            editor.apply()

        }


        coins += 5
        editor.putInt("coins", coins)
        editor.apply()

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(bg.get(bgId)), contentDescription = "background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Image(painter = painterResource(R.drawable.kitty_win), contentDescription = "Win",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(45.dp)
                    .size(180.dp))

            Text(
                text = greetWin.get(greetId % greetWin.size),
                fontSize = 60.sp,
                fontFamily = FontFamily.Cursive,
                color = Color.Yellow,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = -200.dp)
            )
            Image(
                painter = painterResource(R.drawable.trophy), contentDescription = "trophy",
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = -40.dp)
                    .size(250.dp),
                contentScale = ContentScale.Crop
            )

            Text(
                text = score.value.toString(),
                fontSize = 60.sp,
                fontFamily = FontFamily.SansSerif,
                color = Color.Blue,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = -90.dp)
            )

            Text(
                text = "Enemies killed : ${enemiesKilled.value}\n${rewardText.value}",
                fontSize = 30.sp,
                lineHeight = 40.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 100.dp)
            )

            Button(onClick = {
                val nextlevel = level+1
                navController?.navigate("Home"){
                    popUpTo(0) {
                        inclusive = true
                    }
                }
                navController?.navigate("GameScreen/${nextlevel}")
            },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -140.dp)
                    .size(100.dp),
                colors = ButtonColors(
                    containerColor = Color.Green,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.Black
                )
            ) {
                Image(painter = painterResource(R.drawable.play), contentDescription = "Play",
                    modifier = Modifier.size(80.dp))
            }

            Button(onClick = {
                navController?.navigate("Home"){
                    popUpTo(0) {
                        inclusive = true
                    }
                }
            },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -30.dp)
                    .size(100.dp),
                colors = ButtonColors(
                    containerColor = Color.Blue,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.Black
                )
                ) {
                Image(painter = painterResource(R.drawable.home), contentDescription = "Home",
                    modifier = Modifier.size(80.dp))
            }
        }
    }

    if(isPaused.value){//pause

        resetBossTime.value=true

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(bg.get(bgId)), contentDescription = "background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Image(painter = painterResource(R.drawable.water_break), contentDescription = "Break",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(50.dp)
                    .size(230.dp))

            Text(
                text = "Game Paused",
                fontSize = 60.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Cursive,
                color = Color.Yellow,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = -150.dp)
            )

            Button(
                onClick = {
                    isFiring.value = true
                    if(!bossReady.value){
                        isEnemyComing.value = true
                    }
                    rocketTargetPosition.value = (Offset(width.value/2 - 75, height.value-100))
                    isPaused.value = false
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 30.dp)
                    .size(100.dp),
                colors = ButtonColors(
                    containerColor = Color.Green,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.Black
                )
            ) {

                Image( painter = painterResource(R.drawable.play), contentDescription = "Play",
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Crop
                    )


            }


            Button(
                onClick = {
                    navController?.navigate("Home"){
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -30.dp)
                    .size(100.dp),
                colors = ButtonColors(
                    containerColor = Color.Blue,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.Black
                )
            ) {
                Image(
                    painter = painterResource(R.drawable.home), contentDescription = "Home",
                    modifier = Modifier.size(80.dp)
                )
            }


        }
    }

}


fun isBulletHittingEnemy(bullet: Offset, enemy: myEnemy): Boolean {
    if(enemy.y<=-50) return false
    val bulletSize = 30f
    val bulletX = bullet.x
    val bulletY = bullet.y+10f


    val halfEnemySize = enemy.size / 2f
    val enemyX = enemy.x
    val enemyY = enemy.y
    val dx = bulletX - enemyX + halfEnemySize/2-10
    val dy = bulletY - enemyY
    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
    return distance < halfEnemySize/2.5
}

@Preview
@Composable
fun GameScreenPreview(){
//    GameScreen(null, 1, null)
}
