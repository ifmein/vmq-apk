package vmq.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colors from values/colors.xml
private val ColorPrimary = Color(0xFF2563EB)
private val ColorPrimaryDark = Color(0xFF1D4ED8)
private val ColorAccent = Color(0xFF3B82F6)
private val ScreenBackground = Color(0xFFF4F7FC)
private val TextPrimary = Color(0xFF162033)
private val TextSecondary = Color(0xFF5E6B85)
private val TextMuted = Color(0xFF7F8BA3)
private val ButtonPrimaryText = Color(0xFFFFFFFF)
private val ActionTileBackground = Color(0xFF2563EB)
private val Divider = Color(0xFFE8EEF8)
private val SurfaceCard = Color(0xFFFFFFFF)

private val LightColorScheme = lightColorScheme(
    primary = ColorPrimary,
    onPrimary = ButtonPrimaryText,
    primaryContainer = ColorPrimaryDark,
    secondary = ColorAccent,
    background = ScreenBackground,
    surface = SurfaceCard,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = ScreenBackground,
    onSurfaceVariant = TextSecondary,
    outline = Divider,
    outlineVariant = Divider
)

@Composable
fun VmqTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
