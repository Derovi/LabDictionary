package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.library.Utils
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.model.Ticket
import by.derovi.botp2p.model.UserRepository
import by.derovi.botp2p.services.ButtonsService
import by.derovi.botp2p.services.CommandService
import by.derovi.botp2p.services.TicketsService
import by.derovi.botp2p.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.util.*

@Component
class CancelAdminCommand : Command {
    override val name = "/cancelAdmin"
    override val role = Role.ADMIN

    @Autowired
    lateinit var context: ApplicationContext

    @Autowired
    lateinit var buttonsService: ButtonsService

    @Autowired
    lateinit var ticketsService: TicketsService

    @Autowired
    lateinit var userService: UserService

    override fun use(user: BotUser, vararg args: String) {
        val ticket = args.firstOrNull()?.toLongOrNull()?.let(ticketsService::getTicket)?.orElse(null) ?: let {
            user.sendMessage("Заявка не найдена!")
            context.getBean(CommandService::class.java).back(user)
            return
        }
        ticketsService.cancelByAdmin(ticket)
        user.sendMessage("Заявка <b>${ticket.id}</b> отменена!")
        val botUser = userService.getBotUserById(ticket.user.userId)
        botUser.sendMessageWithBackButton(buildString {
            append("\uD83D\uDEAB Ваша оплата ")
            if (ticket.approvedByUserAt != null) {
                append(Utils.formatDate(ticket.approvedByUserAt!!)).append(" ")
            }
            append("<b>не была</b> подтверждена!\n")
            append("Если вы хотите это оспорить, обратитесь в поддержку @derovi")
            toString()
        })
        context.getBean(CommandService::class.java).back(user)
    }
}