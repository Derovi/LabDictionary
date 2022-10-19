package by.derovi.botp2p.services

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.dialogs.Dialog
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

@Service
class DialogService {
//    @Autowired
//    lateinit var commandService: CommandService

    @Autowired
    lateinit var applicationContext: ApplicationContext

    val userIDToDialog = mutableMapOf<Long, Dialog>()

    fun isDialogActive(botUser: BotUser) = userIDToDialog.contains(botUser.id)
    fun isDialogActive(userId: Long) = userIDToDialog.contains(userId)

    fun <T : Dialog> createDialogObject(dialogClass: Class<T>) = applicationContext.getBean(dialogClass)

    fun startDialog(user: BotUser, dialogClass: Class<out Dialog>, args: List<String> = listOf()) {
        startDialog(user, applicationContext.getBean(dialogClass), args)
    }

    fun startDialog(user: BotUser, dialog: Dialog, args: List<String> = listOf()) {
        if (userIDToDialog.contains(user.id)) {
            stopDialog(user)
        }
        userIDToDialog[user.id] = dialog
        dialog.start(user, args)
    }

    fun continueDialog(user: BotUser) {
        if (userIDToDialog.contains(user.id)) {
            if (!userIDToDialog[user.id]!!.update(user)) {
                stopDialog(user)
            }
        } else {
            applicationContext.getBean(CommandService::class.java).use("/start", user)
        }
    }

    fun stopDialog(user: BotUser) {
        userIDToDialog.remove(user.id)
    }
}