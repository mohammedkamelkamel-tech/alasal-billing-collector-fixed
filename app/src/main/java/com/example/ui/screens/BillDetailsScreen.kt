package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.model.BillEntity
import com.example.data.model.BillStatus
import com.example.ui.components.PaymentDialog
import com.example.utils.CurrencyFormatter
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.VibrantGreen

@Composable
fun BillDetailsScreen(
    bill: BillEntity,
    onBackClick: () -> Unit,
    onPayClick: (BillEntity, Double, String) -> Unit,
    onEditClick: (BillEntity) -> Unit,
    onDeleteClick: (BillEntity) -> Unit,
    canEdit: Boolean = false // 👈 صلاحية تعديل الفاتورة (المسؤول فقط)
) {
    val context = LocalContext.current
    val statusEnum = BillStatus.fromString(bill.status)
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    val savePngLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) {
            val pngFile = com.example.utils.InvoiceImageGenerator.generateInvoiceImage(context, bill)
            if (pngFile != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        java.io.FileInputStream(pngFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Toast.makeText(context, "تم حفظ الفاتورة كصورة بنجاح", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "فشل حفظ الصورة", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "فشل توليد صورة الفاتورة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showPaymentDialog) {
        PaymentDialog(
            bill = bill,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, method ->
                showPaymentDialog = false
                onPayClick(bill, amount, method)
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "تأكيد حذف الفاتورة",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من حذف فاتورة المشترك (${bill.userName})؟\nلا يمكن التراجع عن هذا الإجراء.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteClick(bill)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، تأكيد الحذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("لا، تراجع")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header Title Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تفاصيل الفاتورة ${bill.invoiceNumber}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "تاريخ الإصدار والقراءة: ${bill.readingDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    StatusChip(status = statusEnum)
                }
            }
        }

        item {
            // User Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "معلومات المشترك",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ElectricBlue
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    DetailRow(label = "الاسم الكامل", value = bill.userName)
                    DetailRow(label = "الرقم التعريفي", value = "USER-2026-001")
                    DetailRow(label = "العنوان", value = bill.userAddress)
                    DetailRow(label = "رقم الهاتف", value = bill.userPhone)
                }
            }
        }

        item {
            // Meter Reading Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تفاصيل قراءة العداد",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ElectricBlue
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    DetailRow(label = "القراءة السابقة", value = "${bill.prevReading.toInt()} ك.و.س")
                    DetailRow(label = "القراءة الحالية", value = "${bill.currentReading.toInt()} ك.و.س")
                    DetailRow(label = "الاستهلاك الحقيقي", value = "${bill.consumptionKwh.toInt()} ك.و.س")
                    
                    if (!bill.readingImageUri.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "صورة العداد المرفقة:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = bill.readingImageUri,
                                contentDescription = "صورة قراءة العداد",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        item {
            // Calculation Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "حساب الفاتورة والرسوم",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ElectricBlue
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    DetailRow(label = "سعر الكيلو", value = "${bill.unitPrice.toInt()} ريال / ك.و.س")
                    DetailRow(label = "قيمة الاستهلاك", value = CurrencyFormatter.riyal(bill.subtotalAmount))
                    DetailRow(label = "المتأخرات السابقة", value = CurrencyFormatter.riyal(bill.previousDebt))
                    DetailRow(label = "تاريخ الإصدار", value = bill.issueDate.ifBlank { bill.readingDate })
                    DetailRow(label = "تاريخ الاستحقاق", value = bill.dueDate)

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المبلغ الإجمالي المستحق:",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = CurrencyFormatter.riyalFull(bill.totalAmount),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = statusEnum.color
                        )
                    }
                }
            }
        }

        item {
            // Payment History Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "سجل الدفعات",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ElectricBlue
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (bill.paidAmount > 0.0) {
                        DetailRow(label = "تاريخ آخر دفعة", value = bill.paymentDate.ifBlank { "-" })
                        DetailRow(label = "إجمالي المدفوع", value = CurrencyFormatter.riyalFull(bill.paidAmount))
                        DetailRow(label = "المتبقي", value = CurrencyFormatter.riyalFull(bill.remainingAmount))
                        DetailRow(label = "طريقة الدفع", value = bill.paymentMethod.ifBlank { "نقدي" })
                        DetailRow(label = "المحصل", value = bill.paymentCollector.ifBlank { "-" })
                        DetailRow(label = "حالة السداد", value = statusEnum.titleAr)
                    } else {
                        Text(
                            text = "لم يتم السداد بعد - الفاتورة بانتظار التحصيل",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        item {
            // Action Buttons Row
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (bill.status != BillStatus.PAID.name && bill.status != BillStatus.CARRIED.name) {
                    Button(
                        onClick = { showPaymentDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pay_bill_details_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen)
                    ) {
                        Icon(imageVector = Icons.Filled.Payment, contentDescription = "دفع")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (bill.paidAmount > 0.0) "تسجيل دفعة إضافية" else "تسجيل دفعة / دفع الفاتورة",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            com.example.utils.PrintHelper.shareReceipt(context, bill)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = "إرسال نصي")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نص", maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }

                    OutlinedButton(
                        onClick = {
                            val pngFile = com.example.utils.InvoiceImageGenerator.generateInvoiceImage(context, bill)
                            if (pngFile != null) {
                                val authority = "${context.packageName}.fileprovider"
                                val contentUri = androidx.core.content.FileProvider.getUriForFile(context, authority, pngFile)
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "فاتورة كهرباء - ${bill.invoiceNumber}")
                                    putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة الفاتورة (صورة):"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "فشل المشاركة", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "فشل توليد صورة الفاتورة", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Image, contentDescription = "مشاركة صورة")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("صورة", maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    
                    OutlinedButton(
                        onClick = {
                            com.example.utils.PrintHelper.printBill(context, bill)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Print, contentDescription = "طباعة")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("طباعة", maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            savePngLauncher.launch("invoice_${bill.invoiceNumber}.png")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.SaveAlt, contentDescription = "حفظ كصورة")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ PNG", maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    
                    OutlinedButton(
                        onClick = {
                            // منع تعديل الفواتير لغير المصرّح لهم
                            if (canEdit) {
                                onEditClick(bill)
                            } else {
                                Toast.makeText(context, "ليس لديك صلاحية", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "تعديل")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعديل", maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                
                Button(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "حذف")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "حذف الفاتورة",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
