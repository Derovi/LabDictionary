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
    ): List<Offer> = listOf()

    override fun getFetchTasks(): List<() -> Map<Setup, List<Offer>>> {
        val fetchTasks = mutableListOf<() -> Map<Setup, List<Offer>>>()
        for (token in supportedTokens()) {
            for (currency in supportedCurrencies()) {
                for (orderType in OrderType.values()) {
                    fetchTasks.add {
                        val url = requestUrl(token, currency, orderType)
                        val data = NetworkUtils.getRequest(url)
                        return@add parseResponse(token, currency, orderType, data).groupBy {
                            Setup(token, currency, this, it.paymentMethod!!, orderType)
                        }
                    }
                }
            }
        }
        return fetchTasks
    }

    fun parseResponse(token: Token, currency: Currency, orderType: OrderType, data: String): List<Offer> {
        return ObjectMapper().readTree(data)["data"][if (orderType == OrderType.BUY) "sell" else "buy"].map { entry ->
            entry["paymentMethods"].mapNotNull { codeToPaymentMethod[it.asText()] }.map { paymentMethod ->
                Offer(
                    entry["price"].asDouble(),
                    token,
                    orderType,
                    entry["availableAmount"].asDouble(),
                    entry["quoteMinAmountPerOrder"].asDouble(),
                    entry["quoteMaxAmountPerOrder"].asDouble(),
                    entry["nickName"].asText(),
                    (entry["completedRate"].asDouble() * 100).toInt(),
                    entry["completedOrderQuantity"].asInt(),
                    true,
                    "https://www.okx.com/ru/p2p-markets/" +
                            "${currencyToCode[currency]}/" +
                            "${if (orderType == OrderType.BUY) "buy" else "sell"}-${tokenToCode[token]}",
                    paymentMethod,
                    this
                )
            }
        }.flatten()
    }

    fun requestUrl(token: Token, currency: Currency, orderType: OrderType) =
        "https://www.okx.com/v3/c2c/tradingOrders/books" +
                "?t=${System.currentTimeMillis()}" +
                "&quoteCurrency=${currencyToCode[currency]}" +
                "&baseCurrency=${tokenToCode[token]}" +
                "&side=${if (orderType == OrderType.BUY) "sell" else "buy"}" +
                "&paymentMethod=all" +
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

    private val codeToPaymentMethod = mapOf(
        "Ak+Bars+Bank" to PaymentMethod.AK_BARS_BANK,
        "Alfa+Bank" to PaymentMethod.ALFA_BANK,
        "bank" to PaymentMethod.BANK_TRANSFER,
        "Gazprombank" to PaymentMethod.GAZPROMBANK,
        "MKB" to PaymentMethod.MKB,
        "Otkritie" to PaymentMethod.OTKRITIE,
        "Payeer" to PaymentMethod.PAYEER,
        "Pochta+Bank" to PaymentMethod.POSTBANK,
        "PSB" to PaymentMethod.PSB,
        "QiWi" to PaymentMethod.QIWI,
        "Raiffaizen" to PaymentMethod.RAIFAIZEN,
        "Rosbank" to PaymentMethod.ROSBANK,
        "Rosselhozbank" to PaymentMethod.ROSSELHOZBANK,
        "Russian+Standard+Bank" to PaymentMethod.RUSSIANSTANDARD,
        "Sberbank" to PaymentMethod.SBERBANK,
        "SBP+Fast+Bank+Transfer" to PaymentMethod.SBP_TRANSFER,
        "Sovcombank" to PaymentMethod.SOVCOMBANK,
        "Tinkoff" to PaymentMethod.TINKOFF,
        "UniCredit+Bank" to PaymentMethod.UNICREDIT,
        "Uralsib+Bank" to PaymentMethod.URALSIB,
        "VTB" to PaymentMethod.VTB,
        "Yandex.Money" to PaymentMethod.YANDEXMONEY
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
        return "OKX"
    }
}