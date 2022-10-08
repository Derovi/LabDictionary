package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
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
                append("/exchanges [<b>${settings.exchanges.joinToString(", ")}</b>] - Указать биржи\n")
                append("/notifications [<b>${settings.notificationThreshold}%</b>] - Параметры уведомлений\n")
                append("/spot [<b>${if (settings.useSpot) "да" else "нет"}</b>] " +
                        "- ${if (settings.useSpot) "Выключить" else "Включить"} спот в связках\n")
                append("/tokens [<b>${settings.tokens.map(Token::readableName).joinToString(", ")}</b>] - Указать токены\n")
                append("/banks [${settings.paymentMethodsAsMap.map { (currency, paymentMethod) -> "<b>${currency}</b>:" +
                        " <i>${paymentMethod.joinToString(", ") }</i>" }.joinToString("; ") }] " +
                        "- Указать банковские карточки\n")
                append("/mode [<b>${settings.tradingMode.readableName}]</b> - Выбрать режим торговли\n")
                append("/guide <b>- Подробнее о настройках</b>\n")
                toString()
            },
            InlineKeyboardMarkup.builder()
                .keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder().text("Биржи").callbackData("/exchanges").build(),
                    InlineKeyboardButton.builder().text("Токены").callbackData("/tokens").build(),
                    InlineKeyboardButton.builder().text("Карточки").callbackData("/banks").build()
                )).keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder()
                        .text("${if (settings.useSpot) "Выключить" else "Включить"} спот")
                        .callbackData("/spot").build(),
                    InlineKeyboardButton.builder()
                        .text("Уведомления [от ${settings.notificationThreshold}]%")
                        .callbackData("/notifications").build(),
                )).keyboardRow(mutableListOf(
                    buttonsService.modeButton(settings),
                )).let {
                    if (settings.banned.isNotEmpty()) {
                        it.keyboardRow(mutableListOf(buttonsService.bansButton(settings)))
                    }
                    it
                }
                .keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder().text("На главную").callbackData("/start").build(),
                )).build()
        )
    }
}
