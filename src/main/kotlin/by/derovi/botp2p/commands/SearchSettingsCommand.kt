package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.commands.SearchSettingsCommand.Companion.getSettings
import by.derovi.botp2p.commands.SearchSettingsCommand.Companion.parseArgs
import by.derovi.botp2p.dialogs.NotificationsDialog
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.model.SearchSettings
import by.derovi.botp2p.model.SettingsMode
import by.derovi.botp2p.model.UserSettings
import by.derovi.botp2p.services.ButtonsService
import org.apache.xpath.operations.Bool
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
        fun parseArgs(args: List<String>) = when (args.size) {
            0 -> null to null
            1 -> args[0].toBooleanStrictOrNull() to null
            else -> args[0].toBooleanStrictOrNull() to args[1].toBooleanStrictOrNull()
        }

        fun getSettings(settings: UserSettings, buy: Boolean?, taker: Boolean?) = if (buy == null) {
                settings.commonSettings
            } else if (taker == null) {
                if (buy) settings.buySettings else settings.sellSettings
            } else {
                if (buy) {
                    if (taker) settings.buyTakerSettings else settings.buyMakerSettings
                } else {
                    if (taker) settings.sellTakerSettings else settings.sellMakerSettings
                }
            }

        fun getSettingsByArgs(settings: UserSettings, args: List<String>): SearchSettings {
            val (buy, taker) = parseArgs(args)
            return getSettings(
                settings,
                buy,
                taker
            )
        }
    }

    override fun use(user: BotUser, vararg args: String) {
        val (buy, taker) = parseArgs(args.toList())
        val settings = user.serviceUser.userSettings
        val searchSettings = getSettings(settings, buy, taker)

        user.sendMessage(
            with(StringBuilder()) {
                if (buy != null) {
                    append("\uD83D\uDC65 Настройки для ")
                    if (buy) {
                        append("<b>Покупки</b>")
                    } else {
                        append("<b>Продажи</b>")
                    }
                    if (taker != null) {
                        append(" как ")
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
                        .callbackData("/exchanges?$buy&$taker").build(),
                    InlineKeyboardButton.builder()
                        .text("\uD83E\uDE99 Токены")
                        .callbackData("/tokens?$buy&$taker").build(),
                    InlineKeyboardButton.builder()
                        .text("\uD83D\uDCB3 Карточки")
                        .callbackData("/banks?$buy&$taker").build()
                ))

                keyboardRow(mutableListOf(buttonsService.backButton()))
                build()
            }
        )
    }
}
