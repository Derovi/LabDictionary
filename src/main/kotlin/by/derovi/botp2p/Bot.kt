package by.derovi.botp2p

import by.derovi.botp2p.exchange.BundleSearch
import by.derovi.botp2p.services.CommandService
import by.derovi.botp2p.services.DialogService
import by.derovi.botp2p.services.UpdateService
import by.derovi.botp2p.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import javax.annotation.PostConstruct
import javax.transaction.Transactional
import kotlin.math.round

// Аннотация @Component необходима, чтобы наш класс распознавался Spring, как полноправный Bean
@Component // Наследуемся от TelegramLongPollingBot - абстрактного класса Telegram API
class Bot : TelegramLongPollingBot() {

    @Autowired
    lateinit var bundleSearch: BundleSearch

    // Геттеры, которые необходимы для наследования от TelegramLongPollingBot
    // Аннотация @Value позволяет задавать значение полю путем считывания из application.yaml
    @Value("\${bot.name}")
    lateinit var botUsernameValue: String

    @Value("\${bot.token}")
    lateinit var botTokenValue: String

    @Autowired
    lateinit var updateService: UpdateService

    override fun onUpdateReceived(update: Update) {
        updateService.handleUpdate(update, this)
    }

    override fun getBotToken(): String {
        return botTokenValue
    }

    override fun getBotUsername(): String {
        return botUsernameValue
    }
}