package by.derovi.botp2p.services

import by.derovi.botp2p.exchange.BundleSearch
import by.derovi.botp2p.exchange.BundleSearchResult
import by.derovi.botp2p.exchange.OrderType
import by.derovi.botp2p.exchange.PaymentMethod
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.model.ServiceUser
import by.derovi.botp2p.model.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import javax.transaction.Transactional
import kotlin.concurrent.thread

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

    fun searchBundlesForUser(user: ServiceUser) {
        val result = bundleSearch.searchBundles(
            user.userSettings.tokens,
            user.userSettings.exchanges.mapNotNull { name -> bundleSearch.exchanges.find { it.name() == name } },
            user.userSettings.paymentMethodsAsMap,
            user.userSettings.useSpot,
            user.userSettings.tradingMode,
            user.userSettings.banned,
            user.userSettings.chosenCurrency
        )
        userToBundleSearchResult[user.userId] = result
        userToBundleSearchResulTT[user.userId] = result.filter {
            it.buyOffers.first().orderType == OrderType.BUY && it.sellOffers.first().orderType == OrderType.SELL
        }
    }

    @Scheduled(initialDelay = 1000, fixedRate = 1 * 60 * 1000) // Repeat every 1 minutes
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