package by.derovi.botp2p.dialogs

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.commands.SearchSettingsCommand
import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.model.CurrencyAndPaymentMethod
import by.derovi.botp2p.model.SearchSettings
import by.derovi.botp2p.model.SearchSettingsRepository
import by.derovi.botp2p.services.ButtonsService
import by.derovi.botp2p.services.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
@Scope("prototype")
class BanksDialog(var state: State = State.CURRENCY) : Dialog {
    @Autowired
    lateinit var bundleSearch: BundleSearch

    @Autowired
    lateinit var buttonsService: ButtonsService

    @Autowired
    lateinit var searchSettingsRepository: SearchSettingsRepository

    @Autowired
    lateinit var commandService: CommandService

    enum class State {
        CURRENCY, CARDS
    }

    lateinit var currency: Currency
    lateinit var args: List<String>

    override fun start(user: BotUser, args: List<String>) {
        this.args = args

        user.sendMessage(
            buildString {
                append("\uD83D\uDCB3️ Карточки\n")
                append("Выберите валюту")
                toString()
            },
            with(InlineKeyboardMarkup.builder()) {
                keyboardRow(Currency.values().map(Currency::name).map {
                    InlineKeyboardButton.builder().text(it).callbackData(it).build()
                })
                keyboardRow(mutableListOf(buttonsService.backButton()))
                build()
            }
        )
    }

    override fun update(user: BotUser): Boolean {
        val searchSettings = SearchSettingsCommand.getSettingsByArgs(user.serviceUser.userSettings, args)

        if (state == State.CURRENCY) {
            val currency = Currency.values().find { it.name.equals(user.message, ignoreCase = true) }
            if (currency == null) {
                user.sendMessage("\uD83D\uDCB3️ Валюта <b>\"${user.message}\"</b> не найдена!")
                return false
            }

            user.sendMessageWithBackButton(
                with(StringBuilder()) {
                    append("\uD83D\uDCB3 Карточки <b>${currency.name}</b>\n")
                    val currentPaymentMethods = searchSettings.paymentMethodsAsMap[currency]?.sortedBy { it.name }
                    val availablePaymentMethods = PaymentMethod.values().sortedBy { it.name }
                    if (currentPaymentMethods != null && currentPaymentMethods.isNotEmpty()) {
                        append("Установлены: <code>${
                            currentPaymentMethods.map(PaymentMethod::name)
                                .joinToString(", ")
                        }</code>\n")
                    }
                    append("Доступны: <code>${availablePaymentMethods.map(PaymentMethod::name)
                        .joinToString(", ")}</code>\n")
                    append("<i>Введите банки через запятую</i>")
                    toString()
                }
            )
            this.currency = currency
            if (currency != Currency.RUB) {
                user.sendMessageWithBackButton(
                    "Поддержка <b>$currency</b> скоро!"
                )
                return false
            }
            state = State.CARDS
            return true
        } else {
            val inputPaymentMethods = user.message.split(Regex("[^a-zA-Z0-9_]+"))
            val newPaymentMethods = mutableListOf<PaymentMethod>()
            for (paymentMethodName in inputPaymentMethods) {
                val paymentMethod = PaymentMethod.values().find { it.name.equals(paymentMethodName, ignoreCase = true) }
                if (paymentMethod == null) {
                    user.sendMessage("\uD83D\uDCB3 Банка ${if (paymentMethodName.isNotEmpty()) "<b>\"$paymentMethodName\"</b>" else ""} не существует!")
                    commandService.back(user)
                    return false
                }
                newPaymentMethods.add(paymentMethod)
            }
            user.sendMessage("\uD83D\uDCB3 Для <b>$currency</b> установлены" +
                    " [<code>${newPaymentMethods.distinct().joinToString(", ")}</code>]")
            searchSettings.paymentMethods.removeIf { it.currency == currency }
            searchSettings.paymentMethods
                .addAll(newPaymentMethods.stream().distinct().map { CurrencyAndPaymentMethod(currency, it) }.toList())
            searchSettingsRepository.save(searchSettings)
            commandService.back(user)
            return false
        }
    }
}
