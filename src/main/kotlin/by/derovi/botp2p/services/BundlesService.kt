package by.derovi.botp2p.services

import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.model.SearchSettings
import by.derovi.botp2p.model.ServiceUser
import by.derovi.botp2p.model.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.transaction.Transactional
import kotlin.concurrent.thread

data class BundleKey(
    val currency: Currency,
    val buyToken: Token,
    val sellToken: Token,
    val buyExchange: Exchange,
    val sellExchange: Exchange
)

@Service
class BundlesService {
    val userToBundleSearchResult = ConcurrentHashMap<Long, List<BundleSearchResult>>()
    val userToBundleSearchResulTT = ConcurrentHashMap<Long, List<BundleSearchResult>>()

    val lastUpdateTime: Long
        get() = bundleSearch.lastFetchData

    @Autowired
    lateinit var bundleSearch: BundleSearch

    @Autowired
    lateinit var userRepository: UserRepository

    fun findBundle(userId: Long, bundleKey: BundleKey): BundleSearchResult? =
        userToBundleSearchResulTT[userId]?.find {
            it.currency == bundleKey.currency
            && it.buyToken == bundleKey.buyToken
            && it.sellToken == bundleKey.sellToken
            && it.buyExchange == bundleKey.buyExchange
            && it.sellExchange == bundleKey.sellExchange
        }

    fun makeWrapper(searchSettings: SearchSettings) = BundleSearch.SearchSettingsWrapper(
        searchSettings.tokens,
        searchSettings.exchanges.mapNotNull { name -> bundleSearch.commonExchanges.find { it.name() == name } },
        searchSettings.buyMakerBinance,
        searchSettings.paymentMethodsAsMap,
    )

    fun searchSettingsMap(user: ServiceUser) = mapOf(
        false to mapOf(
            false to makeWrapper(user.userSettings.getSearchSettings(false, false)),
            true to makeWrapper(user.userSettings.getSearchSettings(false, true))
        ),
        true to mapOf(
            false to makeWrapper(user.userSettings.getSearchSettings(true, false)),
            true to makeWrapper(user.userSettings.getSearchSettings(true, true))
        )
    )

    fun searchBestPricesForUser(
        user: ServiceUser,
        buy: Boolean,
        taker: Boolean,
        exchanges: List<Exchange>?,
        tokens: List<Token>?
    ) = bundleSearch.searchBestPrices(
        searchSettingsMap(user),
        buy,
        taker,
        user.userSettings.banned,
        Currency.RUB,
        user.userSettings.minimumValue,
        user.userSettings.workValue,
        exchanges,
        tokens
    )

    fun searchBundlesForUser(user: ServiceUser) {
        userToBundleSearchResult[user.userId] = bundleSearch.searchBundles(
            searchSettingsMap(user),
            user.userSettings.useSpot,
            user.userSettings.tradingMode,
            user.userSettings.banned,
            user.userSettings.chosenCurrency,
            user.userSettings.minimumValue,
            user.userSettings.workValue
        )
        if (user.userSettings.tradingMode == TradingMode.TAKER_TAKER) {
            userToBundleSearchResulTT[user.userId] = userToBundleSearchResult[user.userId]!!
        } else {
            userToBundleSearchResulTT[user.userId] = bundleSearch.searchBundles(
                searchSettingsMap(user),
                user.userSettings.useSpot,
                TradingMode.TAKER_TAKER,
                user.userSettings.banned,
                user.userSettings.chosenCurrency,
                user.userSettings.minimumValue,
                user.userSettings.workValue
            )
        }
    }

    val lock = AtomicBoolean(false)

    @Scheduled(initialDelay = 1000, fixedRate = 1000) // Repeat every 1 minutes
    fun fetchData() {
        if (lock.get()) return
        thread {
            lock.set(true)
            var time = System.currentTimeMillis()
            println("=== Fetching data ===")
            try {
                bundleSearch.fetchData()
            } catch (_: Exception) {} finally {
                lock.set(false)
            }
            println("Took ${(System.currentTimeMillis() - time) / 1000.0}")
        }
    }

    @Scheduled(initialDelay = 1000, fixedRate = 1000) // Repeat every 3 minutes
    @Transactional
    fun aba() {
        var time = System.currentTimeMillis()
        for (user in userRepository.findAll().filter { it.role != Role.UNSUBSCRIBED }) {
            searchBundlesForUser(user)
        }
    }
}