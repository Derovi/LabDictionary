package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.library.Utils
import by.derovi.botp2p.model.Role
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
class TicketsCommand : Command {
    override val name = "/tickets"
    override val role = Role.ADMIN

    @Autowired
    lateinit var context: ApplicationContext

    @Autowired
    lateinit var buttonsService: ButtonsService

    @Autowired
    lateinit var promoService: PromoService

    @Autowired
    lateinit var ticketService: TicketsService

    override fun use(user: BotUser, vararg args: String) {
        val page = args.firstOrNull()?.toIntOrNull() ?: 0
        val tickets = ticketService.getAllTickets()
        val ticket = tickets.getOrNull(page)
        if (tickets.isEmpty()) {
            user.sendMessage(
                "Тикетов пока нет, ${Utils.formatDate(System.currentTimeMillis())}",
                with(InlineKeyboardMarkup.builder()) {
                    keyboardRow(listOf(
                        InlineKeyboardButton
                            .builder()
                            .text("\uD83D\uDD04 Обновить")
                            .callbackData("/tickets").build()
                    ))
                    keyboardRow(mutableListOf(InlineKeyboardButton.builder().text("↩️ Главное меню").callbackData("/start").build()))
                    build()
                }
            )
            return
        }
        if (ticket == null) {
            user.sendMessage("Тикет не найден!")
            context.getBean(CommandService::class.java).back(user)
            return
        }

        user.sendMessage(
            buildString {
                append("<b>${page + 1}/${tickets.size}</b>, ${Utils.formatDate(System.currentTimeMillis())}\n\n")
                append("Пользователь: ")
                if (ticket.user.login != null) {
                    append("@").append(ticket.user.login)
                } else {
                    append("<b>").append(ticket.user.userId).append("</b>")
                }
                append("\n")
                append("Адрес TRC-20: <code>").append(ticket.address).append("</code>\n")
                append("К оплате: <b>").append(ticket.promoPrice).append(" usdt</b>\n")
                if (ticket.price != ticket.promoPrice) {
                    append("К оплате без скидки: <s>").append(ticket.price).append("</s>\n")
                }
                val promo = ticket.promo
                if (promo != null) {
                    append("Промо: <code>").append(promo.id).append("</code>")
                    if (promo.referId != null) {
                        append(" #${promo.referId}")
                    }
                    append("\n")
                }
                append("Использовано бонусов: <b>").append(ticket.referBonusUsed).append(" usdt</b>\n")
                append("Всего бонусов: <b>").append(ticket.user.referBonus).append(" usdt</b>\n")
                append("Подписка: <b>${ticket.role.readableName}</b>\n")
                append("Длительность: <b>${ticket.duration.readableName}</b>\n")
                append("Создано: <b>${Utils.formatDate(ticket.createdAt)}</b>\n")
                if (ticket.approvedByUserAt == null) {
                    append("Не подтверждена пользователем\n")
                } else {
                    append("Подтверждена пользователем: <b>${Utils.formatDate(ticket.approvedByUserAt!!)}</b>")
                }
            },
            with(InlineKeyboardMarkup.builder()) {
                keyboardRow(listOf(
                    InlineKeyboardButton
                        .builder()
                        .text("\uD83D\uDD04 Обновить")
                        .callbackData("/tickets?$page").build()
                ))

                val columns = mutableListOf<InlineKeyboardButton>()
                if (page > 0) columns.add(
                    InlineKeyboardButton.builder().text("⬅️ Предыдущая").callbackData("/tickets?${page - 1}").build()
                )

                if (page + 1 < tickets.size) {
                    columns.add(
                        InlineKeyboardButton.builder().text("➡️ Следующая").callbackData("/tickets?${page + 1}").build()
                    )
                }
                keyboardRow(columns)
                keyboardRow(mutableListOf(
                    InlineKeyboardButton.builder()
                        .text("✅ Подтвердить")
                        .callbackData("/approveAdmin?${ticket.id}")
                        .build(),
                    InlineKeyboardButton.builder()
                        .text("\uD83D\uDEAB Отменить")
                        .callbackData("/cancelAdmin?${ticket.id}")
                        .build(),
                ))
                keyboardRow(mutableListOf(InlineKeyboardButton.builder().text("↩️ Главное меню").callbackData("/start").build()))
                build()
            }
        )
        // abacaba
    }
}
