package com.gohj99.telewatch.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.ui.theme.TelewatchTheme

@Composable
fun SplashPasswordScreen(
    onDoneClick: (String) -> Unit,
    passwordHint: String = ""
) {
    var password by remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 15.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.Password),
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 15.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text(text = passwordHint) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
                .width(149.dp)
                .height(35.dp)
                .background(
                    color = Color.Gray,
                    shape = RoundedCornerShape(4.dp)
                ),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = { onDoneClick(password.text) },
            modifier = Modifier
                .width(148.dp)
                .height(33.dp)
                .background(
                    color = Color.Blue,
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            Text(
                text = stringResource(id = R.string.Done),
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashPasswordScreenPreview() {
    TelewatchTheme {
        SplashPasswordScreen(onDoneClick = { /*TODO*/ })
    }
}
