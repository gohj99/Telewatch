package com.gohj99.telewatch.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.ui.theme.TelewatchTheme

@Composable
fun ErrorScreen(onRetry: () -> Unit, onSetting: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(text = "An error occurred\nPlease try again", color = Color.White,)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(id = R.string.Retry))
        }
        Button(onClick = onSetting) {
            Text(text = stringResource(id = R.string.Setting))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorScreenPreview() {
    TelewatchTheme {
        ErrorScreen({ /*TODO*/ }, { /*TODO*/ })
    }
}
