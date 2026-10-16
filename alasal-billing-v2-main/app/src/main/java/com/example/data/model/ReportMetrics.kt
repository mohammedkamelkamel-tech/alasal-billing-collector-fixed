package com.example.data.model

/** Deterministic totals for report cards; paid totals use payment records to avoid double-counting partial payments. */
data class ReportMetrics(
    val invoiceCount: Int,
    val paidInvoiceCount: Int,
    val unpaidInvoiceCount: Int,
    val invoicedAmount: Double,
    val collectedAmount: Double,
    val outstandingAmount: Double
)

object ReportMetricsCalculator {
    fun calculate(bills: List<BillEntity>, payments: List<PaymentEntity>): ReportMetrics {
        val activeBills = bills.filter { it.status != "CARRIED" }
        return ReportMetrics(
            invoiceCount = activeBills.size,
            paidInvoiceCount = activeBills.count { it.remainingAmount <= 0.0 && it.totalAmount > 0.0 },
            unpaidInvoiceCount = activeBills.count { it.remainingAmount > 0.0 },
            invoicedAmount = activeBills.sumOf { it.totalAmount },
            collectedAmount = payments.sumOf { it.amount },
            outstandingAmount = activeBills.sumOf { it.remainingAmount.coerceAtLeast(0.0) }
        )
    }
}
