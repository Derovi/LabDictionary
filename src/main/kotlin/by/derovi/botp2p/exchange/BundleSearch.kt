package by.derovi.botp2p.exchange

import by.derovi.botp2p.exchange.exchanges.Binance
import by.derovi.botp2p.library.PoolWithRetries
import by.derovi.botp2p.model.Maker
import by.derovi.botp2p.model.SearchSettings
import by.derovi.botp2p.pairs
import by.derovi.botp2p.services.FeesService
import by.derovi.botp2p.services.SpotService
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors
import kotlin.math.min

const val OFFERS_LIMIT_FOR_BUNDLE = 5

class BundleSearch(val commonExchanges: Array<Exchange>) {
    @Autowired
    lateinit var spotService: SpotService

    @Autowired
    lateinit var feesService: FeesService

    var setupToOffers = ConcurrentHashMap<Setup, MutableList<Offer>>()
    var lastFetchData = 0L

    fun fetchData() {
        // clear
        val newSetupToOffers = ConcurrentHashMap<Setup, MutableList<Offer>>()
        // fetch
        val pool = PoolWithRetries(15)
        commonExchanges.asSequence().map(Exchange::getFetchTasks).flatten().map {{
            it().forEach { (setup, offers) ->
                newSetupToOffers.getOrPut(setup) { mutableListOf() }
                    .addAll(offers.filter { it.completeRate == null || it.completeRate > 49 })
            }
        }}.forEach(pool::addTask)
        pool.shuffleTasks()
        pool.executeTasks()

        // sort
        for ((setup, offers) in newSetupToOffers) {
            if (offers.isEmpty()) {
                continue
            }

            if (setup.orderType == OrderType.BUY) {
                offers.sortBy { it.price }
            } else {
                offers.sortByDescending { it.price }
            }
            offers.dropLast((offers.size - OFFERS_LIMIT_FOR_BUNDLE).coerceAtLeast(0))
        }

        lastFetchData = System.currentTimeMillis()
        setupToOffers = newSetupToOffers
    }

//    fun offersWithRestrictions(
//        searchSettingsMap: Map<Boolean, Map<Boolean, SearchSettingsWrapper>>,
//        token: Token,
//        currency: Currency,
//        exchange: Exchange,
//        paymentMethods: Map<Currency, List<PaymentMethod>>,
//        tradingMode: TradingMode,
//        bannedMakers: List<Maker>,
//        minValue: Int,
//        workValue: Int,
//    ): Pair<List<Offer>, List<Offer>> {
//        fun checkRestrictions(
//            buy: Boolean,
//            taker: Boolean,
//            paymentMethod: PaymentMethod
//        ) = with(searchSettingsMap[buy]!![taker]!!) {
//            exchanges.contains(exchange) &&
//            tokens.contains(token) &&
//            paymentMethods[currency]?.contains(paymentMethod) == true
//        }
//
//        val buyOffers = mutableListOf<Offer>()
//        val sellOffers = mutableListOf<Offer>()
//
//        fun criteria(offer: Offer) =
//            spotService.getUSDTAmount(offer.maxLimit, token, offer.price) >= minValue
//            &&spotService.getUSDTAmount( offer.minLimit, token, offer.price) <= workValue
//            && !bannedMakers.contains(Maker(offer.username, offer.exchange!!.name()))
//
//        val currentPaymentMethods = paymentMethods.getOrDefault(currency, listOf())
//        for (paymentMethod in currentPaymentMethods) {
//            setupToOffers[Setup(token, currency, exchange, paymentMethod, OrderType.BUY)]?.filter(::criteria)?.let {
//                if (checkRestrictions(true, true, paymentMethod)) {
//                    buyOffers.addAll(it) // buy taker
//                }
//                if (tradingMode != TradingMode.TAKER_TAKER && it.isNotEmpty()) {
//                    if (checkRestrictions(false, false, paymentMethod)) {
//                        sellOffers.add(it.first()) // sell maker
//                    }
//                }
//            }
//            setupToOffers[Setup(token, currency, exchange, paymentMethod, OrderType.SELL)]?.filter(::criteria)?.let {
//                if (tradingMode == TradingMode.MAKER_MAKER_BINANCE_MERCHANT
//                    || tradingMode == TradingMode.MAKER_MAKER_NO_BINANCE && exchange != Binance) {
//                    if (it.isNotEmpty()) {
//                        if (checkRestrictions(true, false, paymentMethod)) {
//                            buyOffers.add(it.first()) // buy maker
//                        }
//                    }
//                }
//                if (checkRestrictions(false, true, paymentMethod)) {
//                    sellOffers.addAll(it) // sell taker
//                }
//            }
//        }
//
//        buyOffers.sortBy { it.price }
//        sellOffers.sortByDescending { it.price }
//        return buyOffers to sellOffers
//    }

    fun offersWithRestrictions(
        searchSettingsMap: Map<Boolean, Map<Boolean, SearchSettingsWrapper>>,
        token: Token,
        currency: Currency,
        exchange: Exchange,
        paymentMethods: Map<Currency, List<PaymentMethod>>,
        tradingMode: TradingMode,
        bannedMakers: List<Maker>,
        minValue: Int,
        workValue: Int,
    ): Pair<List<Offer>, List<Offer>> {
        fun checkRestrictions(
            buy: Boolean,
            taker: Boolean,
            paymentMethod: PaymentMethod
        ) = with(searchSettingsMap[buy]!![taker]!!) {
            exchanges.contains(exchange) &&
                    tokens.contains(token) &&
                    paymentMethods[currency]?.contains(paymentMethod) == true
        }

        val buyOffersTaker = mutableListOf<Offer>()
        val buyOffersMaker = mutableListOf<Offer>()
        val sellOffersTaker = mutableListOf<Offer>()
        val sellOffersMaker = mutableListOf<Offer>()

        fun criteria(offer: Offer) =
            spotService.getUSDTAmount(offer.maxLimit, token, offer.price) >= minValue
                    &&spotService.getUSDTAmount( offer.minLimit, token, offer.price) <= workValue
                    && !bannedMakers.contains(Maker(offer.username, offer.exchange!!.name()))

        val currentPaymentMethods = paymentMethods.getOrDefault(currency, listOf())
        for (paymentMethod in currentPaymentMethods) {
            setupToOffers[Setup(token, currency, exchange, paymentMethod, OrderType.BUY)]?.filter(::criteria)?.let {
                if (checkRestrictions(true, true, paymentMethod)) {
                    buyOffersTaker.addAll(it) // buy taker
                }
                if (tradingMode != TradingMode.TAKER_TAKER && it.isNotEmpty()) {
                    if (checkRestrictions(false, false, paymentMethod)) {
                        sellOffersMaker.addAll(it.take(1)) // sell maker
                    }
                }
            }
            setupToOffers[Setup(token, currency, exchange, paymentMethod, OrderType.SELL)]?.filter(::criteria)?.let {
                if (tradingMode == TradingMode.MAKER_MAKER_BINANCE_MERCHANT
                    || tradingMode == TradingMode.MAKER_MAKER_NO_BINANCE && exchange != Binance) {
                    if (it.isNotEmpty()) {
                        if (checkRestrictions(true, false, paymentMethod)) {
                            buyOffersMaker.addAll(it.take(1)) // buy maker
                        }
                    }
                }
                if (checkRestrictions(false, true, paymentMethod)) {
                    sellOffersTaker.addAll(it) // sell taker
                }
            }
        }

        buyOffersTaker.sortBy { it.price }
        sellOffersTaker.sortByDescending { it.price }
        buyOffersMaker.sortByDescending { it.price }
        sellOffersMaker.sortBy { it.price }
        //merge

        return ((if (tradingMode == TradingMode.TAKER_TAKER || tradingMode == TradingMode.TAKER_MAKER)
            buyOffersTaker else buyOffersMaker)
        to (if (tradingMode == TradingMode.TAKER_TAKER)
            sellOffersTaker else sellOffersMaker))
    }

    fun <O, T, R> merge(
        originMap: Map<Boolean, Map<Boolean, O>>,
        setExtractor: (O) -> Set<T>,
        converter: (Set<T>) -> R
    ): R {
        fun getOrEmpty(buy: Boolean, taker: Boolean) = originMap[buy]?.get(taker)?.let(setExtractor) ?: emptySet()
        return converter(
            getOrEmpty(false, false) union
            getOrEmpty(false, true) union
            getOrEmpty(true, false) union
            getOrEmpty(true, true)
        )
    }

    class SearchSettingsWrapper(
        val tokens: List<Token>,
        val exchanges: List<Exchange>,
        val paymentMethodsAsMap: Map<Currency, List<PaymentMethod>>
    )

    fun Offer.usdtPrice() = this.price / spotService.price(this.token)

    fun searchBestPrices(
        searchSettingsMap: Map<Boolean, Map<Boolean, SearchSettingsWrapper>>,
        buy: Boolean,
        taker: Boolean,
        bannedMakers: List<Maker>,
        currency: Currency,
        minValue: Int,
        workValue: Int
    ): List<Offer> {
        val paymentMethods = merge(
            searchSettingsMap,
            { it.paymentMethodsAsMap[currency]?.toSet() ?: emptySet() },
            { mapOf(currency to it.toList()) }
        )
        val exchanges = merge(
            searchSettingsMap,
            { it.exchanges.toSet() },
            { it }
        )
        val tokens = merge(
            searchSettingsMap,
            { it.tokens.toSet() },
            { it }
        )

        val tradingMode = if (taker) TradingMode.TAKER_TAKER else TradingMode.MAKER_MAKER_BINANCE_MERCHANT
        val offers = mutableListOf<Offer>()
        for (exchange in exchanges) {
            for (token in tokens) {
                val buyAndSellOffers = offersWithRestrictions(
                    searchSettingsMap,
                    token,
                    currency,
                    exchange,
                    paymentMethods,
                    tradingMode,
                    bannedMakers,
                    minValue,
                    workValue
                )
                if (buy) {
                    offers.addAll(buyAndSellOffers.first)
                } else {
                    offers.addAll(buyAndSellOffers.second)
                }
            }
        }

        if (buy && taker || !buy && !taker) {
            offers.sortBy { it.usdtPrice() }
        } else {
            offers.sortByDescending { it.usdtPrice() }
        }
        return offers
    }

    fun searchBundles(
        searchSettingsMap: Map<Boolean, Map<Boolean, SearchSettingsWrapper>>,
        useSpot: Boolean,
        tradingMode: TradingMode,
        bannedMakers: List<Maker>,
        chosenCurrency: Currency?,
        minValue: Int,
        workValue: Int
    ): List<BundleSearchResult> {
        val result = mutableListOf<BundleSearchResult>()

        data class Key(val currency: Currency, val token: Token, val exchange: Exchange)
        val ketToBuyOffers = mutableMapOf<Key, List<Offer>>()
        val keyToSellOffers = mutableMapOf<Key, List<Offer>>()

        val paymentMethods = merge(
            searchSettingsMap,
            { it.paymentMethodsAsMap.entries },
            { it.stream().collect(Collectors.toMap(
                Map.Entry<Currency, List<PaymentMethod>>::key,
                Map.Entry<Currency, List<PaymentMethod>>::value)
            ) }
        )
        val exchanges = merge(
            searchSettingsMap,
            { it.exchanges.toSet() },
            { it }
        )
        val tokens = merge(
            searchSettingsMap,
            { it.tokens.toSet() },
            { it }
        )

        for (currency in paymentMethods.keys) {
            for (exchange in exchanges) {
                for (token in tokens) {
                    val offers = offersWithRestrictions(
                        searchSettingsMap,
                        token,
                        currency,
                        exchange,
                        paymentMethods,
                        tradingMode,
                        bannedMakers,
                        minValue,
                        workValue
                    )
                    val key = Key(currency, token, exchange)
                    ketToBuyOffers[key] = offers.first
                    keyToSellOffers[key] = offers.second
                }
            }
        }

        val currencies = if (chosenCurrency == null) paymentMethods.keys else listOf(chosenCurrency)
        for (currency in currencies) {
            for ((exchange1, exchange2) in exchanges.pairs()) {
                val tokenPairs = if (useSpot) tokens.pairs() else tokens.map { it to it }.asIterable()
                for ((token1, token2) in tokenPairs) {
                    val buyOffers = (ketToBuyOffers[Key(currency, token1, exchange1)] ?: continue).take(5)
                    val sellOffers = (keyToSellOffers[Key(currency, token2, exchange2)] ?: continue).take(5)
                    if (buyOffers.isEmpty() || sellOffers.isEmpty()) continue

                    var bestSpread = Double.NEGATIVE_INFINITY
                    var bestSpreadWithFee = Double.NEGATIVE_INFINITY

                    lateinit var bestTransferGuide: FeesService.TransferGuide

                    val spreadsWithFee = List(buyOffers.size) { MutableList(sellOffers.size) { 0.0 } }

                    for ((idx1, buyOffer) in buyOffers.withIndex()) {
                        for ((idx2, sellOffer) in sellOffers.withIndex()) {
                            val buyValue = buyOffer.available
                            val sellValue = sellOffer.available * spotService.price(token2) / spotService.price(token1)

                            val transferGuide = feesService.findTransferGuide(
                                exchange1,
                                exchange2,
                                token1,
                                token2,
                                min(buyValue, sellValue)
                            )
                            val spread = sellOffer.usdtPrice() / buyOffer.usdtPrice() - 1
                            val spreadWithFee = spread - transferGuide.calculateFinalFee()
                            spreadsWithFee[idx1][idx2] = spreadWithFee

                            if (bestSpreadWithFee < spreadWithFee) {
                                bestSpreadWithFee = spreadWithFee
                                bestSpread = spread
                                bestTransferGuide = transferGuide
                            }
                        }
                    }

                    val (
                        filteredBuyOffers,
                        filteredSellOffers,
                        filteredSpreadsWithFee
                    ) = filterSpreads(buyOffers, sellOffers, spreadsWithFee)


                    result.add(BundleSearchResult(
                        currency,
                        exchange1,
                        exchange2,
                        bestTransferGuide,
                        token1,
                        token2,
                        filteredBuyOffers,
                        filteredSellOffers,
                        filteredSpreadsWithFee,
                        bestSpread
                    ))
                }
            }
        }

        result.sortByDescending { it.bestSpread - it.transferGuide.calculateFinalFee() }
        return result
            .stream()
            .filter { it.buyOffers.isNotEmpty() && it.sellOffers.isNotEmpty() }
            .toList()
    }

    private fun filterSpreads(
        rawBuyOffers: List<Offer>,
        rawSellOffers: List<Offer>,
        rawSpreadsWithFee: List<List<Double>>
    ): Triple<List<Offer>, List<Offer>, List<List<Double>>> {
        val rowWhitelist = mutableListOf<Int>()
        val columnWhitelist = mutableListOf<Int>()

        for (idx1 in rawBuyOffers.indices) {
            if (rawSpreadsWithFee[idx1].find { it > 0.0005 } != null) {
                rowWhitelist.add(idx1)
            }
        }

        for (idx2 in rawSellOffers.indices) {
            if (rawBuyOffers.indices.map { rawSpreadsWithFee[it][idx2] }.find { it > 0.0005 } != null) {
                columnWhitelist.add(idx2)
            }
        }

        return Triple (
            rawBuyOffers.filterIndexed { index, _ -> index in rowWhitelist },
            rawSellOffers.filterIndexed { index, _ -> index in columnWhitelist },
            rawSpreadsWithFee
                .asSequence()
                .filterIndexed { index, _ -> index in rowWhitelist }
                .map { it.filterIndexed { index, _ -> index in columnWhitelist } }
                .toList()
        )
    }
}
