package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.dialogs.NotificationsDialog
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.model.SettingsMode
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
                append("⚙ Режим настроек [<b>${settings.settingsMode.readableName}</b>]\n")
                if (settings.settingsMode == SettingsMode.STANDARD) {
                    append("\uD83D\uDCB1 Биржи [<code>${settings.commonSettings.exchanges.joinToString(", ")}</code>]\n")
                    append(
                        "\uD83E\uDE99 Токены [<code>${
                            settings.commonSettings.tokens.map(Token::readableName).joinToString(", ")
                        }</code>]\n"
                    )
                }
                append(if (settings.useSpot) "\uD83D\uDFE2 Спот [<b>Включен</b>]\n" else "\uD83D\uDD34 Спот [<b>Выключен</b>]\n")
                append("${NotificationsDialog.notificationsTitle(settings.notificationsOn, settings.notificationThreshold)}\n")
                append("\uD83D\uDCB5 Минимальный объем [${settings.minimumValue} usdt]\n")
                append("\uD83D\uDCB0 Рабочий объем [${settings.workValue} usdt]\n")
                append("\uD83D\uDC65 Режим торговли [<b>${settings.tradingMode.readableName}</b>]\n")
                if (settings.settingsMode == SettingsMode.STANDARD) {
                    append("\uD83D\uDCB3 Платежки\n")
                    settings.commonSettings.paymentMethodsAsMap.map { (currency, paymentMethods) ->
                        append("<b>${currency.name}</b> [<code>${paymentMethods.joinToString(", ")}</code>]")
                    }
                }
                toString()
            },
            with(InlineKeyboardMarkup.builder()) {
                keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder().text("⚙ Режим настроек [${settings.settingsMode.readableName}]").callbackData("/settingsmode").build()
                ))
                when(settings.settingsMode) {
                    SettingsMode.STANDARD -> keyboardRow(
                        mutableListOf(
                            InlineKeyboardButton.builder().text("\uD83D\uDCB1 Биржи").callbackData("/exchanges")
                                .build(),
                            InlineKeyboardButton.builder().text("\uD83E\uDE99 Токены").callbackData("/tokens").build(),
                            InlineKeyboardButton.builder().text("\uD83D\uDCB3 Платежки").callbackData("/banks").build()
                        )
                    ).keyboardRow(
                        mutableListOf(
                            InlineKeyboardButton.builder().text("\uD83D\uDCE5 Импорт").callbackData("/import?0").build(),
                            InlineKeyboardButton.builder().text("\uD83D\uDCE4 Экспорт").callbackData("/export?0").build()
                        )
                    )
                    SettingsMode.BUY_SELL -> keyboardRow(
                        mutableListOf(
                            InlineKeyboardButton.builder()
                                .text("➡ Покупка")
                                .callbackData("/searchsettings?1").build(),
                            InlineKeyboardButton.builder()
                                .text("⬅ Продажа")
                                .callbackData("/searchsettings?2").build()
                        )
                    )
                    SettingsMode.TAKER_MAKER -> keyboardRow(
                        mutableListOf(
                            InlineKeyboardButton.builder()
                                .text("⬇️ Тейкер")
                                .callbackData("/searchsettings?3").build(),
                            InlineKeyboardButton.builder()
                                .text("⬆️ Мейкер")
                                .callbackData("/searchsettings?4").build()
                        )
                    )
                    SettingsMode.BUY_SELL_TAKER_MAKER -> {
                        keyboardRow(
                            mutableListOf(
                                InlineKeyboardButton.builder()
                                    .text("↘ Покупка-Тейкер")
                                    .callbackData("/searchsettings?5").build(),
                                InlineKeyboardButton.builder()
                                    .text("↙ Продажа-Тейкер")
                                    .callbackData("/searchsettings?6").build()
                            )
                        )
                        keyboardRow(
                            mutableListOf(
                                InlineKeyboardButton.builder()
                                    .text("↗ Покупка-Мейкер")
                                    .callbackData("/searchsettings?7").build(),
                                InlineKeyboardButton.builder()
                                    .text("↖ Продажа-Мейкер")
                                    .callbackData("/searchsettings?8").build()
                            )
                        )
                    }
                }
                keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder()
                        .text(if (settings.useSpot) "\uD83D\uDFE2 Спот [Включен]" else "\uD83D\uDD34 Спот [Выключен]")
                        .callbackData("/spot").build(),
                    InlineKeyboardButton.builder()
                        .text(settings.notificationThreshold.let {
                            "\uD83D\uDD14 Уведомления [${if (!settings.notificationsOn) "Откл." else "от $it%"}]"
                        })
                        .callbackData("/notifications").build(),
                ))
                keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder()
                        .text("\uD83D\uDCB5 Минимальный объем [${settings.minimumValue} usdt]")
                        .callbackData("/setminvalue").build(),
                    InlineKeyboardButton.builder()
                        .text("\uD83D\uDCB0 Рабочий объем [${settings.workValue} usdt]")
                        .callbackData("/setworkvalue").build(),
                ))
                keyboardRow(mutableListOf(
                    buttonsService.modeButton(settings),
                ))
                if (settings.banned.isNotEmpty()) {
                    keyboardRow(mutableListOf(buttonsService.bansButton(settings)))
                }
                keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder().text("↩️ На главную").callbackData("/start").build(),
                ))
                build()
            }
        )
    }
}
