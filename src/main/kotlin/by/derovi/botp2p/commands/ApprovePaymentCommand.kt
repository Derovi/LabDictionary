package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.ButtonsService
import by.derovi.botp2p.services.CommandService
import by.derovi.botp2p.services.TicketsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class ApprovePaymentCommand : Command {
    override val name = "/approvePayment"
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
        ticketsService.approveByUser(ticket)
        user.sendMessage(
            buildString {
                append("Оплата тарифа <b>${ticket.role.readableName}</b> на <b>${ticket.duration.readableName2}</b>\n")
                append(ticket.role.description)
                append("\n\n")

                append("Спасибо за оплату!\n")
                append("Сумма к оплате: <b>\$${ticket.promoPrice} usdt</b>\n")
                append("Адрес:\n")
                append("<code>${ticket.address}</code>\n")
                append("Оплата будет проверена в течение 2 часов\n")
                append("Поддержка: @derovi")
            },
            with(InlineKeyboardMarkup.builder()) {
                keyboardRow(mutableListOf(
                    InlineKeyboardButton
                        .builder()
                        .text("↩️ Назад")
                        .callbackData("/start")
                        .build()
                ))
                build()
            }
        )
        // abacaba
    }
}