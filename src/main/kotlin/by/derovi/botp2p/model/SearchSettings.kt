package by.derovi.botp2p.model

import by.derovi.botp2p.exchange.Currency
import by.derovi.botp2p.exchange.PaymentMethod
import by.derovi.botp2p.exchange.Token
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
data class SearchSettings(
    @Id
    @GeneratedValue
    var id: Long,
    @Enumerated(value = EnumType.STRING)
    @ElementCollection(targetClass = Token::class)
    var tokens: MutableList<Token>,
    @ElementCollection
    var exchanges: MutableList<String>,
    var buyMakerBinance: Boolean,
    @ElementCollection(targetClass = CurrencyAndPaymentMethod::class)
    var paymentMethods: MutableList<CurrencyAndPaymentMethod>,
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