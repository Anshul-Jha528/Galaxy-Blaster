package com.anshuljha.rocketblaster


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController?){
    Box(modifier = Modifier.fillMaxSize().background(color = Color.Black),
        contentAlignment = Alignment.Center) {

        Image(painter = painterResource(R.drawable.bg2),
            contentDescription = "Background",
            Modifier.fillMaxSize().fillMaxHeight())



        Image(painter = painterResource(R.drawable.game_icon), contentDescription = "Icon",
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 120.dp).size(200.dp).clip(
                CircleShape
            ))


        Text(
            text = "Galaxy Blaster - The Kitty's Adventure",
            fontSize = 45.sp,
            lineHeight = 50.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center).padding(vertical = 60.dp, horizontal = 20.dp),
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
            fontStyle = FontStyle.Normal,
            style = TextStyle(
                shadow = Shadow(
                    offset = Offset(
                        5.0f,
                        5.0f
                    ), blurRadius = 1f
                ),
                fontWeight = FontWeight.W500
            )
            )

        Image(painter = painterResource(R.drawable.brand_logo), contentDescription = "BrandLogo",
            modifier = Modifier.align(Alignment.BottomCenter).padding(100.dp).clip(RoundedCornerShape(15.dp)),
            contentScale = ContentScale.Fit)

        LaunchedEffect(Unit) {
            delay(3000L)
            navController!!.navigate("Home"){
                popUpTo("splash"){
                    inclusive = true
                }
            }
        }

    }
}

@Preview
@Composable
fun SplashScreenPreview(){
    SplashScreen(null)
}