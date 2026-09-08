package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.VibrantGreen
import com.example.ui.theme.WarningYellow

/**
 * حالات الفاتورة.
 *
 * تمت إضافة:
 * - PARTIAL: مدفوعة جزئياً (بقي مبلغ متبقٍ يُسجَّل كمتأخرات).
 * - CARRIED: مُرحّلة، أي أن متبقّيها أُدرج ضمن فاتورة أحدث كمتأخرات،
 *   وبالتالي لم يعد مبلغها مستحقاً بذاته (يمنع ازدواج احتساب المتأخرات).
 */
enum class BillStatus(val titleAr: String, val color: Color, val iconSymbol: String) {
    PAID("مدفوعة", VibrantGreen, "✅"),
    PARTIAL("مدفوعة جزئياً", WarningYellow, "🟡"),
    UNPAID("غير مدفوعة", ErrorRed, "❌"),
    OVERDUE("متأخرة", WarningYellow, "⚠️"),
    CARRIED("مُرحّلة", ElectricBlue, "🔄");

    companion object {
        fun fromString(statusStr: String): BillStatus {
            return entries.find { it.name.equals(statusStr, ignoreCase = true) || it.titleAr == statusStr } ?: UNPAID
        }

        /** هل ما زال على الفاتورة مبلغ مستحق فعلي؟ */
        fun isOutstanding(statusStr: String): Boolean {
            return when (fromString(statusStr)) {
                PAID, CARRIED -> false
                else -> true
            }
        }
    }
}
