package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.library.Utils
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.model.Ticket
import by.derovi.botp2p.model.UserRepository
import by.derovi.botp2p.services.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.util.*

@Component
class ApproveAdminCommand : Command {
    override val name = "/approveAdmin"
    override val role = Role.ADMIN

    @Autowired
    lateinit var context: ApplicationContext

    @Autowired
    lateinit var buttonsService: ButtonsService

    @Autowired
    lateinit var ticketsService: TicketsService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var promoService: PromoService

    override fun use(user: BotUser, vararg args: String) {
        val ticket = args.firstOrNull()?.toLongOrNull()?.let(ticketsService::getTicket)?.orElse(null) ?: let {
            user.sendMessage("Заявка не найдена!")
            context.getBean(CommandService::class.java).back(user)
            return
        }
        ticketsService.approveByAdmin(ticket)
        user.sendMessage("Заявка <b>${ticket.id}</b> подтверждена!")
        val botUser = userService.getBotUserById(ticket.user.userId)

        val promo = ticket.promo
        if (promo != null) {
            promoService.promoUsed(
                promo,
                ticket.promoPrice,
                ticket.role,
                ticket.duration
            )
        }
        botUser.serviceUser.referBonus -= ticket.referBonusUsed
        userRepository.save(botUser.serviceUser)
        botUser.sendMessage(buildString {
            append("✅ Вам выдана подписка <b>${ticket.role.readableName}</b> на <b>${ticket.duration.readableName2}</b>\n")
            append("Спасибо за оплату!")
            toString()
        },
            with(InlineKeyboardMarkup.builder()) {
                keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder()
                        .text("↩️ Главное меню")
                        .callbackData("/start")
                        .build()
                ))
                build()
            }
        )
        context.getBean(CommandService::class.java).back(user)
    }
}