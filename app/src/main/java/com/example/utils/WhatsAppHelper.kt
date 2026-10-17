package com.example.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Color
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.model.BillEntity
import com.example.data.model.PaymentEntity
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * مشاركة الفواتير وإيصالات التحصيل مع WhatsApp.
 * مشاركة الصورة تفتح شاشة اختيار المستلم في WhatsApp، ويؤكد المستخدم الإرسال بنفسه.
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
            if (context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getBoolean("invoice_show_unit_price", true)) {
                appendLine("سعر الكيلو: ${formatMoney(bill.unitPrice)} ريال")
            }
            appendLine("قيمة الاستهلاك: ${formatMoney(bill.subtotalAmount)} ريال")
            if (bill.previousDebt != 0.0) appendLine("المتأخرات/الرصيد: ${formatMoney(bill.previousDebt)} ريال")
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

    fun sendCollection(context: Context, bill: BillEntity, amount: Double, method: String): Boolean {
        val message = buildString {
            appendLine("🧾 إيصال تحصيل - محطة العسل")
            appendLine("━━━━━━━━━━━━━━")
            appendLine("العميل: ${bill.userName}")
            appendLine("الفاتورة: ${bill.invoiceNumber}")
            appendLine("تاريخ التحصيل: ${bill.paymentDate}")
            appendLine("المبلغ المحصل: ${formatMoney(amount)} ريال")
            appendLine("طريقة الدفع: $method")
            appendLine("إجمالي الفاتورة: ${formatMoney(bill.totalAmount)} ريال")
            appendLine("المتبقي بعد التحصيل: ${formatMoney(bill.remainingAmount.coerceAtLeast(0.0))} ريال")
            appendLine(if (bill.remainingAmount <= 0.0) "الحالة: تم السداد بالكامل ✅" else "الحالة: دفعة جزئية")
            appendLine()
            appendLine("شكراً لتعاملكم معنا 🌷")
            appendLine("محطة العسل لخدمات الكهرباء والإنترنت")
        }
        return openWhatsApp(context, bill.userPhone, message)
    }

    /** يشارك صورة PNG لإيصال الدفعة المحددة، مع رقم إيصال مستقل عن رقم فاتورة الاستهلاك. */
    fun sendCollectionReceipt(context: Context, payment: PaymentEntity, phone: String): Boolean {
        // Validate and normalize the subscriber's number before opening WhatsApp.
        // The jid extra asks WhatsApp to open the intended chat while keeping the receipt image attached.
        val normalizedPhone = normalizeYemenPhone(phone) ?: return false
        val receiptText = buildString {
            appendLine("محطة العسل")
            appendLine("إيصال تحصيل")
            appendLine("رقم الإيصال: ${payment.receiptNumber.takeIf { it > 0 } ?: "غير متوفر"}")
            appendLine("المشترك: ${payment.userName}")
            appendLine("فاتورة الاستهلاك: ${payment.invoiceNumber}")
            appendLine("تاريخ التحصيل: ${payment.paymentDate.ifBlank { "-" }}")
            appendLine("المبلغ المحصل: ${formatMoney(payment.amount)} ريال")
            appendLine("طريقة الدفع: ${payment.method}")
            appendLine("المتبقي بعد الدفعة: ${formatMoney(payment.afterRemaining.coerceAtLeast(0.0))} ريال")
            appendLine("المحصل: ${payment.collectorName.ifBlank { "غير مسجل" }}")
            appendLine()
            appendLine("شكراً لتعاملكم معنا")
            appendLine("خدمات الكهرباء والإنترنت")
        }
        return try {
            val imageFile = createReceiptImage(context, receiptText)
            val imageUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, receiptText)
                // WhatsApp's jid format is the full international number without '+'.
                putExtra("jid", "${normalizedPhone}@s.whatsapp.net")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val whatsappIntent = shareIntent.clone() as Intent
            whatsappIntent.setPackage("com.whatsapp")
            try {
                context.startActivity(whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (_: ActivityNotFoundException) {
                val businessIntent = shareIntent.clone() as Intent
                businessIntent.setPackage("com.whatsapp.w4b")
                try {
                    context.startActivity(businessIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    true
                } catch (_: Exception) {
                    context.startActivity(Intent.createChooser(shareIntent, "إرسال إيصال التحصيل").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    true
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun createReceiptImage(context: Context, text: String): File {
        val width = 900
        val padding = 48
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 36f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width - padding * 2)
            .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
            .setIncludePad(true)
            .setLineSpacing(12f, 1.15f)
            .build()
        val height = (layout.height + padding * 2 + 40).coerceAtLeast(500)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 110, 75)
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        canvas.drawRoundRect(16f, 16f, width - 16f, height - 16f, 28f, 28f, borderPaint)
        canvas.save()
        canvas.translate(padding.toFloat(), padding.toFloat() + 12f)
        layout.draw(canvas)
        canvas.restore()
        val file = File(context.cacheDir, "receipt-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
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
                } catch (_: Exception) {
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
