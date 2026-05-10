package com.restaurant.management.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class HeaderAccent {
    Primary,
    Secondary,
    Tertiary,
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    accent: HeaderAccent = HeaderAccent.Primary,
    @DrawableRes decorationResId: Int? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val bg: Color
    val fg: Color
    when (accent) {
        HeaderAccent.Primary -> {
            bg = MaterialTheme.colorScheme.primary
            fg = MaterialTheme.colorScheme.onPrimary
        }
        HeaderAccent.Secondary -> {
            bg = MaterialTheme.colorScheme.secondary
            fg = MaterialTheme.colorScheme.onSecondary
        }
        HeaderAccent.Tertiary -> {
            bg = MaterialTheme.colorScheme.tertiary
            fg = MaterialTheme.colorScheme.onTertiary
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = bg,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = fg.copy(alpha = 0.92f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (actions != null) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
            if (decorationResId != null) {
                Image(
                    painter = painterResource(decorationResId),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(64.dp)
                            .alpha(0.92f)
                            .padding(start = 8.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}
