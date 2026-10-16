package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BillEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.PermissionCatalog
import com.example.data.model.PermissionKeys
import com.example.utils.CurrencyFormatter
import com.example.ui.components.PaymentDialog

@Composable
fun CollectionScreen(
    bills: List<BillEntity>,
    payments: List<PaymentEntity>,
    canPerformAction: (String) -> Boolean,
    onPayClick: (BillEntity, Double, String) -> Unit,
    onWhatsAppClick: (BillEntity) -> Unit,
    onReceiptClick: (PaymentEntity, String) -> Unit
) {
    var selectedBill by remember { mutableStateOf<BillEntity?>(null) }
    var selectedSubscriberId by remember { mutableStateOf<String?>(null) }
    var selectedSubscriberName by remember { mutableStateOf("") }

    selectedBill?.let { bill ->
        PaymentDialog(
            bill = bill,
            onDismiss = { selectedBill = null },
            onConfirm = { amount, method ->
                onPayClick(bill, amount, method)
                selectedBill = null
            }
        )
    }


    if (selectedSubscriberId != null) {
        val subscriberPayments = payments.filter { payment ->
            if (!selectedSubscriberId.isNullOrBlank()) {
                // استخدم المعرّف أولاً، مع تضمين السجلات القديمة التي لم يُحفظ فيها userId.
                payment.userId == selectedSubscriberId ||
                    (payment.userId.isBlank() && payment.userName.equals(selectedSubscriberName, ignoreCase = true))
            } else {
                payment.userName.equals(selectedSubscriberName, ignoreCase = true)
            }
        }.sortedWith(compareByDescending<PaymentEntity> { it.paymentAt }.thenByDescending { it.receiptNumber })
        AlertDialog(
            onDismissRequest = { selectedSubscriberId = null; selectedSubscriberName = "" },
            title = { Text("تحصيلات $selectedSubscriberName") },
            text = {
                if (subscriberPayments.isEmpty()) Text("لا توجد تحصيلات مسجلة لهذا المشترك.")
                else Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("عدد الدفعات: ${subscriberPayments.size}", fontWeight = FontWeight.Bold)
                    Text("إجمالي التحصيل: ${CurrencyFormatter.riyal(subscriberPayments.sumOf { it.amount })}", color = MaterialTheme.colorScheme.primary)
                    subscriberPayments.forEach { payment ->
                        HorizontalDivider()
                        Text("المبلغ: ${CurrencyFormatter.riyal(payment.amount)}", fontWeight = FontWeight.Bold)
                        Text("إيصال #${payment.receiptNumber.takeIf { it > 0 } ?: "-"}")
                        Text("فاتورة الاستهلاك: ${payment.invoiceNumber}")
                        Text("التاريخ: ${payment.paymentDate.ifBlank { "-" }}")
                        Text("المحصل: ${payment.collectorName.ifBlank { "غير مسجل" }}")
                        val phone = bills.firstOrNull { it.userId == payment.userId }?.userPhone.orEmpty()
                        TextButton(onClick = { onReceiptClick(payment, phone) }, enabled = phone.isNotBlank() && payment.receiptNumber > 0) { Text("إرسال الإيصال") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedSubscriberId = null; selectedSubscriberName = "" }) { Text("إغلاق") } }
        )
    }

    val pending = bills.filter { it.status != "PAID" && it.status != "CARRIED" && (it.remainingAmount > 0.0 || it.paidAmount <= 0.0) }
        .sortedByDescending { it.createdAt }
    // كل إيصال يظهر كسجل مستقل، حتى لو كانت الفاتورة نفسها دُفعت على عدة دفعات.
    val collected = payments.sortedByDescending { it.paymentAt }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
                    Column {
                        Text("التحصيل والمقبوضات", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("تسجيل الدفعات ومراجعة من قام بالتحصيل", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Text("الفواتير المستحقة للتحصيل", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        if (pending.isEmpty()) {
            item { Text("لا توجد فواتير مستحقة للتحصيل.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(pending, key = { it.id }) { bill ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                bill.userName.ifBlank { "مشترك" },
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    selectedSubscriberId = bill.userId
                                    selectedSubscriberName = bill.userName.ifBlank { "مشترك" }
                                }
                            )
                            Text("فاتورة ${bill.invoiceNumber}", style = MaterialTheme.typography.labelSmall)
                            Text("المطلوب: ${CurrencyFormatter.riyal(bill.remainingAmount.takeIf { it > 0 } ?: bill.totalAmount)}", color = MaterialTheme.colorScheme.error)
                            if (bill.paidAmount > 0) {
                                Text("المحصّل سابقاً: ${CurrencyFormatter.riyal(bill.paidAmount)}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { onWhatsAppClick(bill) }) {
                                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("واتساب")
                            }
                            if (canPerformAction(PermissionCatalog.PAYMENTS_COLLECT) || canPerformAction(PermissionKeys.CAN_PAY_BILL)) {
                                Button(onClick = { selectedBill = bill }) {
                                    Icon(Icons.Filled.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("تحصيل")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text("آخر المقبوضات", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        if (collected.isEmpty()) {
            item { Text("لا توجد عمليات تحصيل مسجلة بعد.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(collected.take(30), key = { "paid-${it.id}" }) { payment ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                payment.userName.ifBlank { "مشترك" },
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    selectedSubscriberId = payment.userId
                                    selectedSubscriberName = payment.userName.ifBlank { "مشترك" }
                                }
                            )
                            Text(CurrencyFormatter.riyal(payment.amount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            "إيصال #${payment.receiptNumber.takeIf { it > 0 } ?: "-"} • الفاتورة: ${payment.invoiceNumber} • التاريخ: ${payment.paymentDate.ifBlank { "-" }}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text("المحصل: ${payment.collectorName.ifBlank { "غير مسجل" }} • الطريقة: ${payment.method}", style = MaterialTheme.typography.bodySmall)
                        Text("المتبقي بعد الإيصال: ${CurrencyFormatter.riyal(payment.afterRemaining.coerceAtLeast(0.0))}", style = MaterialTheme.typography.labelSmall)
                        val phone = bills.firstOrNull { it.userId == payment.userId }?.userPhone.orEmpty()
                        TextButton(onClick = { onReceiptClick(payment, phone) }, enabled = phone.isNotBlank() && payment.receiptNumber > 0) { Text("فتح / إرسال الإيصال عبر واتساب") }
                    }
                }
            }
        }
    }
}
