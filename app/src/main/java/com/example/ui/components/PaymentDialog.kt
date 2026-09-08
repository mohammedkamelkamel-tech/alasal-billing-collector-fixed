package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.BillEntity
import com.example.ui.theme.VibrantGreen
import com.example.utils.CurrencyFormatter

/**
 * نافذة تسجيل الدفع (تدعم الدفع الجزئي).
 *
 * سبب الإنشاء: كان زر "دفع" يحوّل الفاتورة إلى "مدفوعة" مباشرة دون سؤال عن
 * المبلغ. الآن تُطلب قيمة المبلغ المدفوع، ويُعرض المتبقي فوراً، وتُحدَّد الحالة
 * تلقائياً (مدفوعة / مدفوعة جزئياً). مكوّن واحد مشترك بين شاشة الفواتير وشاشة
 * التفاصيل لتجنّب تكرار الكود.
 */
@Composable
fun PaymentDialog(
    bill: BillEntity,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, method: String) -> Unit
) {
    val due = bill.remainingAmount.coerceAtLeast(0.0)
    var amountText by remember { mutableStateOf(due.toLong().toString()) }
    var method by remember { mutableStateOf(bill.paymentMethod.ifBlank { "نقدي" }) }

    val entered = amountText.toDoubleOrNull() ?: 0.0
    val paidNow = entered.coerceAtLeast(0.0)
    val remainingAfter = due - paidNow
    val isValid = entered > 0.0 && due > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تسجيل دفعة - فاتورة ${bill.invoiceNumber}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("المشترك: ${bill.userName}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "المبلغ المستحق: ${CurrencyFormatter.riyal(due)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { new -> amountText = new.filter { it.isDigit() || it == '.' } },
                    label = { Text("المبلغ المدفوع") },
                    isError = amountText.isNotEmpty() && !isValid,
                    supportingText = {
                        if (amountText.isNotEmpty() && !isValid) {
                            Text("أدخل مبلغاً أكبر من صفر")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input")
                )

                OutlinedTextField(
                    value = method,
                    onValueChange = { method = it },
                    label = { Text("طريقة الدفع") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (remainingAfter < 0.0) "الرصيد لك بعد الدفع:" else "المتبقي بعد الدفع:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        CurrencyFormatter.riyal(kotlin.math.abs(remainingAfter)),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (remainingAfter > 0.0) MaterialTheme.colorScheme.error else VibrantGreen
                    )
                }
                Text(
                    text = when {
                        remainingAfter > 0.0 -> "ستُسجَّل الفاتورة كمدفوعة جزئياً ويُرحَّل المتبقي كمتأخرات."
                        remainingAfter < 0.0 -> "سيُقبل المبلغ الزائد ويُحفظ كرصيد لك، ويُخصم تلقائياً من الفاتورة القادمة."
                        else -> "ستُسجَّل الفاتورة كمدفوعة بالكامل."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(paidNow, method.ifBlank { "نقدي" }) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen),
                modifier = Modifier.testTag("confirm_payment_btn")
            ) { Text("تأكيد الدفع") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
