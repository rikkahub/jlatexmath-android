package ru.noties.jlatexmath.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.noties.jlatexmath.android.app.ui.theme.JlatexmathparentTheme

class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JlatexmathparentTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LatexInlineDemo(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun LatexInlineDemo(modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.bodyLarge
    val density = LocalDensity.current
    val formula = remember {
        "\\sum_{k=0}^{20} a_kx^k\\-+\\-\\frac{1}{1+x^2}\\-=\\-\\int_0^1 t^2\\,dt\\-+\\-\\sqrt{1+\\alpha^2}\\-+\\-\\Omega"
    }


}

@Preview(showBackground = true)
@Composable
fun ComposePreview() {
    JlatexmathparentTheme {
        LatexInlineDemo()
    }
}
