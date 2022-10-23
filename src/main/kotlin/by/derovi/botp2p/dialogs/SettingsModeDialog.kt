package by.derovi.botp2p.dialogs

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.model.CurrencyAndPaymentMethod
import by.derovi.botp2p.model.SettingsMode
import by.derovi.botp2p.services.ButtonsService
import by.derovi.botp2p.services.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
@Scope("prototype")
class SettingsModeDialog : Dialog {
    @Autowired
    lateinit var bundleSearch: BundleSearch

    @Autowired
    lateinit var commandService: CommandService

    @Autowired
    lateinit var buttonsService: ButtonsService

    lateinit var currency: Currency
    override fun start(user: BotUser, args: List<String>) {
        fun buttonForMode(mode: SettingsMode) =
            InlineKeyboardButton
                .builder()
                .text(mode.readableName + if (user.serviceUser.userSettings.settingsMode == mode) " ✓" else "")
                .callbackData(mode.name)
                .build()

        user.sendMessage(
            with(StringBuilder()) {
                append("⚙ Изменить режим настроек\n")
                SettingsMode.values().forEach {
                    if (user.serviceUser.userSettings.settingsMode == it) {
                        append("<b>✓ </b>")
                    }
                    append("<b>${it.readableName}</b> - ${it.description}\n")
                }
                toString()
            },
            with(InlineKeyboardMarkup.builder()) {
                keyboardRow(mutableListOf(buttonForMode(SettingsMode.STANDARD)))
                keyboardRow(mutableListOf(buttonForMode(SettingsMode.BUY_SELL)))
                keyboardRow(mutableListOf(buttonForMode(SettingsMode.TAKER_MAKER)))
                keyboardRow(mutableListOf(buttonForMode(SettingsMode.BUY_SELL_TAKER_MAKER)))
                keyboardRow(mutableListOf(buttonsService.backButton()))
                build()
            }
        )
    }

    override fun update(user: BotUser): Boolean {
        val mode = SettingsMode.values().find { it.name.equals(user.message, ignoreCase = true) }
        if (mode == null) {
            user.sendMessage("<b>Режим</b> <i>${user.message}<i> <b>не найден!</b>")
            commandService.back(user)
        } else {
            user.serviceUser.userSettings.settingsMode = mode
            user.searchBundles()
            commandService.back(user)
        }
        return false
    }
}
