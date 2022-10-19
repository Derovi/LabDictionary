package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.ButtonsService
import by.derovi.botp2p.services.CommandService
import by.derovi.botp2p.services.TicketsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class CancelPaymentCommand : Command {
    override val name = "/cancelPayment"
    override val role = Role.UNSUBSCRIBED

    @Autowired
    lateinit var context: ApplicationContext

    @Autowired
    lateinit var buttonsService: ButtonsService

    @Autowired
    lateinit var ticketsService: TicketsService

    override fun use(user: BotUser, vararg args: String) {
        val ticket = ticketsService.getTicket(user.serviceUser) ?: let {
            user.sendMessage("Заявка не найдена!")
            context.getBean(CommandService::class.java).back(user, 2)
            return
        }
        user.sendMessage(
            buildString {
                append("Оплата тарифа <b>${ticket.role.readableName}</b> на <b>${ticket.duration.readableName2}</b> отменена!\n")
            }
        )
        ticketsService.cancelByUser(ticket)
        context.getBean(CommandService::class.java).back(user, 2)
        // abacaba
    }
}