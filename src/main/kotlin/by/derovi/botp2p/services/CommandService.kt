package by.derovi.botp2p.services

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.commands.Command
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.util.Stack
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import kotlin.random.Random

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

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    fun removeExpired() {
        managedCommands.values.removeIf { it.createdAt + 60 * 60 * 1000 < System.currentTimeMillis() }
    }
    
    class ManagedCommand(val command: String, val createdAt: Long)

    val managedCommands = mutableMapOf<Long, ManagedCommand>()
    fun getManagedCommand(id: Long) = managedCommands[id]
    fun manageCommand(command: String): Long {
        val id = Random.nextLong()
        managedCommands[id] = ManagedCommand(command, System.currentTimeMillis())
        return id
    }
    fun manageCommandUrl(command: String): String {
        val id = manageCommand(command)
        return "/managedcommand?$id"
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

    fun backImplicit(botUser: BotUser, number: Int = 1) {
        repeat(number) { userToCommandStack[botUser.id]?.removeLastOrNull() }
    }

    fun lastCommand(botUser: BotUser) = userToCommandStack[botUser.id]?.lastOrNull() ?: "/start"

    fun use(command: String, args: Array<String>, botUser: BotUser, addToStack: Boolean = true) {
        use(command + "?" + args.joinToString("&"), botUser, addToStack)
    }

    fun use(fullCommand: String, botUser: BotUser, addToStack: Boolean = true) {
        val name = ("$fullCommand?").substringBefore("?")
        val arguments = fullCommand.substringAfter("?").split("&")

        if (!commands.containsKey(name)) {
            if (name.startsWith("/")) {
                botUser.sendMessage(
                    "<b>Команда</b> <i>$name</i> <b>не найдена</b>!",
                    InlineKeyboardMarkup.builder().keyboardRow(
                        mutableListOf(
                            InlineKeyboardButton.builder().text("На главную").callbackData("/start").build()
                        )
                    ).build()
                )
            }
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
                "Для этой команды нужна подписка <b>${command.role.readableName}</b>!",
                with(InlineKeyboardMarkup.builder()) {
                    keyboardRow(mutableListOf(
                        InlineKeyboardButton.builder()
                            .text("\uD83D\uDC49 Подписка")
                            .callbackData("/subscription").build()
                    ))
                    keyboardRow(mutableListOf(
                        InlineKeyboardButton.builder()
                            .text("↩️ На главную").
                            callbackData("/start").build()
                    ))
                    build()
                }
            )
        }
    }
}
