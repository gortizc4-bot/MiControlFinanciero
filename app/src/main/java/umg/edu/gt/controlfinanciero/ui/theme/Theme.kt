
package umg.edu.gt.controlfinanciero.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MiControlFinancieroTheme(
    darkTheme: Boolean = false,
    primaryColor: Color = Color(0xFF9575CD),
    content: @Composable () -> Unit
) {

    val colorScheme = if (darkTheme) {

        darkColorScheme(
            primary = primaryColor,
            secondary = primaryColor,
            tertiary = Color(0xFFE1BEE7),
            background = Color(0xFF18151F),
            surface = Color(0xFF24202D),
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        )

    } else {

        lightColorScheme(
            primary = primaryColor,
            secondary = primaryColor,
            tertiary = Color(0xFFE1BEE7),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            onPrimary = Color.White,
            onBackground = Color(0xFF302746),
            onSurface = Color(0xFF302746)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
