package by.derovi.botp2p.exchange.exchanges

import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.exchange.NetworkUtils
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.entity.ContentType

object Binance : Exchange {
    override fun getFetchTasks(): List<() -> Map<Setup, List<Offer>>> {
        val fetchTasks = mutableListOf<() -> Map<Setup, List<Offer>>>()
        for (token in supportedTokens()) {
            for (currency in supportedCurrencies()) {
                for (orderType in OrderType.values()) {
                    for (page in 0 until 4) {
                        fetchTasks.add {
                            val payload = requestPayload(token, currency, orderType, page)
                            val data = NetworkUtils.postRequest(
                                "https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search",
                                payload,
                                ContentType.APPLICATION_JSON
                            )
                            return@add parseResponse(token, orderType, currency, data).groupBy {
                                Setup(token, currency, this, it.paymentMethod!!, orderType)
                            }
                        }
                    }
                }
            }
        }
        return fetchTasks
    }

    override fun fetch(
        orderType: OrderType,
        token: Token,
        currency: Currency,
        paymentMethod: PaymentMethod
    ): List<Offer> = listOf()

    fun parseResponse(token: Token, orderType: OrderType, currency: Currency, data: String) =
        ObjectMapper().readTree(data)["data"].map { entry ->
            entry["adv"]["tradeMethods"]
                .mapNotNull { codeToPaymentMethod[it["identifier"].asText()] }
                .map { paymentMethod ->
                val price = entry["adv"]["price"].asDouble()
                val available = entry["adv"]["surplusAmount"].asDouble()
                Offer(
                    price,
                    token,
                    orderType,
                    available,
                    entry["adv"]["minSingleTransAmount"].asDouble(),
                    price * available,
                    entry["advertiser"]["nickName"].asText(),
                    (entry["advertiser"]["monthFinishRate"].asDouble() * 100).toInt(),
                    entry["advertiser"]["monthOrderCount"].asInt(),
                    true,
                    "https://p2p.binance.com/ru/trade/" +
                            (if (orderType == OrderType.SELL) "sell/" else "${paymentMethod}/") +
                            "${tokenToCode[token]}?fiat=${currencyToCode[currency]}" +
                            if (orderType == OrderType.SELL) "&payment=${paymentMethod}" else "",
                    paymentMethod,
                    this
                )
            }
        }.flatten()

    fun requestPayload(token: Token, currency: Currency, orderType: OrderType, page: Int) =
         "{\"proMerchantAds\":false," +
         "\"page\":$page," +
         "\"rows\":20," +
         "\"payTypes\":[]," +
         "\"countries\":[]," +
         "\"publisherType\":null," +
         "\"asset\":\"${tokenToCode[token]}\"," +
         "\"fiat\":\"${currencyToCode[currency]}\"," +
         "\"tradeType\":\"${ if (orderType == OrderType.BUY) "BUY" else "SELL" }\"}"

    val tokenToCode = mapOf(
        Token.BTC to "BTC",
        Token.USDT to "USDT",
        Token.BUSD to "BUSD",
        Token.ETH to "ETH",
        Token.BNB to "BNB"
    )

    val currencyToCode = mapOf(
        Currency.RUB to "RUB",
//        Currency.USD to "USD"
    )

    private val codeToPaymentMethod = mapOf(
        "TinkoffNew" to PaymentMethod.TINKOFF,
        "RosBankNew" to PaymentMethod.ROSBANK,
        "RaiffeisenBank" to PaymentMethod.RAIFAIZEN,
        "QIWI" to PaymentMethod.QIWI,
        "PostBankRussia" to PaymentMethod.POSTBANK,
        "MTSBank" to PaymentMethod.MTSBANK,
        "RussianStandardBank" to PaymentMethod.RUSSIANSTANDARD,
        "OTPBankRussia" to PaymentMethod.OTPBANK,
        "UniCreditRussia" to PaymentMethod.UNICREDIT,
        "CitibankRussia" to PaymentMethod.CITIBANK,
        "BCSBank" to PaymentMethod.BCSBANK,
        "YandexMoneyNew" to PaymentMethod.YANDEXMONEY,
        "UralsibBank" to PaymentMethod.URALSIB
    )

    override fun supportedTokens(): Array<Token> {
        return tokenToCode.keys.toTypedArray()
    }

    override fun supportedPaymentMethods(): Array<PaymentMethod> {
        return codeToPaymentMethod.values.toTypedArray()
    }

    override fun supportedCurrencies(): Array<Currency> {
        return currencyToCode.keys.toTypedArray()
    }

    override fun name(): String {
        return "Binance"
    }
}