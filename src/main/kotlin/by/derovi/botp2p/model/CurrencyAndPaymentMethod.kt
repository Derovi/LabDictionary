package by.derovi.botp2p.model

import by.derovi.botp2p.exchange.Currency
import by.derovi.botp2p.exchange.PaymentMethod
import javax.persistence.Embeddable
import javax.persistence.EnumType
import javax.persistence.Enumerated

@Embeddable
class CurrencyAndPaymentMethod(
    @Enumerated(EnumType.STRING)
    var currency: Currency,
    @Enumerated(EnumType.STRING)
    var paymentMethod: PaymentMethod
)

