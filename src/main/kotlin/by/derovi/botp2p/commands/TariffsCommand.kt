package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.model.SubscriptionDuration
import by.derovi.botp2p.services.ButtonsService
import by.derovi.botp2p.services.CommandService
import by.derovi.botp2p.services.PromoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class TariffsCommand : Command {
    override val name = "/tariffs"
    override val role = Role.UNSUBSCRIBED

    @Autowired
    lateinit var context: ApplicationContext

    @Autowired
    lateinit var buttonsService: ButtonsService

    @Autowired
    lateinit var promoService: PromoService

    companion object {
        fun tariffsDescription(
            prices: Map<Role, Map<SubscriptionDuration, Int>>,
            promoPrices: Map<Role, Map<SubscriptionDuration, Int>>?,
            tariffs: List<Role>
        ) = buildString {
            for (role in tariffs) {
                append("<b>").append(role.readableName).append("</b>\n")
                append(role.description)
                append("\n↓\n")
                for ((duration, price) in prices[role] ?: mapOf()) {
                    val promoPrice = promoPrices?.get(role)?.get(duration) ?: price
                    append("").append(duration.readableName).append(" - ")
                    if (price != promoPrice) {
                        append("<s>$price</s> <b>$promoPrice usdt</b>\n")
                    } else {
                        append("<b>$promoPrice usdt</b>\n")
                    }
                }
                append("\n")
            }
            toString()
        }
    }
    override fun use(user: BotUser, vararg args: String) {
        val tariffs = Role.values().filter(Role::isShown)
        user.sendMessage(
            with(StringBuilder()) {
                val prices = promoService.prices
                val promo = user.serviceUser.promo
                val promoPrices = if (promo != null) promoService.pricesWithPromo(promo.discount) else null
                append(tariffsDescription(prices, promoPrices, tariffs))
                if (promo != null && promo.discount > 0) {
                    append("У вас действует скидка " +
                            "<b>${PromoService.percentToReadable(promo.discount)}</b>")
                }
                toString()
            },
            with(InlineKeyboardMarkup.builder()) {
                for (role in tariffs) {
                    keyboardRow(mutableListOf(
                        InlineKeyboardButton
                            .builder()
                            .text(role.readableName)
                            .callbackData("/order?${role.name}")
                            .build()
                    ))
                }
                keyboardRow(mutableListOf(buttonsService.backButton()))
                build()
            }
        )
    }
}