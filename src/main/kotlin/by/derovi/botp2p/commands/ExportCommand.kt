package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.exchange.BundleSearch
import by.derovi.botp2p.exchange.Currency
import by.derovi.botp2p.exchange.PaymentMethod
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.model.*
import by.derovi.botp2p.services.ButtonsService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class ExportCommand : Command {
    @Autowired
    lateinit var buttonsService: ButtonsService

    override val name = "/export"
    override val role = Role.STANDARD

    val objectMapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)

    class SearchSettingsWrapper(
        var tokens: MutableList<Token>,
        var exchanges: MutableList<String>,
        var paymentMethods: Map<Currency, List<PaymentMethod>>
    ) {
        constructor() : this(mutableListOf(), mutableListOf(), mapOf())

        companion object {
            fun from(searchSettings: SearchSettings) = SearchSettingsWrapper(
                searchSettings.tokens,
                searchSettings.exchanges,
                searchSettings.paymentMethodsAsMap
            )
        }

        fun unwrap(searchSettings: SearchSettings) {
            searchSettings.tokens = tokens
            searchSettings.exchanges = exchanges
            searchSettings.paymentMethods = paymentMethods.entries.asSequence()
                .map { entry -> entry.value.map { CurrencyAndPaymentMethod(entry.key, it) } }
                .flatten()
                .toMutableList()
        }
    }

    override fun use(user: BotUser, vararg args: String) {
        val settingsIdx = args.firstOrNull()?.toIntOrNull() ?: 0
        val settings = user.serviceUser.userSettings
        val searchSettings = SearchSettingsCommand.getSettingsByIdx(settings, settingsIdx)
        val (buy, taker) = SearchSettingsCommand.getCharacteristicsByIdx(settingsIdx)

        user.sendMessageWithBackButton(
            with(StringBuilder()) {
                append("\uD83D\uDCE4 Выгрузка настроек")
                if (buy != null || taker != null) {
                    append(" для ")
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

                append("<code>")
                append(objectMapper.writeValueAsString(SearchSettingsWrapper.from(searchSettings)))
                append("</code>")
                append("\n Скопируйте выгрузку и используйте <b>Импорт</b> для загрузки этих настроек")
                toString()
            }
        )
    }
}
