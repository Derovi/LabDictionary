package by.derovi.botp2p.dialogs

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.model.CurrencyAndPaymentMethod
import by.derovi.botp2p.services.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope("prototype")
class BanksDialog(var state: State = State.CURRENCY) : Dialog {
    @Autowired
    lateinit var bundleSearch: BundleSearch

    @Autowired
    lateinit var commandService: CommandService

    enum class State {
        CURRENCY, CARDS
    }

    lateinit var currency: Currency
    override fun start(user: BotUser) {}

    override fun update(user: BotUser): Boolean {
        fun addBanksInfo(stringBuilder: StringBuilder, currentPaymentMethods: List<PaymentMethod>?) {
            stringBuilder.append("Доступны: <i>${PaymentMethod.values().map(PaymentMethod::name)
                .joinToString(", ")}</i>\n")
            stringBuilder.append(
                if (currentPaymentMethods == null || currentPaymentMethods.isEmpty())
                    "<b>Установленных нет</b>\n"
                else
                    "Установлены: <i>${currentPaymentMethods.map(PaymentMethod::name)
                        .joinToString(", ")}</i>\n"
            )
            stringBuilder.append("<i>Банки нужно перечислять через запятую</i>\nПример: <b>TINKOFF, SBERBANK</b>")
        }

        if (state == State.CURRENCY) {
            val currency = Currency.values().find { it.name.equals(user.message, ignoreCase = true) }
            if (currency == null) {
                user.sendMessage("<b>Валюта \"${user.message}\" не найдена!</b>")
                return false
            }

            user.sendMessageWithBackButton(
                with(StringBuilder()) {
                    append("<b>Укажите один или несколько банков</b>\n")
                    addBanksInfo(this, user.serviceUser.userSettings.paymentMethodsAsMap[currency])
                    toString()
                }
            )
            this.currency = currency
            state = State.CARDS
            return true
        } else {
            val inputPaymentMethods = user.message.split(Regex("[^a-zA-Z0-9_]+"))
            val newPaymentMethods = mutableListOf<PaymentMethod>()
            for (paymentMethodName in inputPaymentMethods) {
                val paymentMethod = PaymentMethod.values().find { it.name.equals(paymentMethodName, ignoreCase = true) }
                if (paymentMethod == null) {
                    user.sendMessage(
                        with(StringBuilder()) {
                            append("<b>Банка \"$paymentMethodName\" не существует!</b>\n")
                            addBanksInfo(this, user.serviceUser.userSettings.paymentMethodsAsMap[currency])
                            toString()
                        }
                    )
                    return false
                }
                newPaymentMethods.add(paymentMethod)
            }
            user.sendMessage("Для <b>$currency</b> установлены: <b>${newPaymentMethods.distinct().joinToString(", ")}</b>")
            user.serviceUser.userSettings.paymentMethods.removeIf { it.currency == currency }
            user.serviceUser.userSettings.paymentMethods
                .addAll(newPaymentMethods.stream().distinct().map { CurrencyAndPaymentMethod(currency, it) }.toList())
            commandService.back(user)
            return false
        }
    }
}
