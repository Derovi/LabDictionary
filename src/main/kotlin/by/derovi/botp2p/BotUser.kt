package by.derovi.botp2p

import by.derovi.botp2p.lang.LangMap
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.model.ServiceUser
import by.derovi.botp2p.services.BundlesService
import by.derovi.botp2p.services.ScreenService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationContext
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import kotlin.concurrent.thread

class BotUser(
    val serviceUser: ServiceUser,
    var message: String,
    val update: Update?,
    val callbackMessageId: Int?,
    var silent: Boolean,
    var lang: LangMap,
    var context: ApplicationContext
) {
    val bot = context.getBean(Bot::class.java)
    val bundlesService = context.getBean(BundlesService::class.java)
//    val screenService = context.getBean(ScreenService::class.java)
    var messageSent: Boolean = false

    val isVirtual: Boolean
        get() = update == null

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

    class UpdatableCommand(val command: String, val args: Array<String>)

    fun sendMessage(text: String, keyboard: InlineKeyboardMarkup?, updatable: UpdatableCommand? = null) {
        if (silent) {
            return
        }
//        val message = EditMessageText()
        if (callbackMessageId != null && !messageSent) {
            if (updatable != null) {
//                screenService.onUpdatableEdited(
//                    this,
//                    callbackMessageId,
//                    updatable.command,
//                    updatable.args
//                )
//                if (screenService.isLastUpdate) {
//                    println("last update")
//                    if (keyboard != null) {
//                        keyboard.keyboard = keyboard.keyboard.toMutableList().also { it.add(0, listOf(
//                            InlineKeyboardButton
//                                .builder()
//                                .text("\uD83D\uDD04 Обновить")
//                                .callbackData("${updatable.command}?${updatable.args.joinToString("&")}").build()
//                        )) }
//                    }
//                }
            } else {
//                screenService.onNonUpdatableEdited(this, callbackMessageId)
            }
            val message = EditMessageText()
            message.messageId = callbackMessageId
            message.setChatId(serviceUser.chatId)
            message.text = if (text.isEmpty()) "No text?!" else text

            message.replyMarkup = keyboard
            message.parseMode = "HTML"
            message.disableWebPagePreview()
            thread {
                try {
                    bot.execute(message)
                } catch (_: Exception) {}
            }
        } else {
            val message = SendMessage()
//            screenService.onAnySent(this)
            message.setChatId(serviceUser.chatId)
            message.text = if (text.isEmpty()) "No text?!" else text

            message.replyMarkup = keyboard
            message.parseMode = "HTML"
            message.disableWebPagePreview()
            thread {
                bot.execute(message)
            }
        }
        messageSent = true
    }

    val id = serviceUser.userId
    val isAdmin: Boolean
        get() = serviceUser.role == Role.ADMIN
    val loginOrId = serviceUser.login ?: id
}
