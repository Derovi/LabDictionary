package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.ButtonsService
import by.derovi.botp2p.services.CommandService
import by.derovi.botp2p.services.PromoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

fun <T> deferred(block: DefferScope<T>.() -> T): T {
    val scope = DefferScope<T>()
    return scope.block().also { scope.funcs.forEach(::run) }
}

class DefferScope <T> {
    val funcs = mutableListOf<() -> Unit>()

    fun defer(func: () -> Unit) {
        funcs.add(func)
    }
}

@Component
class OrderCommand : Command {
    override val name = "/order"
    override val role = Role.UNSUBSCRIBED

    @Autowired
    lateinit var context: ApplicationContext

    @Autowired
    lateinit var buttonsService: ButtonsService

    @Autowired
    lateinit var promoService: PromoService

    override fun use(user: BotUser, vararg args: String) {
        val role = Role.values().filter(Role::isTariff).find { it.name == args[0] } ?: run {
            user.sendMessage("Не найден тариф <b>${args[0]}</b>")
            context.getBean(CommandService::class.java).back(user)
            return
        }
        val prices = promoService.prices
        user.sendMessage(
            buildString {
                append("Вы выбрали тариф ")
                val promo = user.serviceUser.promo
                val promoPrices = if (promo != null) promoService.pricesWithPromo(promo.discount) else null
                append(TariffsCommand.tariffsDescription(prices, promoPrices, listOf(role)))
                toString()
            },
            with(InlineKeyboardMarkup.builder()) {
                for ((duration, _) in prices[role] ?: mapOf()) {
                   keyboardRow(mutableListOf(
                       InlineKeyboardButton.builder()
                           .text("✅ Оплатить на ${duration.readableName2}")
                           .callbackData("/pay?$role&$duration")
                           .build()
                   ))
                }
                keyboardRow(mutableListOf(buttonsService.backButton()))
                build()
            }
        )
        // abacaba
    }
}