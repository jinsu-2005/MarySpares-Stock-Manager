package com.marytwowheelers.spares.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AddPartDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, partNumber: String, shelfLocation: String, sellingPricePaise: Long, mrpPaise: Long, initialStock: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var partNumber by remember { mutableStateOf("") }
    var shelfLocation by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var mrp by remember { mutableStateOf("") }
    var initialStock by remember { mutableStateOf("0") }

    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.red < 0.5f

    // ─── Theme Colors ─────────────────────────────
    val cardBg = if (isDark) Color(0xFF222530) else Color.White
    val cardBorder = if (isDark) Color(0xFF323748) else Color(0xFFEEF0FA)
    val primaryText = if (isDark) Color(0xFFF3F4F6) else Color(0xFF1E1B4B)
    val secondaryText = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val pillBg = if (isDark) Color(0xFF2C3140) else Color(0xFFF1F3F9)
    val accentColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF5046E5)
    val emeraldTint = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val emeraldBg = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)

    val isSpValid = sellingPrice.isBlank() || (sellingPrice.toDoubleOrNull() != null && (sellingPrice.toDoubleOrNull() ?: 0.0) >= 0)
    val isMrpValid = mrp.isBlank() || (mrp.toDoubleOrNull() != null && (mrp.toDoubleOrNull() ?: 0.0) >= 0)
    val isStockValid = initialStock.isNotBlank() && initialStock.toIntOrNull() != null && (initialStock.toIntOrNull() ?: -1) >= 0

    val isFormValid = name.isNotBlank() && isStockValid && isSpValid && isMrpValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(color = emeraldBg, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddBox,
                        contentDescription = null,
                        tint = emeraldTint,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column {
                    Text(
                        text = "Add New Spare Part",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = primaryText
                        )
                    )
                    Text(
                        text = "Catalog a new inventory item",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.5.sp,
                            color = secondaryText
                        )
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Auto-Assigned Serial Number Info Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF2E2A48) else Color(0xFFEFEBFA),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF3E3666) else Color(0xFFDDD6FE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Auto-assigns next serial (#1, #2...)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFDDD6FE) else Color(0xFF4338CA)
                            )
                        )
                    }
                }

                // 1. Part Name (Mandatory)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    label = { Text("Part Name *", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = secondaryText,
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Part Number
                OutlinedTextField(
                    value = partNumber,
                    onValueChange = { partNumber = it },
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    label = { Text("Part Number", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.QrCode,
                            contentDescription = null,
                            tint = secondaryText,
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. Shelf Location & Initial Stock (Mandatory)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = shelfLocation,
                        onValueChange = { shelfLocation = it },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        label = { Text("Location", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = secondaryText,
                                modifier = Modifier.size(17.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = initialStock,
                        onValueChange = { initialStock = it },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        label = { Text("Initial Units *", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Layers,
                                contentDescription = null,
                                tint = secondaryText,
                                modifier = Modifier.size(17.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.9f)
                    )
                }

                // 4. Selling Price & MRP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        label = { Text("Selling Price (₹)", fontSize = 12.sp) },
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = mrp,
                        onValueChange = { mrp = it },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        label = { Text("MRP (₹)", fontSize = 12.sp) },
                        prefix = { Text("₹ ", color = secondaryText, fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val spPaise = ((sellingPrice.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    val mrpPaise = ((mrp.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    val stock = initialStock.toIntOrNull() ?: 0
                    onConfirm(
                        name.trim(),
                        partNumber.trim(),
                        shelfLocation.trim(),
                        spPaise,
                        mrpPaise,
                        stock
                    )
                },
                enabled = isFormValid,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF6366F1) else cs.primary,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text("Save Part", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = secondaryText, fontSize = 13.sp)
            }
        },
        containerColor = cardBg,
        shape = RoundedCornerShape(22.dp)
    )
}
