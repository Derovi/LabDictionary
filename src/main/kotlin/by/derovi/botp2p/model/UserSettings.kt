package by.derovi.botp2p.model

import by.derovi.botp2p.exchange.TradingMode
import by.derovi.botp2p.exchange.Currency
import by.derovi.botp2p.exchange.PaymentMethod
import by.derovi.botp2p.exchange.Token
import javax.persistence.*

@Embeddable
data class UserSettings(
    var notificationThreshold: Double?, // 0 - 100 percent
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
    var commonSettings: SearchSettings,
    @OneToOne
    var buySettings: SearchSettings,
    @OneToOne
    var sellSettings: SearchSettings,
    @OneToOne
    val buyTakerSettings: SearchSettings,
    @OneToOne
    val sellTakerSettings: SearchSettings,
    @OneToOne
    val buyMakerSettings: SearchSettings,
    @OneToOne
    val sellMakerSettings: SearchSettings
) {
    fun getSearchSettings(buy: Boolean, taker: Boolean) =
        when (settingsMode) {
            SettingsMode.STANDARD -> commonSettings
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
