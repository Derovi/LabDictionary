package by.derovi.botp2p

import by.derovi.botp2p.exchange.BundleSearch
import by.derovi.botp2p.lang.LangMap
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.model.ServiceUser
import by.derovi.botp2p.services.BundlesService
import org.springframework.context.ApplicationContext
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

class BotUser(
    val serviceUser: ServiceUser,
    var message: String,
    var silent: Boolean,
    var lang: LangMap,
    var context: ApplicationContext
) {
    val bot = context.getBean(Bot::class.java)
    val bundlesService = context.getBean(BundlesService::class.java)

    fun sendMessage(text: String) {
        sendMessage(text, null)
    }

    fun sendMessageWithBackButton(text: String) {
        sendMessage(text, InlineKeyboardMarkup.builder().keyboardRow(
            mutableListOf(InlineKeyboardButton.builder().text("Назад").callbackData("/back").build())
        ).build())
    }

    fun searchBundles() {
        bundlesService.searchBundlesForUser(serviceUser)
    }

    fun sendMessage(text: String, keyboard: InlineKeyboardMarkup?) {
        if (silent) {
            return
        }
        val message = SendMessage()

        message.setChatId(serviceUser.chatId)
        message.text = if (text.isEmpty()) "No text?!" else text

        message.replyMarkup = keyboard
        message.parseMode = "HTML"
        message.disableWebPagePreview()
        bot.execute(message)
    }

    val id = serviceUser.userId
    val isAdmin: Boolean
        get() = serviceUser.role == Role.ADMIN
    val loginOrId = serviceUser.login ?: id
}
