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
    ) = listOf<Offer>()

    override fun getFetchTasks(): List<() -> Map<Setup, List<Offer>>> {
        val fetchTasks = mutableListOf<() -> Map<Setup, List<Offer>>>()
        for (token in supportedTokens()) {
            for (currency in supportedCurrencies()) {
                for (orderType in OrderType.values()) {
                    for (page in 0 until 4) {
                        fetchTasks.add {
                            val url = requestUrl(token, currency, orderType, page)
                            val data = NetworkUtils.getRequest(url)
                            return@add parseResponse(token, orderType, data).groupBy {
                                Setup(token, currency, this, it.paymentMethod!!, orderType)
                            }
                        }
                    }
                }
            }
        }
        return fetchTasks
    }

    fun parseResponse(token: Token, orderType: OrderType, data: String) =
        ObjectMapper().readTree(data)["data"].map { entry ->
            entry["payMethods"].mapNotNull { idToPaymentMethod[it["payMethodId"].asInt()] }.map { paymentMethod ->
                Offer(
                    entry["price"].asDouble(),
                    token,
                    orderType,
                    entry["tradeCount"].asDouble(),
                    entry["minTradeLimit"].asDouble(),
                    entry["maxTradeLimit"].asDouble(),
                    entry["userName"].asText(),
                    entry["orderCompleteRate"].asInt(),
                    entry["tradeMonthTimes"].asInt(),
                    entry["isOnline"].asBoolean(),
                    "https://www.huobi.com/ru-ru/fiat-crypto/trade/" +
                            "${if (orderType == OrderType.BUY) "buy" else "sell"}-${token.name.lowercase()}/",
                    paymentMethod,
                    this
                )
            }.filter { it.isOnline }
        }.flatten()

    fun requestUrl(coin: Token, currency: Currency, orderType: OrderType, page: Int) =
        "https://otc-api.trygofast.com/v1/data/trade-market" +
                "?coinId=${coinToId[coin]}" +
                "&currency=${currencyToId[currency]}" +
                "&tradeType=${ if (orderType == OrderType.BUY) "sell" else "buy" }" +
                "&currPage=$page" +
                "&payMethod=0" +
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

    private val idToPaymentMethod = mapOf(
        28 to PaymentMethod.TINKOFF,
        358 to PaymentMethod.ROSBANK,
        36 to PaymentMethod.RAIFAIZEN,
        9 to PaymentMethod.QIWI,
        29 to PaymentMethod.SBERBANK,
        357 to PaymentMethod.POSTBANK,
        356 to PaymentMethod.MTSBANK,
        26 to PaymentMethod.RUSSIANSTANDARD,
        45 to PaymentMethod.OTPBANK,
        363 to PaymentMethod.UNICREDIT,
        360 to PaymentMethod.CITIBANK,
        170 to PaymentMethod.BCSBANK,
        19 to PaymentMethod.YANDEXMONEY,
        179 to PaymentMethod.URALSIB,
        351 to PaymentMethod.GAZPROMBANK
    )

    override fun supportedTokens(): Array<Token> {
        return coinToId.keys.toTypedArray()
    }

    override fun supportedPaymentMethods(): Array<PaymentMethod> {
        return idToPaymentMethod.values.toTypedArray()
    }

    override fun supportedCurrencies(): Array<Currency> {
        return currencyToId.keys.toTypedArray()
    }

    override fun name(): String {
        return "Huobi"
    }
}