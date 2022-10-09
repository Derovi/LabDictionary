package by.derovi.botp2p.exchange.exchanges

import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.exchange.NetworkUtils
import com.fasterxml.jackson.databind.ObjectMapper

object OKX : Exchange {

    override fun fetch(
        orderType: OrderType,
        token: Token,
        currency: Currency,
        paymentMethod: PaymentMethod
    ): List<Offer> {
        val result = mutableListOf<Offer>()
        val url = requestUrl(token, currency, orderType, paymentMethod, 1)
        val data = NetworkUtils.getRequest(url)
        result.addAll(parseResponse(token, currency, url, orderType, data))
        result.forEach {
            it.paymentMethod = paymentMethod
            it.exchange = this
        }
        return result
    }

    fun parseResponse(token: Token, currency: Currency, url: String, orderType: OrderType, data: String): List<Offer> {
        return ObjectMapper().readTree(data)["data"][if (orderType == OrderType.BUY) "sell" else "buy"].map {
            Offer(
                it["price"].asDouble(),
                token,
                orderType,
                it["availableAmount"].asDouble(),
                it["quoteMinAmountPerOrder"].asDouble(),
                it["quoteMaxAmountPerOrder"].asDouble(),
                it["nickName"].asText(),
                (it["completedRate"].asDouble() * 100).toInt(),
                it["completedOrderQuantity"].asInt(),
                true,
                "https://www.okx.com/ru/p2p-markets/" +
                        "${currencyToCode[currency]}/" +
                        "${if (orderType == OrderType.BUY) "buy" else "sell"}-${tokenToCode[token]}"
            )
        }
    }

    fun requestUrl(token: Token, currency: Currency, orderType: OrderType, paymentMethod: PaymentMethod, page: Int) =
        "https://www.okx.com/v3/c2c/tradingOrders/books" +
                "?t=${System.currentTimeMillis()}" +
                "&quoteCurrency=${currencyToCode[currency]}" +
                "&baseCurrency=${tokenToCode[token]}" +
                "&side=${if (orderType == OrderType.BUY) "sell" else "buy"}" +
                "&paymentMethod=${paymentMethodToCode[paymentMethod]}" +
                "&userType=all" +
                "&showTrade=false" +
                "&showFollow=false" +
                "&showAlreadyTraded=false" +
                "&isAbleFilter=false"

    val tokenToCode = mapOf(
        Token.BTC to "btc",
        Token.USDC to "usdc",
        Token.USDT to "usdt",
        Token.ETH to "eth",
    )

    private val currencyToCode = mapOf(
        Currency.RUB to "rub",
    )

    private val paymentMethodToCode = mapOf(
        PaymentMethod.AK_BARS_BANK to "Ak+Bars+Bank",
        PaymentMethod.ALFA_BANK to "Alfa+Bank",
        PaymentMethod.BANK_TRANSFER to "bank",
        PaymentMethod.GAZPROMBANK to "Gazprombank",
        PaymentMethod.MKB to "MKB",
        PaymentMethod.OTKRITIE to "Otkritie",
        PaymentMethod.PAYEER to "Payeer",
        PaymentMethod.POSTBANK to "Pochta+Bank",
        PaymentMethod.PSB to "PSB",
        PaymentMethod.QIWI to "QiWi",
        PaymentMethod.RAIFAIZEN to "Raiffaizen",
        PaymentMethod.ROSBANK to "Rosbank",
        PaymentMethod.ROSSELHOZBANK to "Rosselhozbank",
        PaymentMethod.RUSSIANSTANDARD to "Russian+Standard+Bank",
        PaymentMethod.SBERBANK to "Sberbank",
        PaymentMethod.SBP_TRANSFER to "SBP+Fast+Bank+Transfer",
        PaymentMethod.SOVCOMBANK to "Sovcombank",
        PaymentMethod.TINKOFF to "Tinkoff",
        PaymentMethod.UNICREDIT to "UniCredit+Bank",
        PaymentMethod.URALSIB to "Uralsib+Bank",
        PaymentMethod.VTB to "VTB",
        PaymentMethod.YANDEXMONEY to "Yandex.Money"
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
        return "OKX"
    }
}