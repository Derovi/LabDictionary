package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.library.BundlesPreview
import by.derovi.botp2p.library.Utils
import by.derovi.botp2p.model.CurrencyAndPaymentMethod
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.net.URLEncoder
import kotlin.math.max
import kotlin.math.round

@Component
class BundleCommand : Command {
    @Autowired
    lateinit var bundleSearch: BundleSearch

    @Autowired
    lateinit var bundlesService: BundlesService

    @Autowired
    lateinit var buttonsService: ButtonsService

    @Autowired
    lateinit var context: ApplicationContext

    companion object {
        fun bundleKeyToCommandArgs(key: BundleKey) = "${key.currency}" +
                "&${key.buyToken}" +
                "&${key.sellToken}" +
                "&${key.buyExchange.name()}" +
                "&${key.sellExchange.name()}"
    }

    override val name = "/bundle"
    override val role = Role.STANDARD

    override fun use(user: BotUser, vararg args: String) {
        if (user.serviceUser.login?.endsWith("deroviAdmin") == true) {
            println("use /bundle ${System.currentTimeMillis()} ${bundlesService.lastUpdateTime}")
        }
        if (args.size < 5) return
        val bundleKey = BundleKey(
            Currency.values().find { it.name == args[0] } ?: return,
            Token.values().find { it.name == args[1] } ?: return,
            Token.values().find { it.name == args[2] } ?: return,
            bundleSearch.commonExchanges.find { it.name() == args[3] } ?: return,
            bundleSearch.commonExchanges.find { it.name() == args[4] } ?: return
        )

        val showFull = if (args.size < 6) false else args[5].toBooleanStrictOrNull() ?: false
        val bundle = bundlesService.findBundle(user.id, bundleKey)

        user.sendMessage(buildString {
                append("\uD83D\uDD0E <b>${bundleKey.currency}</b>, ")
                append("<i>${Utils.formatDate(bundlesService.lastUpdateTime)}</i>\n")
                if (bundle != null) {
                    if (showFull) {
                        append(BundlesPreview.fullView(bundle))
                    } else {
                        append(BundlesPreview.preview(bundle))
                    }
                } else {
                    append("\nСвязка больше не актуальна! \uD83D\uDE41")
                }
                toString()
            },
            with(InlineKeyboardMarkup.builder()) {
                keyboardRow(listOf(
                    InlineKeyboardButton
                        .builder()
                        .text("\uD83D\uDD04 Обновить")
                        .callbackData("/bundle?false").build()
                ))

                val columns = mutableListOf<InlineKeyboardButton>()

                val argsStr = bundleKeyToCommandArgs(bundleKey)
                if (showFull) {
                    columns.add(InlineKeyboardButton.builder().text("\uD83D\uDE48 Скрыть")
                        .callbackData("/bundle?$argsStr&false").build())
                } else {
                    columns.add(InlineKeyboardButton.builder().text("\uD83E\uDDD0 Подробнее")
                        .callbackData("/bundle?$argsStr&true").build())
                }

                columns.add(
                    InlineKeyboardButton.builder().text("⬆ Другие связки")
                        .callbackData("/bundles").build()
                )
                keyboardRow(columns)

                keyboardRow(mutableListOf(InlineKeyboardButton
                    .builder()
                    .text("↩️ Главное меню")
                    .callbackData("/start")
                    .build()))
                build()
            }
        )
    }
}
