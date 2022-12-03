package by.derovi.botp2p.exchange.exchanges

import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.exchange.NetworkUtils
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.entity.ContentType
import kotlin.math.min

object Bybit : Exchange {

    override fun getFetchTasks(): List<() -> Map<Setup, List<Offer>>> {
        val fetchTasks = mutableListOf<() -> Map<Setup, List<Offer>>>()
        for (token in supportedTokens()) {
            for (currency in supportedCurrencies()) {
                for (orderType in OrderType.values()) {
                    for (page in 0 until 4) {
                        fetchTasks.add {
                            val payload = requestPayload(token, currency, orderType, page)
                            val data = NetworkUtils.postRequest(
                                "https://api2.bybit.com/spot/api/otc/item/list",
                                payload,
                                ContentType.APPLICATION_FORM_URLENCODED
                            )
                            return@add parseResponse(token, currency, orderType, data).groupBy {
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

    fun parseResponse(token: Token, currency: Currency, orderType: OrderType, data: String): List<Offer> =
        ObjectMapper().readTree(data)["result"]["items"].map { entry ->
            entry["payments"].mapNotNull { codeToPaymentMethod[it.asInt()] }.map { paymentMethod ->
                val price = entry["price"].asDouble()
                val available = entry["lastQuantity"].asDouble()
                Offer(
                    price,
                    token,
                    orderType,
                    available,
                    entry["minAmount"].asDouble(),
                    min(entry["maxAmount"].asDouble(), price * available),
                    entry["nickName"].asText(),
                    entry["recentExecuteRate"].asInt(),
                    entry["recentOrderNum"].asInt(),
                    entry["isOnline"].asBoolean(),
                    "https://www.bybit.com/fiat/trade/otc/" +
                            "?actionType=${if (orderType == OrderType.BUY) 1 else 0}" +
                            "&token=${tokenToCode[token]}" +
                            "&fiat=${currencyToCode[currency]}" +
                            "&paymentMethod=${paymentMethod}",
                    paymentMethod,
                    this
                )
            }//.filter { it.isOnline }
        }.flatten()

    fun requestPayload(token: Token, currency: Currency, orderType: OrderType, page: Int) =
        "userId=" +
        "&tokenId=${tokenToCode[token]}" +
        "&currencyId=${currencyToCode[currency]}" +
        "&payment=" +
        "&side=${if (orderType == OrderType.BUY) 1 else 0}" +
        "&size=20" +
        "&page=$page" +
        "&amount="

    val tokenToCode = mapOf(
        Token.BTC to "BTC",
        Token.USDC to "USDC",
        Token.USDT to "USDT",
        Token.ETH to "ETH",
    )

    private val currencyToCode = mapOf(
        Currency.RUB to "RUB",
    )

    private val codeToPaymentMethod = mapOf(
        14 to PaymentMethod.BANK_TRANSFER,
        27 to PaymentMethod.FPS,
        44 to PaymentMethod.MTSBANK,
        49 to PaymentMethod.OTPBANK,
        51 to PaymentMethod.PAYEER,
        56 to PaymentMethod.PERFECT_MONEY,
        59 to PaymentMethod.POSTBANK,
        62 to PaymentMethod.QIWI,
        64 to PaymentMethod.RAIFAIZEN,
        66 to PaymentMethod.ROSSELHOZBANK,
        72 to PaymentMethod.SPORTBANK,
        75 to PaymentMethod.TINKOFF,
        88 to PaymentMethod.YANDEXMONEY,
        102 to PaymentMethod.HOMECREDIT,
        173 to PaymentMethod.ABSOLUTBANK,
        185 to PaymentMethod.ROSBANK,
        231 to PaymentMethod.CITIBANK,
        333 to PaymentMethod.PARITETBANK,
        332 to PaymentMethod.MTBANK,
        377 to PaymentMethod.SBERBANK,
        378 to PaymentMethod.GAZPROMBANK,
        379 to PaymentMethod.ALFA_BANK,
        380 to PaymentMethod.OTKRITIE,
        381 to PaymentMethod.VTB,
        382 to PaymentMethod.SBP_TRANSFER
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
        return "Bybit"
    }
}