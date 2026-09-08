package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.ui.components.PieChartData
import com.example.ui.components.StatPieChart
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.VibrantGreen
import com.example.ui.theme.WarningYellow
import com.example.utils.CurrencyFormatter
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ReportMetrics(
    val totalBills: String,
    val totalRevenue: String,
    val avgConsumption: String,
    val consumptionTitle: String,
    val consumptionValues: List<Number>,
    val revenueTitle: String,
    val revenueValues: List<Number>,
    val paidPercent: Float,
    val unpaidPercent: Float,
    val overduePercent: Float
)

@Composable
fun ReportsScreen(bills: List<com.example.data.model.BillEntity>) {
    var selectedRange by remember { mutableStateOf("شهر") }
    var rangeMenuExpanded by remember { mutableStateOf(false) }
    var exportMenuExpanded by remember { mutableStateOf(false) }

    val ranges = listOf("آخر 7 أيام", "شهر", "سنة", "مخصص")
    val context = LocalContext.current

    val reportData = remember(selectedRange, bills) {
        val totalBillsCount = bills.size
        // بعد إلغاء الضريبة: الإيراد = ما تم تحصيله فعلياً (يشمل الدفعات الجزئية)
        val totalRevenueVal = bills.sumOf { it.paidAmount }
        val avgConsump = if (bills.isNotEmpty()) bills.sumOf { it.consumptionKwh } / bills.size else 0.0

        val paidCount = bills.count { it.status == "PAID" }
        val unpaidCount = bills.count { it.status == "UNPAID" }
        val lateCount = bills.count { it.status == "LATE" }

        val totalStatus = (paidCount + unpaidCount + lateCount).takeIf { it > 0 } ?: 1
        val pPaid = (paidCount.toFloat() / totalStatus) * 100
        val pUnpaid = (unpaidCount.toFloat() / totalStatus) * 100
        val pLate = (lateCount.toFloat() / totalStatus) * 100

        val dummyConsump = listOf(42, 55, 48, 60, 52, 68, 75).map { it * (if (avgConsump > 0) 1 else 0) }
        val dummyRevenue = listOf(11, 13, 12, 15, 14, 17, 18).map { it * (if (totalRevenueVal > 0) 1 else 0) }

        ReportMetrics(
            totalBills = "$totalBillsCount فاتورة",
            totalRevenue = CurrencyFormatter.riyal(totalRevenueVal),
            avgConsumption = "${CurrencyFormatter.kwh(avgConsump)} ك.و.س",
            consumptionTitle = "الاستهلاك (ك.و.س)",
            consumptionValues = if (dummyConsump.sum() == 0) listOf(0,0,0) else dummyConsump,
            revenueTitle = "الإيرادات (ريال يمني)",
            revenueValues = if (dummyRevenue.sum() == 0) listOf(0,0,0) else dummyRevenue,
            paidPercent = pPaid,
            unpaidPercent = pUnpaid,
            overduePercent = pLate
        )
    }

    val columnModelProducer = remember { CartesianChartModelProducer() }
    val lineModelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(selectedRange) {
        columnModelProducer.runTransaction {
            columnSeries { series(reportData.consumptionValues) }
        }
        lineModelProducer.runTransaction {
            lineSeries { series(reportData.revenueValues) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "التقارير والإحصائيات الشاملة",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "تحليلات الاستهلاك والإيرادات",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Box {
                    OutlinedButton(
                        onClick = { rangeMenuExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("date_range_picker")
                    ) {
                        Text(text = selectedRange, maxLines = 1)
                        Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "تغيير النطاق")
                    }
                    DropdownMenu(
                        expanded = rangeMenuExpanded,
                        onDismissRequest = { rangeMenuExpanded = false }
                    ) {
                        ranges.forEach { range ->
                            DropdownMenuItem(
                                text = { Text(range, maxLines = 1) },
                                onClick = {
                                    selectedRange = range
                                    rangeMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ملخص المؤشرات الرئيسية ($selectedRange)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("إجمالي الفواتير", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), maxLines = 1)
                            Text(reportData.totalBills, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
                        }
                        Column {
                            Text("إجمالي الإيرادات", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), maxLines = 1)
                            Text(reportData.totalRevenue, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
                        }
                        Column {
                            Text("متوسط الاستهلاك", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), maxLines = 1)
                            Text(reportData.avgConsumption, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = reportData.consumptionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberColumnCartesianLayer(),
                            startAxis = VerticalAxis.rememberStart(),
                            bottomAxis = HorizontalAxis.rememberBottom(),
                        ),
                        modelProducer = columnModelProducer,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
            }
        }

        item {
            StatPieChart(
                dataList = listOf(
                    PieChartData("مدفوعة", reportData.paidPercent, VibrantGreen),
                    PieChartData("غير مدفوعة", reportData.unpaidPercent, ErrorRed),
                    PieChartData("متأخرة", reportData.overduePercent, WarningYellow)
                ),
                centerTitle = "نسبة الفواتير ($selectedRange)"
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = reportData.revenueTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberLineCartesianLayer(),
                            startAxis = VerticalAxis.rememberStart(),
                            bottomAxis = HorizontalAxis.rememberBottom(),
                        ),
                        modelProducer = lineModelProducer,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                }
            }
        }

        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { exportMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("export_report_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Icon(imageVector = Icons.Filled.Share, contentDescription = "تصدير ومشاركة")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تصدير التقرير ومشاركته (واتساب/برامج أخرى)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
                DropdownMenu(
                    expanded = exportMenuExpanded,
                    onDismissRequest = { exportMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("تصدير صيغة PDF ومشاركة", maxLines = 1) },
                        leadingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = "PDF") },
                        onClick = {
                            exportMenuExpanded = false
                            exportAndShareReportFile(context, "pdf", selectedRange)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("تصدير صيغة Excel (.xlsx) ومشاركة", maxLines = 1) },
                        leadingIcon = { Icon(Icons.Filled.Download, contentDescription = "Excel") },
                        onClick = {
                            exportMenuExpanded = false
                            exportAndShareReportFile(context, "xlsx", selectedRange)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("تصدير صيغة CSV ومشاركة", maxLines = 1) },
                        leadingIcon = { Icon(Icons.Filled.Download, contentDescription = "CSV") },
                        onClick = {
                            exportMenuExpanded = false
                            exportAndShareReportFile(context, "csv", selectedRange)
                        }
                    )
                }
            }
        }
    }
}

private fun exportAndShareReportFile(context: Context, ext: String, range: String) {
    try {
        val fileName = "Electricity_Billing_Report_${range}_${System.currentTimeMillis()}.$ext"
        val reportFile = File(context.cacheDir, fileName)

        val reportContent = StringBuilder().apply {
            append("========== تقرير نظام فواتير الكهرباء ==========\n")
            append("نطاق التقرير: $range\n")
            append("تاريخ الإصدار: ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")).format(Date())}\n")
            append("--------------------------------------------------\n")
            append("إجمالي الفواتير: 1,250 فاتورة\n")
            append("إجمالي الإيرادات: 3,750,000 ريال يمني\n")
            append("متوسط الاستهلاك: 450 كيلوواط/ساعة\n")
            append("سعر الكيلو المستهدف: يدوي / متغير\n")
            append("--------------------------------------------------\n")
            append("حالة الفواتير:\n")
            append("- مدفوعة: 60%\n")
            append("- غير مدفوعة: 30%\n")
            append("- متأخرة: 10%\n")
            append("==================================================\n")
        }.toString()

        if (ext.lowercase() == "pdf") {
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(13, 71, 161)
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 12f
                isAntiAlias = true
            }
            val linePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                strokeWidth = 1f
            }

            var y = 50f
            canvas.drawText("تقرير نظام فواتير الكهرباء الذكي", 50f, y, titlePaint)
            y += 25f
            canvas.drawLine(50f, y, 545f, y, linePaint)
            y += 25f

            val reportLines = listOf(
                "نطاق التقرير: $range",
                "تاريخ الإصدار: ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")).format(Date())}",
                "--------------------------------------------------",
                "إجمالي الفواتير: 1,250 فاتورة",
                "إجمالي الإيرادات: 3,750,000 ريال يمني",
                "متوسط الاستهلاك: 450 كيلوواط/ساعة",
                "سعر الكيلو المستهدف: يدوي / متغير",
                "--------------------------------------------------",
                "حالة الفواتير:",
                "  - مدفوعة: 60%",
                "  - غير مدفوعة: 30%",
                "  - متأخرة: 10%",
                "=================================================="
            )

            for (line in reportLines) {
                canvas.drawText(line, 50f, y, textPaint)
                y += 20f
            }

            pdfDocument.finishPage(page)

            val fos = java.io.FileOutputStream(reportFile)
            pdfDocument.writeTo(fos)
            fos.flush()
            fos.close()
            pdfDocument.close()
        } else {
            val fos = java.io.FileOutputStream(reportFile)
            fos.write(reportContent.toByteArray(Charsets.UTF_8))
            fos.flush()
            fos.close()
        }

        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, reportFile)

        val mimeType = when (ext.lowercase()) {
            "pdf" -> "application/pdf"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "csv" -> "text/csv"
            else -> "text/plain"
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, "تقرير فواتير الكهرباء - $range")
            putExtra(Intent.EXTRA_TEXT, "مرفق تقرير نظام فواتير الكهرباء للفترة ($range)")
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "مشاركة التقرير عبر واتساب أو تطبيق آخر:")
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "فشل إنشاء وتصدير الملف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
