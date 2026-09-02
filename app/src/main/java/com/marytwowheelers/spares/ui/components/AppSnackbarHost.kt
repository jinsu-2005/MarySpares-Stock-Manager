package com.marytwowheelers.spares.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 80.dp
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.red < 0.5f

    val containerColor = if (isDark) Color(0xFF1E222D) else Color(0xFF1E1B4B)
    val contentColor = if (isDark) Color(0xFFF3F4F6) else Color.White
    val actionColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFFA5B4FC)
    val borderColor = if (isDark) Color(0xFF2E3547) else Color(0xFF312E81)

    SnackbarHost(
        hostState = hostState,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = bottomPadding)
    ) { data ->
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = 8.dp,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = contentColor,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f)
                )

                data.visuals.actionLabel?.let { actionLabel ->
                    Spacer(Modifier.width(10.dp))
                    TextButton(
                        onClick = { data.performAction() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = actionColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
