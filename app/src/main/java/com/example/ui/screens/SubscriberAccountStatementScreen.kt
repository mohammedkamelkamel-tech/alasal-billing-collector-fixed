package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BillEntity
import com.example.data.model.BillStatus
import com.example.data.model.PaymentEntity
import com.example.data.model.UserEntity
import com.example.utils.CurrencyFormatter

private data class StatementRow(
    val timestamp: Long,
    val sortKey: String,
    val title: String,
    val reference: String,
    val date: String,
    val debit: Double,
    val credit: Double,
    val detail: String
)

@Composable
fun SubscriberAccountStatementScreen(
    user: UserEntity,
    bills: List<BillEntity>,
    payments: List<PaymentEntity>,
    onShare: (String) -> Unit
) {
    val activeBills = remember(bills) { bills.filter { it.status != BillStatus.CARRIED.name } }
    val rows = remember(activeBills, payments) {
        buildList {
            activeBills.forEach { bill ->
                add(
                    StatementRow(
                        timestamp = bill.createdAt,
                        sortKey = bill.id,
                        title = "فاتورة استهلاك",
                        reference = "رقم الفاتورة: ${bill.invoiceNumber}",
                        date = bill.issueDate.ifBlank { "-" },
                        debit = bill.totalAmount,
                        credit = 0.0,
                        detail = "الاستهلاك: ${formatNumber(bill.consumptionKwh)} ك.و.س • المدفوع على الفاتورة: ${CurrencyFormatter.riyal(bill.paidAmount)}"
                    )
                )
            }
            payments.forEach { payment ->
                add(
                    StatementRow(
                        timestamp = payment.paymentAt,
                        sortKey = payment.id,
                        title = "إيصال تحصيل",
                        reference = "رقم الإيصال: ${payment.receiptNumber.takeIf { it > 0L } ?: "-"} • فاتورة: ${payment.invoiceNumber}",
                        date = payment.paymentDate.ifBlank { "-" },
                        debit = 0.0,
                        credit = payment.amount,
                        detail = "طريقة الدفع: ${payment.method} • المحصل: ${payment.collectorName.ifBlank { "غير مسجل" }}"
                    )
                )
            }
        }.sortedWith(compareByDescending<StatementRow> { it.timestamp }.thenByDescending { it.sortKey })
    }
    val totalInvoiced = activeBills.sumOf { it.totalAmount }
    val totalCollected = payments.sumOf { it.amount }
    val currentBalance = activeBills.sumOf { it.remainingAmount }
    val shareText = remember(user, activeBills, payments, totalInvoiced, totalCollected, currentBalance, rows) {
        buildString {
            appendLine("محطة العسل - كشف حساب مشترك")
            appendLine("المشترك: ${user.name}")
            appendLine("الهاتف: ${user.phone.ifBlank { "-" }}")
            appendLine("عدد فواتير الاستهلاك: ${activeBills.size}")
            appendLine("عدد إيصالات التحصيل: ${payments.size}")
            appendLine("إجمالي الفواتير الظاهرة: ${CurrencyFormatter.riyal(totalInvoiced)}")
            appendLine("إجمالي التحصيلات المسجلة: ${CurrencyFormatter.riyal(totalCollected)}")
            appendLine("الرصيد المتبقي حسب الفواتير: ${CurrencyFormatter.riyal(currentBalance)}")
            appendLine("━━━━━━━━━━━━━━━━━━")
            rows.sortedBy { it.timestamp }.forEach { row ->
                appendLine("${row.date} | ${row.title}")
                appendLine("${row.reference}")
                if (row.debit > 0) appendLine("مدين: ${CurrencyFormatter.riyal(row.debit)}")
                if (row.credit > 0) appendLine("مدفوع: ${CurrencyFormatter.riyal(row.credit)}")
                appendLine(row.detail)
                appendLine()
            }
            appendLine("كشف حساب صادر من محطة العسل لخدمات الكهرباء والإنترنت")
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("كشف حساب المشترك", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(user.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("الهاتف: ${user.phone.ifBlank { "-" }}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الفواتير: ${activeBills.size}")
                    Text("الإيصالات: ${payments.size}")
                }
                HorizontalDivider()
                Text("إجمالي الفواتير الظاهرة: ${CurrencyFormatter.riyal(totalInvoiced)}")
                Text("إجمالي التحصيلات: ${CurrencyFormatter.riyal(totalCollected)}")
                Text("الرصيد المتبقي: ${CurrencyFormatter.riyal(currentBalance)}", fontWeight = FontWeight.Bold, color = if (currentBalance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Button(onClick = { onShare(shareText) }, modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.Icon(Icons.Filled.Share, contentDescription = null)
                    Text("إرسال كشف الحساب عبر واتساب")
                }
            }
        }
        Text("سجل الفواتير والتحصيلات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (rows.isEmpty()) {
            Text("لا توجد فواتير أو إيصالات مسجلة لهذا المشترك.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(rows, key = { it.sortKey }) { row ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(row.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(row.date, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(row.reference)
                            if (row.debit > 0) Text("المبلغ المستحق: ${CurrencyFormatter.riyal(row.debit)}", color = MaterialTheme.colorScheme.error)
                            if (row.credit > 0) Text("المبلغ المحصل: ${CurrencyFormatter.riyal(row.credit)}", color = MaterialTheme.colorScheme.primary)
                            Text(row.detail, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(java.util.Locale.US, "%.2f", value)
