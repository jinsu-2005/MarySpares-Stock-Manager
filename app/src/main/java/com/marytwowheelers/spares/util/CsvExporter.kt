package com.marytwowheelers.spares.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.marytwowheelers.spares.data.model.PartWithStock
import com.marytwowheelers.spares.data.model.StockState
import com.marytwowheelers.spares.data.model.stockState
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object CsvExporter {

    /**
     * Exports inventory directly to a user-selected Uri via Storage Access Framework (SAF).
     */
    fun exportInventoryToUri(context: Context, uri: Uri, parts: List<PartWithStock>): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                BufferedWriter(OutputStreamWriter(os, StandardCharsets.UTF_8)).use { writer ->
                    writer.append("Serial Number,Part Name,Part Number,Shelf Location,Quantity,Selling Price (INR),MRP (INR),Stock Status\n")
                    for (item in parts) {
                        val serial = item.part.serialNumber
                        val name = escapeCsv(item.part.name)
                        val pn = escapeCsv(item.part.partNumber)
                        val shelf = escapeCsv(item.part.shelfLocation)
                        val qty = item.currentStock
                        val sp = String.format(Locale.US, "%.2f", item.part.sellingPricePaise / 100.0)
                        val mrp = String.format(Locale.US, "%.2f", item.part.mrpPaise / 100.0)
                        val status = when (item.stockState) {
                            StockState.HEALTHY -> "In Stock"
                            StockState.LOW -> "Low Stock"
                            StockState.OUT -> "Out of Stock"
                        }
                        writer.append("$serial,$name,$pn,$shelf,$qty,$sp,$mrp,$status\n")
                    }
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Exports complete database backup as a ZIP archive containing all 4 collection CSVs
     * directly to a user-selected Uri via Storage Access Framework (SAF).
     */
    fun exportCloudBackupToZipUri(
        context: Context,
        uri: Uri,
        parts: List<Map<String, Any?>>,
        movements: List<Map<String, Any?>>,
        users: List<Map<String, Any?>>,
        invitations: List<Map<String, Any?>>
    ): Result<Int> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                ZipOutputStream(os).use { zip ->
                    // 1. parts.csv
                    zip.putNextEntry(ZipEntry("parts.csv"))
                    val partsSb = StringBuilder()
                    partsSb.append("ID,Part Number,Name,Shelf Location,MRP Paise,Selling Price Paise,Serial Number,Deleted,Updated At\n")
                    for (p in parts) {
                        partsSb.append("${escapeCsv(p["id"]?.toString() ?: "")},")
                        partsSb.append("${escapeCsv(p["partNumber"]?.toString() ?: "")},")
                        partsSb.append("${escapeCsv(p["name"]?.toString() ?: "")},")
                        partsSb.append("${escapeCsv(p["shelfLocation"]?.toString() ?: "")},")
                        partsSb.append("${p["mrpPaise"] ?: 0},")
                        partsSb.append("${p["sellingPricePaise"] ?: 0},")
                        partsSb.append("${p["serialNumber"] ?: 0},")
                        partsSb.append("${p["deleted"] ?: false},")
                        partsSb.append("${p["updatedAt"] ?: 0}\n")
                    }
                    zip.write(partsSb.toString().toByteArray(StandardCharsets.UTF_8))
                    zip.closeEntry()

                    // 2. movements.csv
                    zip.putNextEntry(ZipEntry("movements.csv"))
                    val movSb = StringBuilder()
                    movSb.append("ID,Part ID,Type,Delta,Timestamp,Reason,Snapshot Count,Previous Stock\n")
                    for (m in movements) {
                        movSb.append("${escapeCsv(m["id"]?.toString() ?: "")},")
                        movSb.append("${escapeCsv(m["partId"]?.toString() ?: "")},")
                        movSb.append("${escapeCsv(m["type"]?.toString() ?: "")},")
                        movSb.append("${m["delta"] ?: 0},")
                        movSb.append("${m["timestamp"] ?: 0},")
                        movSb.append("${escapeCsv(m["reason"]?.toString() ?: "")},")
                        movSb.append("${m["snapshotCount"] ?: ""},")
                        movSb.append("${m["previousRecordedStock"] ?: ""}\n")
                    }
                    zip.write(movSb.toString().toByteArray(StandardCharsets.UTF_8))
                    zip.closeEntry()

                    // 3. users.csv
                    zip.putNextEntry(ZipEntry("users.csv"))
                    val userSb = StringBuilder()
                    userSb.append("UID,Email,Display Name,Role,Status,Auth Provider,Updated At\n")
                    for (u in users) {
                        userSb.append("${escapeCsv(u["uid"]?.toString() ?: "")},")
                        userSb.append("${escapeCsv(u["email"]?.toString() ?: "")},")
                        userSb.append("${escapeCsv(u["displayName"]?.toString() ?: "")},")
                        userSb.append("${escapeCsv(u["role"]?.toString() ?: "")},")
                        userSb.append("${escapeCsv(u["status"]?.toString() ?: "")},")
                        userSb.append("${escapeCsv(u["authProvider"]?.toString() ?: "")},")
                        userSb.append("${u["updatedAt"] ?: 0}\n")
                    }
                    zip.write(userSb.toString().toByteArray(StandardCharsets.UTF_8))
                    zip.closeEntry()

                    // 4. invitations.csv
                    zip.putNextEntry(ZipEntry("invitations.csv"))
                    val invSb = StringBuilder()
                    invSb.append("Email,Name,Role,Status,Invited By,Created At\n")
                    for (i in invitations) {
                        invSb.append("${escapeCsv(i["email"]?.toString() ?: "")},")
                        invSb.append("${escapeCsv(i["name"]?.toString() ?: "")},")
                        invSb.append("${escapeCsv(i["role"]?.toString() ?: "")},")
                        invSb.append("${escapeCsv(i["status"]?.toString() ?: "")},")
                        invSb.append("${escapeCsv(i["invitedBy"]?.toString() ?: "")},")
                        invSb.append("${i["createdAt"] ?: 0}\n")
                    }
                    zip.write(invSb.toString().toByteArray(StandardCharsets.UTF_8))
                    zip.closeEntry()

                    zip.flush()
                }
            }
            Result.success(parts.size + movements.size + users.size + invitations.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportInventoryToCsv(context: Context, parts: List<PartWithStock>): Boolean {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "Mary_Spares_Inventory_$timeStamp.csv"
            val file = File(context.cacheDir, fileName)

            FileWriter(file).use { writer ->
                // Write Header
                writer.append("Serial Number,Part Name,Part Number,Shelf Location,Quantity,Selling Price (INR),MRP (INR),Stock Status\n")

                // Write rows
                for (item in parts) {
                    val serial = item.part.serialNumber
                    val name = escapeCsv(item.part.name)
                    val pn = escapeCsv(item.part.partNumber)
                    val shelf = escapeCsv(item.part.shelfLocation)
                    val qty = item.currentStock
                    val sp = String.format(Locale.US, "%.2f", item.part.sellingPricePaise / 100.0)
                    val mrp = String.format(Locale.US, "%.2f", item.part.mrpPaise / 100.0)
                    val status = when (item.stockState) {
                        StockState.HEALTHY -> "In Stock"
                        StockState.LOW -> "Low Stock"
                        StockState.OUT -> "Out of Stock"
                    }

                    writer.append("$serial,$name,$pn,$shelf,$qty,$sp,$mrp,$status\n")
                }
                writer.flush()
            }

            val uri = FileProvider.getUriForFile(
                context,
                "com.marytwowheelers.spares.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Mary Spares Inventory Export ($timeStamp)")
                putExtra(Intent.EXTRA_TEXT, "Attached is the latest spare parts inventory export from Mary Spares (${parts.size} items).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Inventory CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Exports all Firestore collections to local CSV backup files saved on the Admin's device.
     * Verifies that each file is successfully created, written, and accessible.
     */
    fun exportCloudBackupToDevice(
        context: Context,
        parts: List<Map<String, Any?>>,
        movements: List<Map<String, Any?>>,
        users: List<Map<String, Any?>>,
        invitations: List<Map<String, Any?>>
    ): Result<List<File>> {
        val createdFiles = mutableListOf<File>()
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            // 1. Export Parts
            val partsFile = File(targetDir, "MarySpares_CloudBackup_Parts_$timeStamp.csv")
            FileWriter(partsFile).use { writer ->
                writer.append("ID,Part Number,Name,Shelf Location,MRP Paise,Selling Price Paise,Serial Number,Deleted,Updated At\n")
                for (p in parts) {
                    writer.append("${escapeCsv(p["id"]?.toString() ?: "")},")
                    writer.append("${escapeCsv(p["partNumber"]?.toString() ?: "")},")
                    writer.append("${escapeCsv(p["name"]?.toString() ?: "")},")
                    writer.append("${escapeCsv(p["shelfLocation"]?.toString() ?: "")},")
                    writer.append("${p["mrpPaise"] ?: 0},")
                    writer.append("${p["sellingPricePaise"] ?: 0},")
                    writer.append("${p["serialNumber"] ?: 0},")
                    writer.append("${p["deleted"] ?: false},")
                    writer.append("${p["updatedAt"] ?: 0}\n")
                }
                writer.flush()
            }
            if (!partsFile.exists() || partsFile.length() <= 0) {
                return Result.failure(IllegalStateException("Parts backup file creation failed or empty."))
            }
            createdFiles.add(partsFile)

            // 2. Export Movements
            val movementsFile = File(targetDir, "MarySpares_CloudBackup_Movements_$timeStamp.csv")
            FileWriter(movementsFile).use { writer ->
                writer.append("ID,Part ID,Type,Delta,Timestamp,Reason,Snapshot Count,Previous Stock\n")
                for (m in movements) {
                    writer.append("${escapeCsv(m["id"]?.toString() ?: "")},")
                    writer.append("${escapeCsv(m["partId"]?.toString() ?: "")},")
                    writer.append("${escapeCsv(m["type"]?.toString() ?: "")},")
                    writer.append("${m["delta"] ?: 0},")
                    writer.append("${m["timestamp"] ?: 0},")
                    writer.append("${escapeCsv(m["reason"]?.toString() ?: "")},")
                    writer.append("${m["snapshotCount"] ?: ""},")
                    writer.append("${m["previousRecordedStock"] ?: ""}\n")
                }
                writer.flush()
            }
            if (!movementsFile.exists() || movementsFile.length() <= 0) {
                return Result.failure(IllegalStateException("Movements backup file creation failed or empty."))
            }
            createdFiles.add(movementsFile)

            // 3. Export Users
            val usersFile = File(targetDir, "MarySpares_CloudBackup_Users_$timeStamp.csv")
            FileWriter(usersFile).use { writer ->
                writer.append("UID,Email,Display Name,Role,Status,Auth Provider,Updated At\n")
                for (u in users) {
                    writer.append("${escapeCsv(u["uid"]?.toString() ?: "")},")
                    writer.append("${escapeCsv(u["email"]?.toString() ?: "")},")
                    writer.append("${escapeCsv(u["displayName"]?.toString() ?: "")},")
                    writer.append("${escapeCsv(u["role"]?.toString() ?: "")},")
                    writer.append("${escapeCsv(u["status"]?.toString() ?: "")},")
                    writer.append("${escapeCsv(u["authProvider"]?.toString() ?: "")},")
                    writer.append("${u["updatedAt"] ?: 0}\n")
                }
                writer.flush()
            }
            if (!usersFile.exists() || usersFile.length() <= 0) {
                return Result.failure(IllegalStateException("Users backup file creation failed or empty."))
            }
            createdFiles.add(usersFile)

            // 4. Export Invitations
            val invitesFile = File(targetDir, "MarySpares_CloudBackup_Invitations_$timeStamp.csv")
            FileWriter(invitesFile).use { writer ->
                writer.append("Email,Name,Role,Status,Invited By,Created At\n")
                for (i in invitations) {
                    writer.append("${escapeCsv(i["email"]?.toString() ?: "")},")
                    writer.append("${escapeCsv(i["name"]?.toString() ?: "")},")
                    writer.append("${escapeCsv(i["role"]?.toString() ?: "")},")
                    writer.append("${escapeCsv(i["status"]?.toString() ?: "")},")
                    writer.append("${escapeCsv(i["invitedBy"]?.toString() ?: "")},")
                    writer.append("${i["createdAt"] ?: 0}\n")
                }
                writer.flush()
            }
            if (!invitesFile.exists() || invitesFile.length() <= 0) {
                return Result.failure(IllegalStateException("Invitations backup file creation failed or empty."))
            }
            createdFiles.add(invitesFile)

            return Result.success(createdFiles)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun escapeCsv(value: String): String {
        var str = value.trim()
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            str = str.replace("\"", "\"\"")
            str = "\"$str\""
        }
        return str
    }
}
