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
        taker: Boolean
    ) = bundleSearch.searchBestPrices(
        searchSettingsMap(user),
        buy,
        taker,
        user.userSettings.banned,
        Currency.RUB,
        user.userSettings.minimumValue,
        user.userSettings.workValue
    )

    fun searchBundlesForUser(user: ServiceUser) {
        val result = bundleSearch.searchBundles(
            searchSettingsMap(user),
            user.userSettings.useSpot,
            user.userSettings.tradingMode,
            user.userSettings.banned,
            user.userSettings.chosenCurrency,
            user.userSettings.minimumValue,
            user.userSettings.workValue
        )
        userToBundleSearchResult[user.userId] = result
        userToBundleSearchResulTT[user.userId] = result.filter {
            it.buyOffers.first().orderType == OrderType.BUY && it.sellOffers.first().orderType == OrderType.SELL
        }
    }

    @Scheduled(initialDelay = 1000, fixedRate = 20 * 1000) // Repeat every 1 minutes
    fun scheduleTaskUsingCronExpression() {
        thread {
            PaymentMethod.values().toList() // Список банков. Указаны все, можно указать конкретные вот так:
            var time = System.currentTimeMillis()
            println("=== Fetching data ===")
            bundleSearch.fetchData()
            println("Took ${(System.currentTimeMillis() - time) / 1000.0}")
        }
    }

    @Scheduled(initialDelay = 1000, fixedRate = 5000) // Repeat every 3 minutes
    @Transactional
    fun aba() {
        var time = System.currentTimeMillis()
        for (user in userRepository.findAll().filter { it.role != Role.UNSUBSCRIBED }) {
            searchBundlesForUser(user)
        }
    }
}