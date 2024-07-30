package com.gohj99.telewatch.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.ui.theme.TelewatchTheme

@Composable
fun SplashWelcomeScreen(
    modifier: Modifier = Modifier,
    onStartMessagingClick: () -> Unit,
    onSettingClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 15.dp)
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(55.dp)
                .clip(CircleShape)  // Ensure clipping
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash_icon),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .scale(1.4f)  // Adjust scale to match XML
                    .clip(CircleShape)
                    .align(Alignment.Center)
            )
            Image(
                painter = painterResource(id = R.drawable.circle),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }

        Text(
            text = stringResource(id = R.string.app_name),
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(id = R.string.Welcome_to_app),
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .height(25.dp),
            textAlign = TextAlign.Center
        )

        CustomButton(
            onClick = onStartMessagingClick,
            text = stringResource(id = R.string.Start_messaging)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(id = R.string.Setting),
            color = Color(0xFF5F92D5),
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clickable(onClick = onSettingClick),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CustomButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp)
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f)

    Box(
        modifier = modifier
            .scale(scale)  // 应用缩放到整个Box
            .size(width = 148.dp, height = 33.dp)
            .clip(shape)
            .background(Color(0xFF2397D3))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = {
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White
        )
    }
}


@Preview(showBackground = true)
@Composable
fun SplashWelcomeScreenPreview() {
    TelewatchTheme {
        SplashWelcomeScreen(
            onStartMessagingClick = {},
            onSettingClick = {}
        )
    }
}
