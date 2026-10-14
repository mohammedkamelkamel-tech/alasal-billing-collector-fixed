package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.example.R
import com.example.data.model.BillEntity
import com.example.data.model.BillStatus
import java.io.File
import java.io.FileOutputStream

object InvoiceImageGenerator {
    
    fun generateInvoiceImage(context: Context, bill: BillEntity, orgName: String? = null, orgPhone: String? = null): File? {
        try {
            val width = 800
            val height = 1200
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            // Fill white background
            canvas.drawColor(Color.WHITE)
            
            // Draw watermark if exists
            val watermarkBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_watermark)
            if (watermarkBitmap != null) {
                val paintWatermark = Paint().apply { alpha = 30 }
                val targetWidth = 500f
                val targetHeight = (watermarkBitmap.height.toFloat() / watermarkBitmap.width.toFloat()) * targetWidth
                val left = (width - targetWidth) / 2f
                val top = (height - targetHeight) / 2f
                val scaledBitmap = Bitmap.createScaledBitmap(watermarkBitmap, targetWidth.toInt(), targetHeight.toInt(), true)
                canvas.drawBitmap(scaledBitmap, left, top, paintWatermark)
            }
            
            // Setup Paints
            val titlePaint = Paint().apply {
                color = Color.rgb(13, 71, 161)
                textSize = 36f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            
            val subtitlePaint = Paint().apply {
                color = Color.rgb(33, 33, 33)
                textSize = 24f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            
            val textPaintRight = Paint().apply {
                color = Color.BLACK
                textSize = 22f
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            
            val textPaintLeft = Paint().apply {
                color = Color.BLACK
                textSize = 22f
                isAntiAlias = true
                textAlign = Paint.Align.LEFT
            }
            
            val linePaint = Paint().apply {
                color = Color.GRAY
                strokeWidth = 2f
            }
            
            var y = 80f
            
            // Header
            val organizationName = orgName?.takeIf { it.isNotBlank() } ?: "نظام فواتير الكهرباء الذكي"
            canvas.drawText(organizationName, width / 2f, y, titlePaint)
            y += 40f
            if (!orgPhone.isNullOrBlank()) {
                canvas.drawText("هاتف: $orgPhone", width / 2f, y, subtitlePaint)
                y += 40f
            }
            
            canvas.drawText("إيصال فاتورة كهرباء", width / 2f, y, subtitlePaint)
            y += 40f
            
            canvas.drawLine(50f, y, width - 50f, y, linePaint)
            y += 40f
            
            // Helper to draw a row
            fun drawRow(label: String, value: String) {
                canvas.drawText(label, width - 50f, y, textPaintRight)
                canvas.drawText(value, 50f, y, textPaintLeft)
                y += 35f
            }
            
            drawRow("اسم المشترك:", bill.userName)
            drawRow("رقم الهاتف:", bill.userPhone)
            drawRow("العنوان / الفرع:", bill.userAddress)
            drawRow("رقم الفاتورة:", bill.invoiceNumber)
            
            y += 20f
            canvas.drawLine(50f, y, width - 50f, y, linePaint)
            y += 40f
            
            drawRow("تاريخ القراءة:", bill.readingDate)
            drawRow("القراءة السابقة:", "${bill.prevReading.toInt()} ك.و.س")
            drawRow("القراءة الحالية:", "${bill.currentReading.toInt()} ك.و.س")
            drawRow("إجمالي الاستهلاك:", "${bill.consumptionKwh.toInt()} ك.و.س")
            if (context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getBoolean("invoice_show_unit_price", true)) {
                drawRow("سعر الكيلو واط:", "${bill.unitPrice} ريال")
            }
            
            y += 20f
            canvas.drawLine(50f, y, width - 50f, y, linePaint)
            y += 40f
            
            drawRow("قيمة الاستهلاك:", CurrencyFormatter.riyal(bill.subtotalAmount))
            drawRow("المتأخرات السابقة:", CurrencyFormatter.riyal(bill.previousDebt))
            
            y += 20f
            canvas.drawLine(50f, y, width - 50f, y, linePaint)
            y += 40f
            
            val totalPaint = Paint(textPaintRight).apply {
                isFakeBoldText = true
                textSize = 26f
            }
            val totalPaintLeft = Paint(textPaintLeft).apply {
                isFakeBoldText = true
                textSize = 26f
            }
            
            canvas.drawText("المبلغ الإجمالي المستحق:", width - 50f, y, totalPaint)
            canvas.drawText(CurrencyFormatter.riyalFull(bill.totalAmount), 50f, y, totalPaintLeft)
            y += 45f
            
            drawRow("المبلغ المدفوع:", CurrencyFormatter.riyal(bill.paidAmount))
            drawRow("المبلغ المتبقي:", CurrencyFormatter.riyal(bill.remainingAmount))
            
            y += 20f
            canvas.drawLine(50f, y, width - 50f, y, linePaint)
            y += 40f
            
            drawRow("حالة السداد:", BillStatus.fromString(bill.status).titleAr)
            drawRow("طريقة الدفع:", bill.paymentMethod.ifBlank { "-" })
            drawRow("تاريخ الدفع:", bill.paymentDate.ifBlank { "-" })
            drawRow("تاريخ الإصدار:", bill.issueDate.ifBlank { bill.readingDate })
            drawRow("تاريخ الاستحقاق:", bill.dueDate)
            
            y += 40f
            canvas.drawText("شكراً لتعاملكم معنا", width / 2f, y, subtitlePaint)
            
            val fileName = "invoice_${bill.invoiceNumber}.png"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /** ينشئ صورة إيصال تحصيل مستقلة بألوان مميزة عن صورة فاتورة الكهرباء. */
    fun generatePaymentReceiptImage(context: Context, payment: com.example.data.model.PaymentEntity): File? {
        try {
            val width = 800
            val height = 900
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.rgb(248, 250, 255))

            val accent = Color.rgb(0, 121, 107) // أخضر مزرق: يميز إيصال التحصيل عن الفاتورة الزرقاء/السوداء
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accent; textSize = 38f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val textRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(0, 77, 64); textSize = 27f; textAlign = Paint.Align.RIGHT
            }
            val textLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(55, 71, 79); textSize = 27f; textAlign = Paint.Align.LEFT
            }
            val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accent; textSize = 34f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; strokeWidth = 3f }
            canvas.drawRoundRect(24f, 24f, 776f, 876f, 28f, 28f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
            })
            var y = 95f
            canvas.drawText("محطة العسل", width / 2f, y, titlePaint); y += 55f
            canvas.drawText("إيصال تحصيل كهرباء", width / 2f, y, titlePaint); y += 28f
            canvas.drawLine(55f, y, 745f, y, linePaint); y += 58f

            fun row(label: String, value: String) {
                canvas.drawText(label, 745f, y, textRight)
                canvas.drawText(value, 55f, y, textLeft)
                y += 48f
            }
            row("اسم المشترك:", payment.userName.ifBlank { "غير مسجل" })
            row("رقم الإيصال:", payment.receiptNumber.takeIf { it > 0 }?.toString() ?: "-")
            row("رقم الفاتورة:", payment.invoiceNumber.ifBlank { "-" })
            row("طريقة الدفع:", payment.method.ifBlank { "-" })
            row("المحصّل:", payment.collectorName.ifBlank { "غير مسجل" })
            row("تاريخ التحصيل:", payment.paymentDate.ifBlank { "-" })
            y += 8f
            canvas.drawLine(55f, y, 745f, y, linePaint); y += 58f
            canvas.drawText("المبلغ المحصّل", width / 2f, y, textRight.apply { textAlign = Paint.Align.CENTER }); y += 55f
            canvas.drawText(CurrencyFormatter.riyalFull(payment.amount), width / 2f, y, amountPaint); y += 60f
            canvas.drawText("المتبقي بعد التحصيل: ${CurrencyFormatter.riyal(payment.afterRemaining.coerceAtLeast(0.0))}", width / 2f, y,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; textSize = 26f; textAlign = Paint.Align.CENTER })
            y += 58f
            canvas.drawText("شكراً لكم", width / 2f, y, titlePaint)

            val file = File(context.cacheDir, "payment_receipt_${payment.receiptNumber.takeIf { it > 0 } ?: System.currentTimeMillis()}.png")
            FileOutputStream(file).use { it -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, it); it.flush() }
            bitmap.recycle()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

}
