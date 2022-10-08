package by.derovi.botp2p.exchange

data class Setup(
    val coin: Token,
    val currency: Currency,
    val exchange: Exchange,
    val paymentMethod: PaymentMethod,
    val orderType: OrderType
)
