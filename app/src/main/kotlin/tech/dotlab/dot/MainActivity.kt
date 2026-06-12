package tech.dotlab.dot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import tech.dotlab.dot.designsystem.DotTheme
import tech.dotlab.dot.device.DeviceOverride
import tech.dotlab.dot.ui.DotApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load any developer device override before the first device resolution.
        DeviceOverride.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            DotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DotApp()
                }
            }
        }
    }
}
