package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.exchange.Offer
import by.derovi.botp2p.exchange.OrderType
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.library.BundlesPreview
import by.derovi.botp2p.library.Utils
import by.derovi.botp2p.model.CurrencyAndPaymentMethod
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.BundlesService
import by.derovi.botp2p.services.ButtonsService
import by.derovi.botp2p.services.CommandService
import by.derovi.botp2p.services.FeesService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.net.URLEncoder
import kotlin.math.max
import kotlin.math.round

@Component
class BundlesCommand : Command {
    @Autowired
    lateinit var bundlesService: BundlesService

    @Autowired
    lateinit var buttonsService: ButtonsService

    @Autowired
    lateinit var context: ApplicationContext

    override val name = "/bundles"
    override val role = Role.STANDARD

    override fun use(user: BotUser, vararg args: String) {
        val commandService = context.getBean(CommandService::class.java)

        val ttOnly = args.getOrNull(1)?.toBooleanStrictOrNull() ?: false
        val showFull = args.getOrNull(2)?.toBooleanStrictOrNull() ?: false
        val bundles = if (ttOnly) bundlesService.userToBundleSearchResulTT[user.id] else bundlesService.userToBundleSearchResult[user.id]

//        val time = "<i>${Utils.formatDate(bundlesService.lastUpdateTime)}</i>"

        val time = "<i>${Utils.formatDate(System.currentTimeMillis())}</i>"
        // buttons
        // end buttons

        if (bundles == null || bundles.isEmpty()) {
            user.sendMessage(
                buildString {
                    append("<i>${Utils.formatDate(bundlesService.lastUpdateTime)}</i>\n")
                    append("<b>Связок нет! Попробуйте поиск в режиме Тейкер-Мейкер.</b>")
                    toString()
                },
                with(InlineKeyboardMarkup.builder()) {
                    keyboardRow(listOf(
                        InlineKeyboardButton
                            .builder()
                            .text("\uD83D\uDD04 Обновить")
                            .callbackData("/bundles?0&$ttOnly&$showFull").build()
                    ))
                    keyboardRow(mutableListOf(buttonsService.modeButton(user.serviceUser.userSettings)))
                    keyboardRow(mutableListOf(InlineKeyboardButton.builder().text("↩️ Главное меню").callbackData("/start").build()))
                    build()
                },
                BotUser.UpdatableCommand(name, args.toList().toTypedArray())
            )
            return
        }

        val bundleIdx = args.firstOrNull()?.toIntOrNull().takeIf { it in bundles.indices } ?: 0

        val bundle = bundles[bundleIdx]
        val text = with(StringBuilder()) {
            append("<b>${bundleIdx + 1}/${bundles.size}, ${bundle.currency}</b>, " +
                    "$time\n")
            if (showFull) {
                append(BundlesPreview.fullView(bundle))
            } else {
                append(BundlesPreview.preview(bundle))
            }
            toString()
        }

        user.sendMessage(
            text,
            with(InlineKeyboardMarkup.builder()) {
                keyboardRow(listOf(
                    InlineKeyboardButton
                        .builder()
                        .text("\uD83D\uDD04 Обновить")
                        .callbackData("/bundles?$bundleIdx&$ttOnly&$showFull").build()
                ))

                val columns = mutableListOf<InlineKeyboardButton>()
                if (bundleIdx > 0) columns.add(
                    InlineKeyboardButton.builder().text("⬅️ Предыдущая").callbackData("/bundles?${bundleIdx - 1}&$ttOnly").build()
                )
                if (showFull) {
                    columns.add(InlineKeyboardButton.builder().text("\uD83D\uDE48 Скрыть")
                        .callbackData("/bundles?$bundleIdx&$ttOnly&false").build())
                } else {
                    columns.add(InlineKeyboardButton.builder().text("\uD83E\uDDD0 Подробнее")
                        .callbackData("/bundles?$bundleIdx&$ttOnly&true").build())
                }

                if (bundleIdx + 1 < bundles.size) {
                    columns.add(
                        InlineKeyboardButton.builder().text("➡️ Следующая").callbackData("/bundles?${bundleIdx + 1}&$ttOnly").build()
                    )
                }
                keyboardRow(columns)

                val availableCurrencies = user.serviceUser.userSettings.paymentMethods
                    .map(CurrencyAndPaymentMethod::currency)
                    .distinct()

                val chosenCurrency = user.serviceUser.userSettings.chosenCurrency
                if (availableCurrencies.size > 1
                    || (chosenCurrency != null && chosenCurrency != availableCurrencies.first())) {
                    keyboardRow(with(mutableListOf<InlineKeyboardButton>()) {
                        add(
                            InlineKeyboardButton
                                .builder()
                                .text("Все" + if (chosenCurrency == null) " ✓" else "")
                                .callbackData("/chooseCurrency")
                                .build()
                        )

                        for (currency in availableCurrencies) {
                            add(
                                InlineKeyboardButton
                                    .builder()
                                    .text(currency.name + if (chosenCurrency == currency) " ✓" else "")
                                    .callbackData("/chooseCurrency?${currency.name}")
                                    .build()
                            )
                        }
                        this
                    })
                }
                keyboardRow(mutableListOf(buttonsService.modeButton(user.serviceUser.userSettings)))
                keyboardRow(mutableListOf(InlineKeyboardButton.builder().text("↩️ Главное меню").callbackData("/start").build()))
                build()
            },
            BotUser.UpdatableCommand(name, args.toList().toTypedArray())
        )
    }
}
