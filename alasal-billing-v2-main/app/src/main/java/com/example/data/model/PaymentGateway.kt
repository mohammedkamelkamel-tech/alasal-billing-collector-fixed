package com.example.data.model

/**
 * Provider-neutral contract for wallet integrations.
 * A real wallet must implement this using the provider's official API and server-verified callbacks.
 * Never mark an invoice paid merely because a payment request was opened on the device.
 */
interface PaymentGateway {
    val providerId: String
    suspend fun createPayment(request: WalletPaymentRequest): WalletPaymentResult
    suspend fun verifyPayment(providerTransactionId: String): WalletVerificationResult
}

data class WalletPaymentRequest(
    val invoiceId: String,
    val invoiceNumber: String,
    val amount: Long,
    val currencyCode: String,
    val customerPhone: String
)

data class WalletPaymentResult(
    val accepted: Boolean,
    val providerTransactionId: String? = null,
    val paymentUrl: String? = null,
    val message: String = ""
)

data class WalletVerificationResult(
    val verified: Boolean,
    val paidAmount: Long = 0L,
    val currencyCode: String = "",
    val providerTransactionId: String = "",
    val message: String = ""
)
