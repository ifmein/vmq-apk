package vmq.model

data class PaymentEvent(
    val type: PaymentType,
    val amount: Double,
)
