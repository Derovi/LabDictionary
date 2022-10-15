package by.derovi.botp2p.model

import by.derovi.botp2p.exchange.TradingMode
import by.derovi.botp2p.exchange.Currency
import by.derovi.botp2p.exchange.PaymentMethod
import by.derovi.botp2p.exchange.Token
import javax.persistence.*

@Embeddable
data class UserSettings(
    var notificationThreshold: Double?, // 0 - 100 percent
    var useSpot: Boolean,
    @Enumerated(value = EnumType.STRING)
    var tradingMode: TradingMode,
    @Enumerated(value = EnumType.STRING)
    @ElementCollection(targetClass = Token::class)
    var tokens: MutableList<Token>,
    @ElementCollection
    var exchanges: MutableList<String>,
    @ElementCollection(targetClass = CurrencyAndPaymentMethod::class)
    var paymentMethods: MutableList<CurrencyAndPaymentMethod>,
    var lang: String?,
    var minimumValue: Int,
    var workValue: Int,
    @ElementCollection(fetch = FetchType.EAGER)
    var banned: MutableList<Maker>,
    @Enumerated(value = EnumType.STRING)
    var chosenCurrency: Currency?
) {
    val paymentMethodsAsMap: Map<Currency, List<PaymentMethod>>
        get() {
            val result = mutableMapOf<Currency, MutableList<PaymentMethod>>()
            for (currencyAndPM in paymentMethods) {
                result.getOrPut(currencyAndPM.currency) { mutableListOf() }.add(currencyAndPM.paymentMethod)
            }
            return result
        }
}