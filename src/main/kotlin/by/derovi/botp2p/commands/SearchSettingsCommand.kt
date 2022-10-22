package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.model.SearchSettings
import by.derovi.botp2p.model.SettingsMode
import by.derovi.botp2p.model.UserSettings
import by.derovi.botp2p.services.ButtonsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class SearchSettingsCommand : Command {
    @Autowired
    lateinit var buttonsService: ButtonsService

    override val name = "/searchsettings"
    override val role = Role.STANDARD

    companion object {
//        fun parseArgs(args: List<String>) = when (args.size) {
//            0 -> null to null
//            1 -> args[0].toBooleanStrictOrNull() to null
//            else -> args[0].toBooleanStrictOrNull() to args[1].toBooleanStrictOrNull()
//        }

//        fun getSettings(settings: UserSettings, buy: Boolean?, taker: Boolean?) = if (buy == null) {
//                settings.commonSettings
//            } else if (taker == null) {
//                if (buy) settings.buySettings else settings.sellSettings
//            } else {
//                if (buy) {
//                    if (taker) settings.buyTakerSettings else settings.buyMakerSettings
//                } else {
//                    if (taker) settings.sellTakerSettings else settings.sellMakerSettings
//                }
//            }

        fun getSettingsByArgs(settings: UserSettings, args: List<String>) =
            getSettingsByIdx(settings, args.firstOrNull()?.toIntOrNull() ?: 0)

        fun getSettingsByIdx(settings: UserSettings, idx: Int): SearchSettings =
            when (idx) {
                1 -> settings.buySettings
                2 -> settings.sellSettings
                3 -> settings.takerSettings
                4 -> settings.makerSettings
                5 -> settings.buyTakerSettings
                6 -> settings.sellTakerSettings
                7 -> settings.buyMakerSettings
                8 -> settings.sellMakerSettings
                else -> settings.commonSettings
            }

        fun getCharacteristicsByIdx(idx: Int) = when(idx) {
            1 -> true to null
            2 -> false to null
            3 -> null to true
            4 -> null to false
            5 -> true to true
            6 -> false to true
            7 -> true to false
            8 -> false to false
            else -> null to null
        }

//        fun getSettingsByArgs(settings: UserSettings, args: List<String>): SearchSettings {
//            val (buy, taker) = parseArgs(args)
//            return getSettings(
//                settings,
//                buy,
//                taker
//            )
//        }
    }

    override fun use(user: BotUser, vararg args: String) {
        val settingsIdx = args.firstOrNull()?.toIntOrNull() ?: 0
        val settings = user.serviceUser.userSettings
        val searchSettings = getSettingsByIdx(settings, settingsIdx)
        val (buy, taker) = getCharacteristicsByIdx(settingsIdx)

        user.sendMessage(
            with(StringBuilder()) {
                if (buy != null || taker != null) {
                    append("\uD83D\uDC65 Настройки для ")
                    if (buy != null) {
                        if (buy) {
                            append("<b>Покупки</b>")
                        } else {
                            append("<b>Продажи</b>")
                        }
                    }
                    if (buy != null && taker != null) {
                        append(" как ")
                    }
                    if (taker != null) {
                        if (taker) {
                            append("<b>Тейкер</b>")
                        } else {
                            append("<b>Мейкер</b>")
                        }
                    }
                }
                append("\n")

                append("\uD83D\uDCB1 Биржи [<code>${searchSettings.exchanges.joinToString(", ")}</code>]\n")
                append(
                    "\uD83E\uDE99 Токены [<code>${
                        searchSettings.tokens.map(Token::readableName).joinToString(", ")
                    }</code>]\n"
                )
                append("\uD83D\uDCB3 Карточки\n")
                if (settings.settingsMode == SettingsMode.STANDARD) {
                    searchSettings.paymentMethodsAsMap.map { (currency, paymentMethods) ->
                        append("<b>${currency.name}</b> [<code>${paymentMethods.joinToString(", ")}</code>]")
                    }
                }
                toString()
            },
            with(InlineKeyboardMarkup.builder()) {
                keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder()
                        .text("\uD83D\uDCB1 Биржи")
                        .callbackData("/exchanges?$settingsIdx").build(),
                    InlineKeyboardButton.builder()
                        .text("\uD83E\uDE99 Токены")
                        .callbackData("/tokens?$settingsIdx").build(),
                    InlineKeyboardButton.builder()
                        .text("\uD83D\uDCB3 Карточки")
                        .callbackData("/banks?$settingsIdx").build()
                )).keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder().text("\uD83D\uDCE5 Импорт").callbackData("/import?$settingsIdx").build(),
                    InlineKeyboardButton.builder().text("\uD83D\uDCE4 Экспорт").callbackData("/export?$settingsIdx").build()
                ))

                keyboardRow(mutableListOf(buttonsService.backButton()))
                build()
            }
        )
    }
}
