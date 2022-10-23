package by.derovi.botp2p.dialogs

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.model.CurrencyAndPaymentMethod
import by.derovi.botp2p.services.ButtonsService
import by.derovi.botp2p.services.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
@Scope("prototype")
class ModeDialog : Dialog {
    @Autowired
    lateinit var bundleSearch: BundleSearch

    @Autowired
    lateinit var buttonsService: ButtonsService

    @Autowired
    lateinit var commandService: CommandService

    lateinit var currency: Currency
    override fun start(user: BotUser, args: List<String>) {
        fun buttonForMode(mode: TradingMode) =
            InlineKeyboardButton
                .builder()
                .text(mode.readableName + if (user.serviceUser.userSettings.tradingMode == mode) " ✓" else "")
                .callbackData(mode.name)
                .build()

        user.sendMessage(
            with(StringBuilder()) {
                append("\uD83D\uDC65 Изменить режим торговли\n")
                TradingMode.values().forEach {
                    if (user.serviceUser.userSettings.tradingMode == it) {
                        append("<b>✓ </b>")
                    }
                    append("<b>${it.readableName}</b> - ${it.description}\n")
                }
                toString()
            },
            InlineKeyboardMarkup.builder()
                .keyboardRow(mutableListOf(
                    buttonForMode(TradingMode.TAKER_TAKER),
                )).keyboardRow(mutableListOf(
                    buttonForMode(TradingMode.TAKER_MAKER),
                )).keyboardRow(mutableListOf(
                    buttonForMode(TradingMode.MAKER_TAKER),
                )).keyboardRow(mutableListOf(
                    buttonForMode(TradingMode.MAKER_MAKER),
                )).keyboardRow(mutableListOf(
                    buttonsService.backButton()
                )).build()
        )
    }

    override fun update(user: BotUser): Boolean {
        val mode = TradingMode.values().find { it.name.equals(user.message, ignoreCase = true) }
        if (mode == null) {
            user.sendMessage("<b>Режим</b> <i>${user.message}<i> <b>не найден!</b>")
            commandService.back(user)
        } else {
            user.serviceUser.userSettings.tradingMode = mode
            user.searchBundles()
            commandService.back(user)
        }
        return false
    }
}
