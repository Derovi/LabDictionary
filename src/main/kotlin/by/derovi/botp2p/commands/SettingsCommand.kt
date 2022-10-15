package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.dialogs.NotificationsDialog
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.ButtonsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class SettingsCommand : Command {
    @Autowired
    lateinit var buttonsService: ButtonsService

    override val name = "/settings"
    override val role = Role.STANDARD

    override fun use(user: BotUser, vararg args: String) {
        val settings = user.serviceUser.userSettings

        user.sendMessage(
            with(StringBuilder()) {
                append("\uD83D\uDCB1 Биржи [<code>${settings.exchanges.joinToString(", ")}</code>]\n")
                append("\uD83E\uDE99 Токены [<code>${settings.tokens.map(Token::readableName).joinToString(", ")}</code>]\n")
                append(if (settings.useSpot) "\uD83D\uDFE2 Спот [<b>Включен</b>]\n" else "\uD83D\uDD34 Спот [<b>Выключен</b>]\n")
                append("${NotificationsDialog.notificationsTitle(settings.notificationThreshold)}\n")
                append("\uD83D\uDCB5 Минимальный объем [${settings.minimumValue} usdt]\n")
                append("\uD83D\uDCB0 Рабочий объем [${settings.workValue} usdt]\n")
                append("\uD83D\uDC65 Режим торговли [<b>${settings.tradingMode.readableName}]</b>\n")
                append("\uD83D\uDCB3 Карточки\n")
                settings.paymentMethodsAsMap.map { (currency, paymentMethods) ->
                    append("<b>${currency.name}</b> [<code>${paymentMethods.joinToString(", ")}</code>]")
                }
                toString()
            },
            InlineKeyboardMarkup.builder()
                .keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder().text("\uD83D\uDCB1 Биржи").callbackData("/exchanges").build(),
                    InlineKeyboardButton.builder().text("\uD83E\uDE99 Токены").callbackData("/tokens").build(),
                    InlineKeyboardButton.builder().text("\uD83D\uDCB3 Карточки").callbackData("/banks").build()
                )).keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder()
                        .text(if (settings.useSpot) "\uD83D\uDFE2 Спот [Включен]" else "\uD83D\uDD34 Спот [Выключен]")
                        .callbackData("/spot").build(),
                    InlineKeyboardButton.builder()
                        .text(settings.notificationThreshold.let {
                            "⚠️ Уведомления [${if (it == null) "Откл." else "от $it%"}]"
                        })
                        .callbackData("/notifications").build(),
                )).keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder()
                        .text("\uD83D\uDCB5 Минимальный объем [${settings.minimumValue} usdt]")
                        .callbackData("/setminvalue").build(),
                    InlineKeyboardButton.builder()
                        .text("\uD83D\uDCB0 Рабочий объем [${settings.workValue} usdt]")
                        .callbackData("/setworkvalue").build(),
                )).keyboardRow(mutableListOf(
                    buttonsService.modeButton(settings),
                )).let {
                    if (settings.banned.isNotEmpty()) {
                        it.keyboardRow(mutableListOf(buttonsService.bansButton(settings)))
                    }
                    it
                }
                .keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder().text("↩️ На главную").callbackData("/start").build(),
                )).build()
        )
    }
}
