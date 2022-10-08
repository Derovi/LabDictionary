package by.derovi.botp2p.services

import by.derovi.botp2p.Bot
import by.derovi.botp2p.BotUser
import by.derovi.botp2p.library.Utils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import javax.transaction.Transactional

@Service
class UpdateService {

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var commandService: CommandService

    @Autowired
    lateinit var langService: LangService

    @Autowired
    lateinit var dialogService: DialogService

    @Autowired
    lateinit var context: ApplicationContext

    @Transactional
    fun handleUpdate(update: Update, bot: Bot) {
        try {
            if (update.hasMessage() || update.hasCallbackQuery()) {
                val (from, text, chatId) = if (update.hasMessage())
                    Triple(update.message.from, update.message.text, update.message.chatId)
                else
                    Triple(update.callbackQuery.from, update.callbackQuery.data, update.callbackQuery.message.chatId)

                val serviceUser = userService.syncUser(from.id, from.userName, chatId)
                val user = BotUser(
                    serviceUser,
                    text,
                    false,
                    langService.getLangMap(serviceUser.userSettings.lang, from.languageCode),
                    context
                )
                if (user.serviceUser.banned) {
                    user.sendMessage("<b>Вас забанили =(</b>")
                    return
                }

                if (text.startsWith("/") || !dialogService.isDialogActive(user)) {
                    useCommandWithArguments(text, user)
                } else {
                    dialogService.continueDialog(user)
                }

                userService.userRepository.save(user.serviceUser)
            }

        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    fun useCommandWithArguments(rawText: String, user: BotUser) {
        val text = if (rawText.startsWith("/start") && rawText.contains(" "))
            String(Utils.base62.decode(rawText.substringAfter(" ").toByteArray())) else rawText
        dialogService.stopDialog(user)
        val commands = text.split("#")
        if (commands.size == 1) {
            commandService.use(text, user)
        } else {
            user.silent = true
            user.message = commands[0]
            commandService.use(commands[0], user)
            for (idx in 1.. commands.size - 2) {
                user.message = commands[idx]
                dialogService.continueDialog(user)
            }
            user.silent = false
            user.message = commands.last()
            dialogService.continueDialog(user)
        }
    }
}