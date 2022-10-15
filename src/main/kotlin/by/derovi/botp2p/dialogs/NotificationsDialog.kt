package by.derovi.botp2p.dialogs

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.exchange.BundleSearch
import by.derovi.botp2p.exchange.Exchange
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.services.ButtonsService
import by.derovi.botp2p.services.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
@Scope("prototype")
class NotificationsDialog : Dialog {
    @Autowired
    lateinit var bundleSearch: BundleSearch

    @Autowired
    lateinit var buttonsService: ButtonsService

    companion object {
        fun notificationsTitle(threshold: Double?, text: String = "Уведомления") =
            "⚠️ $text [<b>${if (threshold == null) "Отключены" else "от $threshold%"}</b>]"
    }

    @Autowired
    lateinit var commandService: CommandService
    override fun start(user: BotUser) {
        user.sendMessage(
            buildString {
                append("${notificationsTitle(user.serviceUser.userSettings.notificationThreshold)}\n")
                append("Вы будете получать уведомления о <b>Тейкер-Тейкер</b> связках со спредом <b>не меньше</b> указанного\n")
                append("<i>Введите число</i>")
                toString()
            },
            with(InlineKeyboardMarkup.builder()) {
                keyboardRow(listOf(
                    InlineKeyboardButton
                        .builder()
                        .text("\uD83D\uDEAB Отключить")
                        .callbackData("off")
                        .build()
                ))
                keyboardRow(listOf(buttonsService.backButton()))
                build()
            }
        )
    }

    override fun update(user: BotUser): Boolean {
        val text = user.message.replace(",", ".")
        if (text == "off") {
            user.serviceUser.userSettings.notificationThreshold = null
            user.sendMessage("⚠️ Уведомления отключены")
            commandService.back(user)
            return false
        }
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
        user.sendMessage(
            notificationsTitle(user.serviceUser.userSettings.notificationThreshold, text = "Установлены уведомления")
        )
        commandService.back(user)
        return false
    }
}