package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.TikIAScreen
import com.example.ui.components.TikIALoadingScreen
import com.example.ui.theme.TikIATheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      TikIATheme {
        TikIAScreen(
          modifier = Modifier.fillMaxSize(),
          onExit = { finish() }
        )
      }
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  TikIALoadingScreen(modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  TikIATheme {
    TikIALoadingScreen()
  }
}

