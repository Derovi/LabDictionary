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
class MinValueDialog() : Dialog {
    @Autowired
    lateinit var bundleSearch: BundleSearch

    @Autowired
    lateinit var commandService: CommandService

    lateinit var currency: Currency
    override fun start(user: BotUser) {
        user.sendMessageWithBackButton(buildString {
            append("\uD83D\uDCB5 Минимальный объем [<b>${user.serviceUser.userSettings.minimumValue} usdt</b>]\n")
            append("Связки с меньшим объемом показываться <b>не</b> будут   \n")
            append("<i>Введите целое число</i>")
            toString()
        })
    }

    override fun update(user: BotUser): Boolean {
        val text = user.message

        val number = text.toIntOrNull()
        if (number == null) {
            user.sendMessage("\uD83D\uDCB5 <b>\"$text\"</b> не является числом!")
            commandService.back(user)
            return false
        }
        if (number < 10) {
            user.sendMessage("\uD83D\uDCB5 Минимальный объем должен должен быть не меньше <b>10 usdt</b>!")
            commandService.back(user)
            return false
        }

        user.serviceUser.userSettings.minimumValue = number
        user.sendMessage("\uD83D\uDCB5 Установлен минимальный объем [<b>$number usdt</b>]")
        commandService.back(user)
        return false
    }
}
