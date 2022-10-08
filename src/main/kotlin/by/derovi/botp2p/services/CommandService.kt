package by.derovi.botp2p.services

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.commands.Command
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.util.Stack
import javax.annotation.PostConstruct

@Service
class CommandService {
    @Autowired
    lateinit var context: ApplicationContext

    lateinit var commands: Map<String, Command>
    val userToCommandStack = mutableMapOf<Long, Stack<String>>()

    @PostConstruct
    fun wireCommands() {
        commands = context.getBeansOfType(Command::class.java).values.associateBy { it.name }
    }

    fun adjustSettingsAccordingToPermissions(botUser: BotUser) {
        for (command in commands.values) {
            if (!command.role.hasPermission(botUser.serviceUser.role)) {
                command.removeAffect(botUser)
            }
        }
    }

    fun back(botUser: BotUser, number: Int = 1) {
        repeat(number) { userToCommandStack[botUser.id]?.removeLastOrNull() }
        use(lastCommand(botUser), botUser, false)
    }

    fun lastCommand(botUser: BotUser) = userToCommandStack[botUser.id]?.lastOrNull() ?: "/start"

    fun use(fullCommand: String, botUser: BotUser, addToStack: Boolean = true) {
        val name = ("$fullCommand?").substringBefore("?")
        val arguments = fullCommand.substringAfter("?").split("&")

        if (!commands.containsKey(name)) {
            botUser.sendMessage("<b>Команда</b> <i>$name</i> <b>не найдена</b>!",
                InlineKeyboardMarkup.builder().keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder().text("На главную").callbackData("/start").build()
                )).build()
            )
            return
        }
        val command = commands[name]!!
        if (command.role.hasPermission(botUser.serviceUser.role)) {
            if (addToStack && name != "/back") {
                userToCommandStack
                    .getOrPut(botUser.id) { Stack<String>() }
                    .push(fullCommand)
            }
            command.use(botUser, *arguments.toTypedArray())
        } else {
            botUser.sendMessage(
                "<b>Для этой команды нужна подписка <i>${command.role.readableName}</i></b>!",
                InlineKeyboardMarkup.builder().keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder().text("Подписка").callbackData("/subscription").build(),
                    InlineKeyboardButton.builder().text("На главную").callbackData("/start").build(),
                )).build()
            )
        }
    }
}
