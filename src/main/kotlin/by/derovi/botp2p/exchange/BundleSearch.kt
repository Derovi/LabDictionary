package by.derovi.botp2p.exchange

import by.derovi.botp2p.exchange.exchanges.Binance
import by.derovi.botp2p.library.PoolWithRetries
import by.derovi.botp2p.model.Maker
import by.derovi.botp2p.pairs
import by.derovi.botp2p.services.FeesService
import by.derovi.botp2p.services.SpotService
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

const val OFFERS_LIMIT_FOR_BUNDLE = 5

class BundleSearch(val exchanges: Array<Exchange>) {
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
        val pool = PoolWithRetries(20)
        exchanges.asSequence().map(Exchange::getFetchTasks).flatten().map {{
            it().forEach { (setup, offers) ->
                newSetupToOffers.getOrPut(setup) { mutableListOf() }
                    .addAll(offers)
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

    fun offersWithRestrictions(
        token: Token,
        currency: Currency,
        exchange: Exchange,
        paymentMethods: Map<Currency, List<PaymentMethod>>,
        tradingMode: TradingMode,
        bannedMakers: List<Maker>
    ): Pair<List<Offer>, List<Offer>> {
        val buyOffers = mutableListOf<Offer>()
        val sellOffers = mutableListOf<Offer>()

        val currentPaymentMethods = paymentMethods.getOrDefault(currency, listOf())
        for (paymentMethod in currentPaymentMethods) {
            setupToOffers[Setup(token, currency, exchange, paymentMethod, OrderType.BUY)]?.let {
                buyOffers.addAll(it)
                if (tradingMode != TradingMode.TAKER_TAKER && it.isNotEmpty()) {
                    sellOffers.add(it.first())
                }
            }
            setupToOffers[Setup(token, currency, exchange, paymentMethod, OrderType.SELL)]?.let {
                if (tradingMode == TradingMode.MAKER_MAKER_BINANCE_MERCHANT
                    || tradingMode == TradingMode.MAKER_MAKER_NO_BINANCE && exchange != Binance) {
                    if (it.isNotEmpty()) {
                        buyOffers.add(it.first())
                    }
                }
                sellOffers.addAll(it)
            }
        }

        buyOffers.sortBy { it.price }
        sellOffers.sortByDescending { it.price }

        fun List<Offer>.filterBanned() = this.filter { !bannedMakers.contains(Maker(it.username, it.exchange!!.name())) }
        return buyOffers.filterBanned() to sellOffers.filterBanned()
    }

    fun searchBundles(
        tokens: List<Token>,
        exchanges: List<Exchange>,
        paymentMethods: Map<Currency, List<PaymentMethod>>,
        useSpot: Boolean,
        tradingMode: TradingMode,
        bannedMakers: List<Maker>,
        chosenCurrency: Currency?
    ): List<BundleSearchResult> {
        val result = mutableListOf<BundleSearchResult>()

        data class Key(val currency: Currency, val token: Token, val exchange: Exchange)
        val ketToBuyOffers = mutableMapOf<Key, List<Offer>>()
        val keyToSellOffers = mutableMapOf<Key, List<Offer>>()

        for (currency in paymentMethods.keys) {
            for (exchange in exchanges) {
                for (token in tokens) {
                    val offers = offersWithRestrictions(
                        token,
                        currency,
                        exchange,
                        paymentMethods,
                        tradingMode,
                        bannedMakers
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

                    fun Offer.usdtPrice() = this.price / spotService.price(this.token)

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
