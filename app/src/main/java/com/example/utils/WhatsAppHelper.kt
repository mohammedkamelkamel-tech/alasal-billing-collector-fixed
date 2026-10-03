package com.example.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.BillEntity
import java.util.Locale

/**
 * فتح محادثة WhatsApp الخاصة بالعميل مع الفاتورة/الإيصال مكتوباً مسبقاً.
 *
 * WhatsApp يتيح فتح المحادثة مع نص مُعبأ مسبقاً عبر wa.me، لكن الضغط على
 * إرسال يظل بيد المستخدم؛ لا يوجد في WhatsApp العادي API محلي يسمح للتطبيق
 * بإرسال الرسالة بصمت من دون تفاعل المستخدم.
 */
object WhatsAppHelper {

    fun sendInvoice(context: Context, bill: BillEntity): Boolean {
        val message = buildString {
            appendLine("🧾 فاتورة محطة العسل")
            appendLine("━━━━━━━━━━━━━━")
            appendLine("العميل: ${bill.userName}")
            appendLine("رقم الفاتورة: ${bill.invoiceNumber}")
            appendLine("التاريخ: ${bill.issueDate}")
            appendLine()
            appendLine("القراءة السابقة: ${formatNumber(bill.prevReading)}")
            appendLine("القراءة الحالية: ${formatNumber(bill.currentReading)}")
            appendLine("الاستهلاك: ${formatNumber(bill.consumptionKwh)} ك.و.س")
            appendLine("سعر الكيلو: ${formatMoney(bill.unitPrice)} ريال")
            appendLine("قيمة الاستهلاك: ${formatMoney(bill.subtotalAmount)} ريال")
            if (bill.previousDebt != 0.0) {
                appendLine("المتأخرات/الرصيد: ${formatMoney(bill.previousDebt)} ريال")
            }
            appendLine("الإجمالي المستحق: ${formatMoney(bill.totalAmount)} ريال")
            if (bill.paidAmount > 0.0) {
                appendLine("المدفوع: ${formatMoney(bill.paidAmount)} ريال")
                appendLine("المتبقي: ${formatMoney(bill.remainingAmount)} ريال")
            }
            if (bill.dueDate.isNotBlank()) appendLine("تاريخ الاستحقاق: ${bill.dueDate}")
            appendLine()
            appendLine("شكراً لتعاملكم معنا 🌷")
            appendLine("محطة العسل لخدمات الكهرباء والإنترنت")
        }
        return openWhatsApp(context, bill.userPhone, message)
    }

    fun sendCollection(
        context: Context,
        bill: BillEntity,
        amount: Double,
        method: String
    ): Boolean {
        val message = buildString {
            appendLine("🧾 إيصال تحصيل - محطة العسل")
            appendLine("━━━━━━━━━━━━━━")
            appendLine("العميل: ${bill.userName}")
            appendLine("الفاتورة: ${bill.invoiceNumber}")
            appendLine("تاريخ التحصيل: ${bill.paymentDate}")
            appendLine("المبلغ المحصل: ${formatMoney(amount)} ريال")
            appendLine("طريقة الدفع: ${method}")
            appendLine("إجمالي الفاتورة: ${formatMoney(bill.totalAmount)} ريال")
            appendLine("المتبقي بعد التحصيل: ${formatMoney(bill.remainingAmount.coerceAtLeast(0.0))} ريال")
            if (bill.remainingAmount <= 0.0) {
                appendLine("الحالة: تم السداد بالكامل ✅")
            } else {
                appendLine("الحالة: دفعة جزئية")
            }
            appendLine()
            appendLine("شكراً لتعاملكم معنا 🌷")
            appendLine("محطة العسل لخدمات الكهرباء والإنترنت")
        }
        return openWhatsApp(context, bill.userPhone, message)
    }

    fun openWhatsApp(context: Context, phone: String, message: String): Boolean {
        val normalized = normalizeYemenPhone(phone) ?: return false
        val encoded = Uri.encode(message)
        val intents = listOf(
            Uri.parse("whatsapp://send?phone=${normalized}&text=${encoded}"),
            Uri.parse("https://wa.me/${normalized}?text=${encoded}")
        )
        val packages = listOf("com.whatsapp", "com.whatsapp.w4b")

        for (uri in intents) {
            for (pkg in packages) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage(pkg)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return true
                } catch (_: ActivityNotFoundException) {
                    // جرّب الرابط/الحزمة التالية.
                } catch (_: Exception) {
                    // لا نوقف حفظ الفاتورة بسبب مشكلة في فتح WhatsApp.
                }
            }
        }
        return false
    }

    private fun normalizeYemenPhone(raw: String): String? {
        var p = raw.trim().replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        if (p.isBlank()) return null
        if (p.startsWith("+")) p = p.drop(1)
        p = when {
            p.startsWith("00967") -> p.drop(2)
            p.startsWith("0967") -> p.drop(1)
            p.startsWith("967") -> p
            p.startsWith("0") -> "967${p.drop(1)}"
            else -> "967${p}"
        }
        return p.takeIf { it.matches(Regex("967[0-9]{9}")) }
    }

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString()
        else String.format(Locale.US, "%.2f", value)

    private fun formatMoney(value: Double): String = formatNumber(value)
}
