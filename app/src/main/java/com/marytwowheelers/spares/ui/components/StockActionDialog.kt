package com.marytwowheelers.spares.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marytwowheelers.spares.data.local.MovementType

@Composable
fun StockActionDialog(
    actionType: MovementType,
    partName: String,
    currentStock: Int,
    onDismiss: () -> Unit,
    onConfirm: (deltaOrTarget: Int, reason: String?) -> Unit
) {
    var quantityInput by remember { mutableStateOf("1") }
    var reasonInput by remember(actionType) {
        mutableStateOf(if (actionType == MovementType.ADJUST) "Stock count correction" else "")
    }

    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.red < 0.5f

    val cardBg = if (isDark) Color(0xFF2C3142) else Color.White
    val cardBorder = if (isDark) Color(0xFF424A63) else Color(0xFFEEF0FA)
    val primaryText = if (isDark) Color(0xFFF9FAFB) else Color(0xFF1E1B4B)
    val secondaryText = if (isDark) Color(0xFFA5B4CB) else Color(0xFF6B7280)
    val pillBg = if (isDark) Color(0xFF373E54) else Color(0xFFF1F3F9)

    val parsedQty = quantityInput.toIntOrNull() ?: 0

    // Compute Resulting Stock & Validation
    val isOverRemoval = actionType == MovementType.REMOVE && parsedQty > currentStock
    val isZeroOrNegative = parsedQty <= 0 && actionType != MovementType.ADJUST

    val resultingStock = when (actionType) {
        MovementType.ADD -> currentStock + parsedQty
        MovementType.REMOVE -> currentStock - parsedQty
        MovementType.RETURN -> currentStock + parsedQty
        MovementType.ADJUST -> parsedQty
    }

    val adjustDiff = parsedQty - currentStock

    val (title, icon, actionColor, iconBg) = when (actionType) {
        MovementType.ADD -> Quadruple(
            "Add Stock",
            Icons.Outlined.AddCircleOutline,
            if (isDark) Color(0xFF34D399) else Color(0xFF059669),
            if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
        )
        MovementType.REMOVE -> Quadruple(
            "Remove Stock",
            Icons.Outlined.RemoveCircleOutline,
            if (isDark) Color(0xFFFF4D6D) else Color(0xFFF43F5E),
            if (isDark) Color(0xFF381520) else Color(0xFFFFEEF0)
        )
        MovementType.RETURN -> Quadruple(
            "Record Customer Return",
            Icons.Outlined.RotateLeft,
            if (isDark) Color(0xFFC4B5FD) else Color(0xFF4F46E5),
            if (isDark) Color(0xFF2E2A48) else Color(0xFFEFEBFA)
        )
        MovementType.ADJUST -> Quadruple(
            "Adjust Count",
            Icons.Outlined.Tune,
            if (isDark) Color(0xFFFFB726) else Color(0xFFD97706),
            if (isDark) Color(0xFF382914) else Color(0xFFFEF3C7)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(20.dp)),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color = iconBg, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = actionColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                    )
                    Text(
                        text = partName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = secondaryText
                        ),
                        maxLines = 1
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current Stock → Resulting Stock Preview Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = pillBg,
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "CURRENT STOCK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    color = secondaryText,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                "$currentStock units",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText
                                )
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFD1D5DB) else secondaryText,
                            modifier = Modifier.size(18.dp)
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "RESULTING STOCK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    color = secondaryText,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                if (isOverRemoval) "Invalid (< 0)" else "$resultingStock units",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverRemoval) Color(0xFFEF4444) else actionColor
                                )
                            )
                        }
                    }
                }

                // If Adjust, show difference
                if (actionType == MovementType.ADJUST && quantityInput.isNotBlank()) {
                    Text(
                        text = "Net change: ${if (adjustDiff >= 0) "+$adjustDiff" else "$adjustDiff"} units",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (adjustDiff >= 0) actionColor else Color(0xFFEF4444)
                        )
                    )
                }

                // Quantity Input
                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { quantityInput = it },
                    label = {
                        Text(
                            if (actionType == MovementType.ADJUST) "Physical Count on Shelf *" else "Quantity to ${actionType.name.lowercase().replaceFirstChar { it.uppercase() }} *"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = isOverRemoval,
                    supportingText = {
                        if (isOverRemoval) {
                            Text(
                                "Cannot remove more than available stock ($currentStock units)",
                                color = Color(0xFFEF4444)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Reason / Note Input
                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    label = { Text("Reason / Note (Optional)") },
                    placeholder = {
                        Text(
                            when (actionType) {
                                MovementType.ADD -> "e.g. Supplier restock shipment"
                                MovementType.REMOVE -> "e.g. Customer counter sale"
                                MovementType.RETURN -> "e.g. Customer return (wrong size)"
                                MovementType.ADJUST -> "e.g. Stock count correction"
                            }
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isOverRemoval && (parsedQty > 0 || actionType == MovementType.ADJUST)) {
                        onConfirm(parsedQty, reasonInput.ifBlank { null })
                    }
                },
                enabled = quantityInput.isNotBlank() && !isOverRemoval && (!isZeroOrNegative || (actionType == MovementType.ADJUST && parsedQty >= 0)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        actionType == MovementType.REMOVE && isDark -> Color(0xFFF43F5E)
                        actionType == MovementType.ADD && isDark -> Color(0xFF059669)
                        actionType == MovementType.RETURN && isDark -> Color(0xFF6366F1)
                        actionType == MovementType.ADJUST && isDark -> Color(0xFFD97706)
                        else -> actionColor
                    },
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = when (actionType) {
                        MovementType.ADD -> "Confirm Add (+$parsedQty)"
                        MovementType.REMOVE -> "Confirm Remove (−$parsedQty)"
                        MovementType.RETURN -> "Accept Return (+$parsedQty)"
                        MovementType.ADJUST -> "Confirm Adjust ($parsedQty)"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = secondaryText)
            }
        },
        containerColor = cardBg,
        shape = RoundedCornerShape(20.dp)
    )
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

