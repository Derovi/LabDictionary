package by.derovi.botp2p.exchange.exchanges

import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.exchange.NetworkUtils
import com.fasterxml.jackson.databind.ObjectMapper

object Huobi : Exchange {
    override fun fetch(
        orderType: OrderType,
        token: Token,
        currency: Currency,
        paymentMethod: PaymentMethod
    ): List<Offer> {
        val result = mutableListOf<Offer>()
        val url = requestUrl(token, currency, orderType, paymentMethod, 1)
        val data = NetworkUtils.getRequest(url)
        result.addAll(parseResponse(token, orderType, data))
        result.forEach {
            it.paymentMethod = paymentMethod
            it.exchange = this
        }
        return result
    }

    fun parseResponse(token: Token, orderType: OrderType, data: String) =
        ObjectMapper().readTree(data)["data"].map { Offer(
            it["price"].asDouble(),
            token,
            orderType,
            it["tradeCount"].asDouble(),
            it["minTradeLimit"].asDouble(),
            it["maxTradeLimit"].asDouble(),
            it["userName"].asText(),
            it["orderCompleteRate"].asInt(),
            it["tradeMonthTimes"].asInt(),
            it["isOnline"].asBoolean(),
            "https://www.huobi.com/ru-ru/fiat-crypto/trade/${if (orderType == OrderType.BUY) "buy" else "sell"}-${token.name.lowercase()}/"
        ) }.filter { it.isOnline }

    fun requestUrl(coin: Token, currency: Currency, orderType: OrderType, paymentMethod: PaymentMethod, page: Int) =
        "https://otc-api.trygofast.com/v1/data/trade-market" +
                "?coinId=${coinToId[coin]}" +
                "&currency=${currencyToId[currency]}" +
                "&tradeType=${ if (orderType == OrderType.BUY) "sell" else "buy" }" +
                "&currPage=$page" +
                "&payMethod=${paymentMethodToId[paymentMethod]}" +
                "&acceptOrder=0" +
                "&country=" +
                "&blockType=general" +
                "&online=1" +
                "&range=" +
                "0&amount=" +
                "&onlyTradable=false"

    val coinToId = mapOf(
        Token.BTC to 1,
        Token.USDT to 2,
        Token.ETH to 3,
//        Token.EOS to 5,
//        Token.XRP to 7,
//        Token.LTC to 8
    )

    private val currencyToId = mapOf(
        Currency.RUB to 11,
//        Currency.USD to 2
    )

    private val paymentMethodToId = mapOf(
        PaymentMethod.TINKOFF to 28,
        PaymentMethod.ROSBANK to 358,
        PaymentMethod.RAIFAIZEN to 36,
        PaymentMethod.QIWI to 9,
        PaymentMethod.SBERBANK to 29,
        PaymentMethod.POSTBANK to 357,
        PaymentMethod.MTSBANK to 356,
        PaymentMethod.RUSSIANSTANDARD to 26,
        PaymentMethod.OTPBANK to 45,
        PaymentMethod.UNICREDIT to 363,
        PaymentMethod.CITIBANK to 360,
        PaymentMethod.BCSBANK to 170,
        PaymentMethod.YANDEXMONEY to 19,
        PaymentMethod.URALSIB to 179,
        PaymentMethod.GAZPROMBANK to 351
    )

    override fun supportedTokens(): Array<Token> {
        return coinToId.keys.toTypedArray()
    }

    override fun supportedPaymentMethods(): Array<PaymentMethod> {
        return paymentMethodToId.keys.toTypedArray()
    }

    override fun supportedCurrencies(): Array<Currency> {
        return currencyToId.keys.toTypedArray()
    }

    override fun name(): String {
        return "Huobi"
    }
}