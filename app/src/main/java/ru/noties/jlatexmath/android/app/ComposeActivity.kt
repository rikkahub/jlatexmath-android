package ru.noties.jlatexmath.android.app

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import ru.noties.jlatexmath.JLatexMathSplitter
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
    val formula = remember {
        "\\sum_{k=0}^{20} a_k x^k + \\frac{1}{1+x^2} = \\int_0^1 t^2 \\, dt + \\sqrt{1+\\alpha^2} + \\Omega"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        val density = LocalDensity.current
        val color = MaterialTheme.colorScheme.onSurface
        val textSizePx = with(density) { 20.sp.toPx() }
        val maxWidthPx = with(density) { 40.sp.toPx() }

        // BoxWithConstraints 是 Box,子项默认堆叠在同一位置,
        // 因此纵向排列需要在内部再套一个 Column。
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionTitle("拆分前(单块,会被裁剪/溢出)")
            SingleBlockLatex(
                latex = formula,
                textSizePx = textSizePx,
                colorArgb = color.toArgb()
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            SectionTitle("拆分后(自动水平拆分 + 换行)")
            SplitLatexText(
                latex = formula,
                maxWidthPx = maxWidthPx,
                textSizePx = textSizePx,
                colorArgb = color.toArgb()
            )
        }
    }
}

/**
 * 直接渲染整块公式,用作对比:超出可用宽度时会被压缩或溢出。
 */
@Composable
private fun SingleBlockLatex(latex: String, textSizePx: Float, colorArgb: Int) {
    val drawable = remember(latex, textSizePx, colorArgb) {
        ru.noties.jlatexmath.JLatexMathDrawable.builder(latex)
            .textSize(textSizePx)
            .color(colorArgb)
            .build()
    }
    val density = LocalDensity.current
    AndroidView(
        factory = { ctx -> ImageView(ctx) },
        update = { it.setImageDrawable(drawable) },
        modifier = Modifier.height(with(density) { drawable.intrinsicHeight.toDp() })
    )
}

/**
 * 使用 [JLatexMathSplitter] 把公式拆为多段,作为 InlineContent 嵌入 AnnotatedString,实现自动换行。
 */
@Composable
private fun SplitLatexText(
    latex: String,
    maxWidthPx: Float,
    textSizePx: Float,
    colorArgb: Int
) {
    val density = LocalDensity.current

    val parts = remember(latex, maxWidthPx, textSizePx, colorArgb) {
        JLatexMathSplitter.split(latex, maxWidthPx, textSizePx, colorArgb)
    }

    val annotatedString = remember(parts) {
        buildAnnotatedString {
            append("wedwefewrferfefr")
            parts.indices.forEach { i -> appendInlineContent("latex_$i", "[$i]") }
        }
    }

    val inlineContent = remember(parts) {
        parts.mapIndexed { i, drawable ->
            val widthEm = drawable.intrinsicWidth / textSizePx
            val heightEm = drawable.intrinsicHeight / textSizePx
            "latex_$i" to InlineTextContent(
                placeholder = Placeholder(
                    width = widthEm.em,
                    height = heightEm.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                )
            ) {
                AndroidView(
                    factory = { ctx -> ImageView(ctx) },
                    update = { it.setImageDrawable(drawable) }
                )
            }
        }.toMap()
    }

    Text(
        text = annotatedString,
        inlineContent = inlineContent,
        fontSize = with(density) { textSizePx.toSp() },
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = TextUnit.Unspecified,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun ComposePreview() {
    JlatexmathparentTheme {
        LatexInlineDemo()
    }
}
