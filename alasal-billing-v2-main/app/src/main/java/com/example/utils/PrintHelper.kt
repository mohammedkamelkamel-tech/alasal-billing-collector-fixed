package com.example.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.data.model.BillEntity
import com.example.data.model.BillStatus
import java.io.FileOutputStream

object PrintHelper {

    /**
     * طباعة الفاتورة عبر خدمة الطباعة في أندرويد (PrintManager)
     */
    fun printBill(context: Context, bill: BillEntity) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Invoice_${bill.invoiceNumber}"

        printManager.print(jobName, object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val builder = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                callback?.onLayoutFinished(builder.build(), true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                val pdfDocument = android.graphics.pdf.PdfDocument()
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val titlePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.rgb(13, 71, 161)
                    textSize = 20f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                val subtitlePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.rgb(33, 33, 33)
                    textSize = 14f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 13f
                    isAntiAlias = true
                }
                val linePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    strokeWidth = 1f
                }

                // Draw watermark
                val watermarkBitmap = android.graphics.BitmapFactory.decodeResource(context.resources, com.example.R.drawable.ic_watermark)
                val paintWatermark = android.graphics.Paint().apply {
                    alpha = 50 // roughly 20% opacity
                }
                
                if (watermarkBitmap != null) {
                    val pageWidth = 595f
                    val pageHeight = 842f
                    // scale down the bitmap
                    val targetWidth = 400f
                    val targetHeight = (watermarkBitmap.height.toFloat() / watermarkBitmap.width.toFloat()) * targetWidth
                    val left = (pageWidth - targetWidth) / 2f
                    val top = (pageHeight - targetHeight) / 2f
                    val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(watermarkBitmap, targetWidth.toInt(), targetHeight.toInt(), true)
                    canvas.drawBitmap(scaledBitmap, left, top, paintWatermark)
                }

                var y = 60f
                canvas.drawText("نظام فواتير الكهرباء الذكي", 50f, y, titlePaint)
                y += 25f
                canvas.drawText("إيصال فاتورة كهرباء رسمية", 50f, y, subtitlePaint)
                y += 20f
                canvas.drawLine(50f, y, 545f, y, linePaint)
                y += 30f

                val lines = listOf(
                    "اسم المشترك: ${bill.userName}",
                    "رقم الهاتف: ${bill.userPhone}",
                    "العنوان / الفرع: ${bill.userAddress}",
                    "رقم الفاتورة: ${bill.invoiceNumber}",
                    "--------------------------------------------------",
                    "القراءة السابقة: ${bill.prevReading.toInt()} ك.و.س",
                    "القراءة الحالية: ${bill.currentReading.toInt()} ك.و.س",
                    "إجمالي الاستهلاك: ${bill.consumptionKwh.toInt()} ك.و.س",
                    "سعر الكيلو واط: ${bill.unitPrice} ريال",
                    "قيمة الاستهلاك: ${CurrencyFormatter.riyal(bill.subtotalAmount)}",
                    "المتأخرات السابقة: ${CurrencyFormatter.riyal(bill.previousDebt)}",
                    "--------------------------------------------------",
                    "المبلغ الإجمالي المستحق: ${CurrencyFormatter.riyalFull(bill.totalAmount)}",
                    "المبلغ المدفوع: ${CurrencyFormatter.riyal(bill.paidAmount)}",
                    "المبلغ المتبقي: ${CurrencyFormatter.riyal(bill.remainingAmount)}",
                    "حالة السداد: ${BillStatus.fromString(bill.status).titleAr}",
                    "طريقة الدفع: ${bill.paymentMethod.ifBlank { "-" }}",
                    "تاريخ الدفع: ${bill.paymentDate.ifBlank { "-" }}",
                    "تاريخ الإصدار: ${bill.issueDate.ifBlank { bill.readingDate }}",
                    "تاريخ القراءة: ${bill.readingDate}",
                    "تاريخ الاستحقاق: ${bill.dueDate}",
                    "=================================================="
                )

                for (line in lines) {
                    canvas.drawText(line, 50f, y, textPaint)
                    y += 22f
                }

                pdfDocument.finishPage(page)

                try {
                    destination?.fileDescriptor?.let { fd ->
                        val fos = FileOutputStream(fd)
                        pdfDocument.writeTo(fos)
                        fos.flush()
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    pdfDocument.close()
                }
            }
        }, null)
    }

    /**
     * مشاركة تفاصيل الفاتورة كإيصال نصي عبر واتساب والتطبيقات الأخرى
     */
    fun shareReceipt(context: Context, bill: BillEntity) {
        val receiptText = """
            ⚡ *إيصال فاتورة كهرباء* ⚡
            
            👤 *اسم المشترك:* ${bill.userName}
            📱 *رقم الهاتف:* ${bill.userPhone}
            📄 *رقم الفاتورة:* ${bill.invoiceNumber}
            
            📊 *القراءة السابقة:* ${bill.prevReading.toInt()} ك.و.س
            📈 *القراءة الحالية:* ${bill.currentReading.toInt()} ك.و.س
            ⚡ *الاستهلاك:* ${bill.consumptionKwh.toInt()} ك.و.س
            
            💰 *قيمة الاستهلاك:* ${CurrencyFormatter.riyal(bill.subtotalAmount)}
            📛 *المتأخرات السابقة:* ${CurrencyFormatter.riyal(bill.previousDebt)}
            💵 *المبلغ الإجمالي:* ${CurrencyFormatter.riyalFull(bill.totalAmount)}
            ✅ *المدفوع:* ${CurrencyFormatter.riyal(bill.paidAmount)}
            🔻 *المتبقي:* ${CurrencyFormatter.riyal(bill.remainingAmount)}
            🗓️ *تاريخ الإصدار:* ${bill.issueDate.ifBlank { bill.readingDate }}
            📅 *تاريخ الاستحقاق:* ${bill.dueDate}
            📌 *حالة السداد:* ${BillStatus.fromString(bill.status).titleAr}
            
            شكراً لكم - نظام فواتير الكهرباء الذكي.
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "إيصال فاتورة كهرباء - ${bill.invoiceNumber}")
            putExtra(Intent.EXTRA_TEXT, receiptText)
        }

        val chooser = Intent.createChooser(shareIntent, "مشاركة الإيصال عبر واتساب أو تطبيق آخر:")
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "لم يتم العثور على تطبيق مناسب للمشاركة", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
