package by.derovi.botp2p.services

import by.derovi.botp2p.Bot
import by.derovi.botp2p.BotUser
import by.derovi.botp2p.commands.SpotCommand
import by.derovi.botp2p.exchange.TradingMode
import by.derovi.botp2p.exchange.Currency
import by.derovi.botp2p.exchange.PaymentMethod
import by.derovi.botp2p.exchange.Token
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
    lateinit var context: ApplicationContext

    @Autowired
    lateinit var langService: LangService

    fun defaultSettings() = UserSettings(
        0.15,
        false,
        TradingMode.TAKER_TAKER,
        mutableListOf(Token.USDT),
        mutableListOf(Huobi.name(), Binance.name()),
        mutableListOf(
            CurrencyAndPaymentMethod(Currency.RUB, PaymentMethod.TINKOFF),
            CurrencyAndPaymentMethod(Currency.RUB, PaymentMethod.SBERBANK),
            CurrencyAndPaymentMethod(Currency.RUB, PaymentMethod.CITIBANK)
        ),
        null,
        50,
        5000,
        mutableListOf(),
        null,
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

    fun getBotUserById(id: Long): BotUser {
        val serviceUser = userRepository.findById(id).get()
        return BotUser(
            serviceUser,
            "",
            null,
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
