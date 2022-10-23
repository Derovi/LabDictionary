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
        fun notificationsTitle(notificationsOn: Boolean, threshold: Double, text: String = "Уведомления") =
            "\uD83D\uDD14 $text [<b>${if (!notificationsOn) "Отключены" else "от $threshold%"}</b>]"
    }

    @Autowired
    lateinit var commandService: CommandService
    override fun start(user: BotUser, args: List<String>) {
        user.sendMessage(
            buildString {
                append("${notificationsTitle(
                    user.serviceUser.userSettings.notificationsOn,
                    user.serviceUser.userSettings.notificationThreshold)}\n")
                append("Вы будете получать уведомления о <b>Тейкер-Тейкер</b> связках со спредом <b>не меньше</b> указанного\n")
                append("<i>Введите число</i>")
                toString()
            },
            with(InlineKeyboardMarkup.builder()) {
                keyboardRow(listOf(
                    if (user.serviceUser.userSettings.notificationsOn) {
                        InlineKeyboardButton
                            .builder()
                            .text("\uD83D\uDEAB Отключить")
                            .callbackData("off")
                            .build()
                    } else {
                        InlineKeyboardButton
                            .builder()
                            .text("\uD83D\uDFE2 Включить")
                            .callbackData("on")
                            .build()
                    }
                ))
                keyboardRow(listOf(buttonsService.backButton()))
                build()
            }
        )
    }

    override fun update(user: BotUser): Boolean {
        val text = user.message.replace(",", ".")
        if (text == "off") {
            user.serviceUser.userSettings.notificationsOn = false
            user.sendMessage("\uD83D\uDD14 Уведомления отключены")
            commandService.back(user)
            return false
        } else if (text == "on") {
            user.serviceUser.userSettings.notificationsOn = true
            user.sendMessage("\uD83D\uDD14 Уведомления включены")
            commandService.back(user)
            return false
        }
        val percent = text.toDoubleOrNull()
        if (percent == null) {
            user.sendMessage("\uD83D\uDD14 <b>\"$text\"</b> не является числом!")
            commandService.back(user)
            return false
        }
        if (percent < 0.5) {
            user.sendMessage("\uD83D\uDD14 Спред должен быть не меньше <b>0.5%</b>")
            commandService.back(user)
            return false
        }

        user.serviceUser.userSettings.notificationThreshold = percent
        user.sendMessage(
            notificationsTitle(
                user.serviceUser.userSettings.notificationsOn,
                user.serviceUser.userSettings.notificationThreshold, text = "Установлены уведомления")
        )
        commandService.back(user)
        return false
    }
}