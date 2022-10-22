package by.derovi.botp2p.dialogs

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.commands.ExportCommand
import by.derovi.botp2p.commands.SearchSettingsCommand
import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.model.CurrencyAndPaymentMethod
import by.derovi.botp2p.model.SearchSettings
import by.derovi.botp2p.model.SearchSettingsRepository
import by.derovi.botp2p.services.CommandService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope("prototype")
class ImportDialog : Dialog {
    @Autowired
    lateinit var bundleSearch: BundleSearch

    @Autowired
    lateinit var commandService: CommandService

    @Autowired
    lateinit var searchSettingsRepository: SearchSettingsRepository

    val objectMapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)

    var settingsIdx = 0
    lateinit var searchSettings: SearchSettings

    lateinit var currency: Currency
    override fun start(user: BotUser, args: List<String>) {
        settingsIdx = args.firstOrNull()?.toIntOrNull() ?: 0
        val settings = user.serviceUser.userSettings
        searchSettings = SearchSettingsCommand.getSettingsByIdx(settings, settingsIdx)
        val (buy, taker) = SearchSettingsCommand.getCharacteristicsByIdx(settingsIdx)

        user.sendMessageWithBackButton(
            with(StringBuilder()) {
                append("\uD83D\uDCE5 Загрузка настроек")
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
                append("\n Отправьте выгрузку, полученную с помощью команды <b>Экспорт</b> для загрузки настроек")
                toString()
            }
        )
    }

    override fun update(user: BotUser): Boolean {
        val text = user.message

        val settingsWrapper = try {
            objectMapper.readValue(text, ExportCommand.SearchSettingsWrapper::class.java)
        } catch (_: Exception) {
            user.sendMessage("Некорректная выгрузка!")
            commandService.back(user)
            return false
        }

        settingsWrapper.exchanges.forEach(::println)
        settingsWrapper.unwrap(searchSettings)
        searchSettings.exchanges.forEach(::println)
        searchSettingsRepository.save(searchSettings)
        user.sendMessage("\uD83D\uDCE5 Настройки загружены")
        commandService.back(user)
        return false
    }
}
