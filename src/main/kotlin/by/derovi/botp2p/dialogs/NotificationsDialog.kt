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
    override fun start(user: BotUser) {}

    override fun update(user: BotUser): Boolean {
        val text = user.message.replace(",", ".")
        if (text.matches(Regex("[^0-9.]"))) {
            user.sendMessage("<b>\"$text\" не является числом!</b>")
            commandService.back(user)
            return false
        }

        val percent = text.toDoubleOrNull()
        if (percent == null || percent < 0.1 || percent > 100) {
            user.sendMessage("<b>Спред должен быть не меньше 0.1% и не больше 100%</b>")
            commandService.back(user)
            return false
        }

        user.serviceUser.userSettings.notificationThreshold = percent
        user.sendMessage("Установлен спред: <b>$percent%</b>")
        commandService.back(user)
        return false
    }
}