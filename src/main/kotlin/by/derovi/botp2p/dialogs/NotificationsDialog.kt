package by.derovi.botp2p.dialogs

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.exchange.BundleSearch
import by.derovi.botp2p.exchange.Exchange
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.services.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope("prototype")
class NotificationsDialog : Dialog {
    @Autowired
    lateinit var bundleSearch: BundleSearch

    @Autowired
    lateinit var commandService: CommandService
    override fun start(user: BotUser) {
        user.sendMessageWithBackButton(buildString {
            append("⚠️ Уведомления [<b>от ${user.serviceUser.userSettings.notificationThreshold}%</b>]\n")
            append("Вы будете получать уведомления о <b>Тейкер-Тейкер</b> связках со спредом <b>не меньше</b> указанного\n")
            append("<i>Введите число</i>")
            toString()
        })
    }

    override fun update(user: BotUser): Boolean {
        val text = user.message.replace(",", ".")
        val percent = text.toDoubleOrNull()
        if (percent == null) {
            user.sendMessage("⚠️ <b>\"$text\"</b> не является числом!")
            commandService.back(user)
            return false
        }
        if (percent < 0.5) {
            user.sendMessage("⚠️ Спред должен быть не меньше <b>0.5%</b>")
            commandService.back(user)
            return false
        }

        user.serviceUser.userSettings.notificationThreshold = percent
        user.sendMessage("⚠️ Установлены уведомления [<b>от ${user.serviceUser.userSettings.notificationThreshold}%</b>]")
        commandService.back(user)
        return false
    }
}