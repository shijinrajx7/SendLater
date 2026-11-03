package com.sendlater.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sendlater.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Plays the opening only once per app launch. */
object IntroState { var shown = false }

/**
 * Marvel-style opening: your photos flash past faster and faster, a white flash,
 * then your name slams in on a red block. Tap anywhere to skip.
 */
@Composable
fun IntroScreen(maker: String, onFinish: () -> Unit) {
    val photos = remember { listOf(R.drawable.intro_1, R.drawable.intro_2, R.drawable.intro_3) }
    val aligns = remember { listOf(Alignment.TopCenter, Alignment.Center, Alignment.BottomCenter) }

    var frame by remember { mutableIntStateOf(0) }
    var phase by remember { mutableIntStateOf(0) } // 0 montage, 1 title, 2 fade out
    val titleScale = remember { Animatable(0.4f) }
    val flash = remember { Animatable(0f) }
    val fade by animateFloatAsState(if (phase == 2) 0f else 1f, tween(350), label = "fade")

    LaunchedEffect(Unit) {
        var d = 170L
        for (i in 0 until 28) {
            frame = i
            delay(d)
            if (d > 55) d -= 6
        }
        phase = 1
        launch {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(450))
        }
        titleScale.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        delay(1600)
        phase = 2
        delay(400)
        onFinish()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .alpha(fade)
            .pointerInput(Unit) { detectTapGestures { onFinish() } }
    ) {
        if (phase == 0) {
            val i = frame
            Image(
                painter = painterResource(photos[i % 3]),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = aligns[(i / 3) % 3],
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val z = 1.04f + (i % 4) * 0.05f
                        scaleX = z
                        scaleY = z
                        rotationZ = if (i % 2 == 0) -1.5f else 1.5f
                    }
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent, Color(0xCC000000)))
                )
            )
        } else {
            Image(
                painter = painterResource(photos[2]),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.28f,
                modifier = Modifier.fillMaxSize()
            )
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .graphicsLayer {
                            scaleX = titleScale.value
                            scaleY = titleScale.value
                        }
                        .background(Color(0xFFE62429))
                        .padding(horizontal = 22.dp, vertical = 8.dp)
                ) {
                    Text(
                        maker.uppercase(),
                        color = Color.White,
                        fontSize = if (maker.length > 10) 28.sp else 40.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "PRESENTS",
                    color = Color(0xCCFFFFFF),
                    fontSize = 14.sp,
                    letterSpacing = 6.sp,
                    modifier = Modifier.alpha(titleScale.value.coerceIn(0f, 1f))
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "SendLater",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.alpha(titleScale.value.coerceIn(0f, 1f))
                )
            }
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flash.value)))
        }
        Text(
            "tap to skip",
            color = Color(0x66FFFFFF),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
        )
    }
}
