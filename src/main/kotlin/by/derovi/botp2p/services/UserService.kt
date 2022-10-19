package by.derovi.botp2p.services

import by.derovi.botp2p.Bot
import by.derovi.botp2p.BotUser
import by.derovi.botp2p.commands.SpotCommand
import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.exchange.exchanges.Binance
import by.derovi.botp2p.exchange.exchanges.Huobi
import by.derovi.botp2p.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

@Service
class UserService {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var searchSettingsRepository: SearchSettingsRepository

    @Autowired
    lateinit var context: ApplicationContext

    @Autowired
    lateinit var langService: LangService

    val userIdToLastAction = mutableMapOf<Long, Long>()

    fun createDefaultSearchSettings(): SearchSettings {
        val searchSettings = SearchSettings(
            0,
            Token.values().toMutableList(),
            context.getBean(BundleSearch::class.java).commonExchanges.map(Exchange::name).toMutableList(),
            mutableListOf(
                CurrencyAndPaymentMethod(Currency.RUB, PaymentMethod.TINKOFF),
                CurrencyAndPaymentMethod(Currency.RUB, PaymentMethod.CITIBANK),
                CurrencyAndPaymentMethod(Currency.RUB, PaymentMethod.ROSSELHOZBANK),
                CurrencyAndPaymentMethod(Currency.RUB, PaymentMethod.POSTBANK),
                CurrencyAndPaymentMethod(Currency.RUB, PaymentMethod.RAIFAIZEN),
                CurrencyAndPaymentMethod(Currency.RUB, PaymentMethod.URALSIB),
                CurrencyAndPaymentMethod(Currency.RUB, PaymentMethod.ROSBANK),
                CurrencyAndPaymentMethod(Currency.RUB, PaymentMethod.GAZPROMBANK),
                CurrencyAndPaymentMethod(Currency.RUB, PaymentMethod.ALFA_BANK),
                CurrencyAndPaymentMethod(Currency.RUB, PaymentMethod.SBERBANK),
                CurrencyAndPaymentMethod(Currency.RUB, PaymentMethod.CITIBANK)
            )
        )
        searchSettingsRepository.save(searchSettings)
        return searchSettings
    }

    fun defaultSettings() = UserSettings(
        1.0,
        true,
        TradingMode.TAKER_TAKER,
        null,
        50,
        5000,
        mutableListOf(),
        null,
        SettingsMode.STANDARD,
        createDefaultSearchSettings(),
        createDefaultSearchSettings(),
        createDefaultSearchSettings(),
        createDefaultSearchSettings(),
        createDefaultSearchSettings(),
        createDefaultSearchSettings(),
        createDefaultSearchSettings(),
    )

    fun syncUser(userId: Long, login: String?, chatId: Long): ServiceUser = userRepository.findById(userId).orElseGet {
        ServiceUser(
            userId,
            defaultSettings(),
            Role.UNSUBSCRIBED,
            0,
            0,
            false,
            login,
            chatId,
            0,
            null,
            null
        ).also {
            userRepository.save(it)
        }
    }

    fun getBotUserById(id: Long, callbackMessageId: Int? = null): BotUser {
        val serviceUser = userRepository.findById(id).get()
        return BotUser(
            serviceUser,
            "",
            null,
            callbackMessageId,
            false,
            langService.getLangMap(serviceUser.userSettings.lang, "ru"),
            context
        )
    }
}

// 7 30 90 365
// Subscription
// Role
// duration - days
// startDate

//TimeUnit:
//30
//60
//90
//365
//
//PriceMap:
//(Role to TimeUnit) to Int
//
//defaultPriceMap
//
//Promocode:
//priceMap: PriceMap
//refer: Long? - userId
//expirationDate: Long? - millis
//
//PromocodeService:
//
//loadDefaultPromocodes() {
//
//}
//
//filterExpiredPromocodes() {
//
//}
//
//createPromocode(priceMap, refer) {
//
//}
//
//Ваш тариф: STANDART
//Осталось дней: 14
//
//Бонус от партрнеров: 0$
//Ваша скидка: 10%
//Ваш реферальный промокод:
//#38473847348
//Каждый, кто укажет этот промокод, получит скидку 10%, а вы получите 10% от его оплаты в качестве бонуса!
//
//<Продлить>
//<Изменить тариф>
//<У меня есть промокод>
//
//STANDARD:
//7  дней - 20$
//30 дней - 40$
//90 дней - 80$
//
//К оплате: 20$
//Кошелек для оплаты:
//#usdt
//Квитанция: #3l4nnn49
//После перевода, напишите @botOplata текст "Оплатил #3l4nnn49"
//
//Квитанция: #3l4nnn4
//Пользователь: #id (@derovi)
//STANDARD, 90 days
//Кошелек: $usdt
//Сумма: 20$
//
//<Подтвердить>
//<Отменить>
//<Страница пользователя>
//<Назад>
//
