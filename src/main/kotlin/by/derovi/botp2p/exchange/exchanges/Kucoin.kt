package by.derovi.botp2p.exchange.exchanges

import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.exchange.NetworkUtils
import com.fasterxml.jackson.databind.ObjectMapper

object Kucoin : Exchange {
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
                        val url = requestUrl(token, currency, orderType, 1, 100)
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

    fun parseResponse(token: Token, currency: Currency, orderType: OrderType, data: String) =
        ObjectMapper()
            .readTree(data)["items"]
            .map { entry ->
                entry["adPayTypes"]
                .map { adPayType ->
                    Offer(
                        entry["floatPrice"].asDouble(),
                        token,
                        orderType,
                        entry["currencyBalanceQuantity"].asDouble(),
                        entry["limitMinQuote"].asDouble(),
                        entry["limitMaxQuote"].asDouble(),
                        entry["nickName"].asText(),
                        entry["dealOrderRate"]
                            .asText()
                            .substringBefore("%")
                            .toDoubleOrNull()
                            ?.toInt() ?: 0,
                        entry["dealOrderNum"].asInt(),
                        true,
                        "https://www.kucoin.com/ru/otc" +
                                "/${if (orderType == OrderType.BUY) "buy" else "sell"}" +
                                "/${tokenToId[token]}" +
                                "/${currencyToId[currency]}",
                        idToPaymentMethod[adPayType["payTypeCode"].asText()],
                        this
                    )
                }.filter { it.paymentMethod != null }
            }.flatten()

    fun requestUrl(token: Token, currency: Currency, orderType: OrderType, page: Int, pageSize: Int) =
        "https://www.kucoin.com/_api/otc/ad/list" +
                "?currency=${tokenToId[token]}" +
                "&side=${if(orderType == OrderType.BUY) "SELL" else "BUY"}" +
                "&legal=${currencyToId[currency]}" +
                "&page=$page" +
                "&pageSize=$pageSize" +
                "&status=PUTUP" +
                "&lang=ru_RU"

    val tokenToId = mapOf(
        Token.BTC to "BTC",
        Token.USDT to "USDT",
        Token.ETH to "ETH",
        Token.USDC to "USDC",
    )

    private val currencyToId = mapOf(
        Currency.RUB to "RUB",
//        Currency.USD to 2
    )

    private val idToPaymentMethod = mapOf(
        "QIWI" to PaymentMethod.QIWI,
        "BANK" to PaymentMethod.BANK_TRANSFER,
        "BANK_TRANSFER" to PaymentMethod.BANK_TRANSFER,
        "PAYEER" to PaymentMethod.PAYEER,
    )

    override fun supportedTokens(): Array<Token> {
        return tokenToId.keys.toTypedArray()
    }

    override fun supportedPaymentMethods(): Array<PaymentMethod> {
        return idToPaymentMethod.values.toTypedArray()
    }

    override fun supportedCurrencies(): Array<Currency> {
        return currencyToId.keys.toTypedArray()
    }

    override fun name(): String {
        return "Kucoin"
    }
}