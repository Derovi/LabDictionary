package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.model.SubscriptionDuration
import by.derovi.botp2p.services.ButtonsService
import by.derovi.botp2p.services.CommandService
import by.derovi.botp2p.services.PromoService
import by.derovi.botp2p.services.TicketsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class PayCommand : Command {
    override val name = "/pay"
    override val role = Role.UNSUBSCRIBED

    @Autowired
    lateinit var context: ApplicationContext

    @Autowired
    lateinit var buttonsService: ButtonsService

    @Autowired
    lateinit var promoService: PromoService

    @Autowired
    lateinit var ticketService: TicketsService

    override fun use(user: BotUser, vararg args: String) {
        println("pay command")
        val role = Role.values().filter(Role::isTariff).find { it.name == args[0] } ?: run {
            user.sendMessage("Не найден тариф <b>${args[0]}</b>")
            context.getBean(CommandService::class.java).back(user)
            return
        }
        val duration = SubscriptionDuration.values().find { it.name == args[1] } ?: run {
            user.sendMessage("Неправильная длительность <b>${args[1]}</b>")
            context.getBean(CommandService::class.java).back(user)
            return
        }
        val price = promoService.prices[role]!![duration]!!

        user.sendMessage(
            buildString {
                append("Оплата тарифа <b>${role.readableName}</b> на <b>${duration.readableName2}</b>\n")
                val promo = user.serviceUser.promo
                val promoPrices = if (promo != null) promoService.pricesWithPromo(promo.discount) else null

                append(role.description)
                append("\n\n")

                val promoPrice = promoPrices?.get(role)?.get(duration) ?: price
//                if (price != promoPrice) {
//                    append("<s>$price</s> <b>$promoPrice usdt</b>\n")
//                } else {
//                    append("<b>$promoPrice usdt</b>\n")
//                }

                append("Сумма к оплате: <b>${promoPrice} usdt</b>\n")
                append("Адрес:\n")
                val ticket = ticketService.createTicket(
                    user.serviceUser,
                    price.toLong(),
                    promoPrice.toLong(),
                    role,
                    duration
                )
                append("<code>${ticket.address}</code>\n")
                append("После оплаты нажмите кнопку Я ОПЛАТИЛ\n")
                append("Поддержка: @derovi")
                println("build text")
                toString()
            },
            with(InlineKeyboardMarkup.builder()) {
                keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder()
                        .text("Я оплатил")
                        .callbackData("/approvePayment")
                        .build()
                ))
                keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder()
                        .text("Отменить оплату")
                        .callbackData("/cancelPayment")
                        .build()
                ))
                println("build markup")
                build()
            }
        )
        // abacaba
    }
}