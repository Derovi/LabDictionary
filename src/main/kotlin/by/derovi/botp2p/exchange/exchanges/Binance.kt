package by.derovi.botp2p.exchange.exchanges

import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.exchange.NetworkUtils
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.entity.ContentType

object Binance : Exchange {
    override fun fetch(
        orderType: OrderType,
        token: Token,
        currency: Currency,
        paymentMethod: PaymentMethod
    ): List<Offer> {
        val result = mutableListOf<Offer>()
        val payload = requestPayload(token, currency, orderType, paymentMethod, 1)
        val data = NetworkUtils.postRequest("https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search", payload, ContentType.APPLICATION_JSON)
        result.addAll(parseResponse(token, orderType, paymentMethod, currency, data))
        result.forEach {
            it.paymentMethod = paymentMethod
            it.exchange = this
        }
        return result
    }

    fun parseResponse(token: Token, orderType: OrderType, paymentMethod: PaymentMethod, currency: Currency, data: String) =
        ObjectMapper().readTree(data)["data"].map { Offer(
            it["adv"]["price"].asDouble(),
            token,
            orderType,
            it["adv"]["surplusAmount"].asDouble(),
            it["adv"]["minSingleTransAmount"].asDouble(),
            it["adv"]["maxSingleTransAmount"].asDouble(),
            it["advertiser"]["nickName"].asText(),
            (it["advertiser"]["monthFinishRate"].asDouble() * 100).toInt(),
            it["advertiser"]["monthOrderCount"].asInt(),
            true,
            "https://p2p.binance.com/ru/trade/" +
                    (if (orderType == OrderType.SELL) "sell/" else "${paymentMethodToCode[paymentMethod]}/") +
                    "${tokenToCode[token]}?fiat=${currencyToCode[currency]}" +
                    if (orderType == OrderType.SELL) "&payment=${paymentMethodToCode[paymentMethod]}" else ""
        ) }.filter { it.isOnline }

    fun requestPayload(token: Token, currency: Currency, orderType: OrderType, paymentMethod: PaymentMethod, page: Int) =
         "{\"proMerchantAds\":false," +
         "\"page\":$page," +
         "\"rows\":10," +
         "\"payTypes\":[\"${paymentMethodToCode[paymentMethod]}\"]," +
         "\"countries\":[]," +
         "\"publisherType\":null," +
         "\"asset\":\"${tokenToCode[token]}\"," +
         "\"fiat\":\"${currencyToCode[currency]}\"," +
         "\"tradeType\":\"${ if (orderType == OrderType.BUY) "BUY" else "SELL" }\"}"

    val tokenToCode = mapOf(
        Token.BTC to "BTC",
        Token.USDT to "USDT",
        Token.ETH to "ETH",
        Token.BNB to "BNB"
    )

    val currencyToCode = mapOf(
        Currency.RUB to "RUB",
//        Currency.USD to "USD"
    )

    private val paymentMethodToCode = mapOf(
        PaymentMethod.TINKOFF to "TinkoffNew",
        PaymentMethod.ROSBANK to "RosBank",
        PaymentMethod.RAIFAIZEN to "RaiffeisenBank",
        PaymentMethod.QIWI to "QIWI",
        PaymentMethod.POSTBANK to "PostBankRussia",
        PaymentMethod.MTSBANK to "MTSBank",
        PaymentMethod.RUSSIANSTANDARD to "RussianStandardBank",
        PaymentMethod.OTPBANK to "OTPBankRussia",
        PaymentMethod.UNICREDIT to "UniCreditRussia",
        PaymentMethod.CITIBANK to "CitibankRussia",
        PaymentMethod.BCSBANK to "BCSBank",
        PaymentMethod.YANDEXMONEY to "YandexMoneyNew",
        PaymentMethod.URALSIB to "UralsibBank"
    )

    override fun supportedTokens(): Array<Token> {
        return tokenToCode.keys.toTypedArray()
    }

    override fun supportedPaymentMethods(): Array<PaymentMethod> {
        return paymentMethodToCode.keys.toTypedArray()
    }

    override fun supportedCurrencies(): Array<Currency> {
        return currencyToCode.keys.toTypedArray()
    }

    override fun name(): String {
        return "Binance"
    }
}