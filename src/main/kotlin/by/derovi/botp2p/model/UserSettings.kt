package by.derovi.botp2p.model

import by.derovi.botp2p.exchange.TradingMode
import by.derovi.botp2p.exchange.Currency
import by.derovi.botp2p.exchange.PaymentMethod
import by.derovi.botp2p.exchange.Token
import javax.persistence.*

@Embeddable
data class UserSettings(
    var notificationThreshold: Double, // 0 - 100 percent
    var notificationsOn: Boolean,
    var useSpot: Boolean,
    @Enumerated(value = EnumType.STRING)
    var tradingMode: TradingMode,
    var lang: String?,
    var minimumValue: Int,
    var workValue: Int,
    @ElementCollection(fetch = FetchType.EAGER)
    var banned: MutableList<Maker>,
    @Enumerated(value = EnumType.STRING)
    var chosenCurrency: Currency?,
    var settingsMode: SettingsMode,
    @OneToOne
    var commonSettings: SearchSettings, // 0
    @OneToOne
    var buySettings: SearchSettings, // 1
    @OneToOne
    var sellSettings: SearchSettings, // 2
    @OneToOne
    val takerSettings: SearchSettings, // 3
    @OneToOne
    val makerSettings: SearchSettings, // 4
    @OneToOne
    val buyTakerSettings: SearchSettings, // 5
    @OneToOne
    val sellTakerSettings: SearchSettings, // 6
    @OneToOne
    val buyMakerSettings: SearchSettings, // 7
    @OneToOne
    val sellMakerSettings: SearchSettings // 8
) {
    fun getSearchSettings(buy: Boolean, taker: Boolean) =
        when (settingsMode) {
            SettingsMode.STANDARD -> commonSettings
            SettingsMode.TAKER_MAKER -> if (taker) takerSettings else makerSettings
            SettingsMode.BUY_SELL -> if (buy) buySettings else sellSettings
            SettingsMode.BUY_SELL_TAKER_MAKER -> when(buy to taker) {
                true to true -> buyTakerSettings
                false to true -> sellTakerSettings
                true to false -> buyMakerSettings
                false to false -> sellMakerSettings
                else -> throw Error("Impossible branch")
        }
    }
}
