package by.derovi.botp2p.exchange

import by.derovi.botp2p.services.FeesService

class BundleSearchResult(
    var currency: Currency,
    var buyExchange: Exchange,
    var sellExchange: Exchange,
    var transferGuide: FeesService.TransferGuide,
    var buyToken: Token,
    var sellToken: Token,
    var buyOffers: List<Offer>,
    var sellOffers: List<Offer>,
    var spreadsWithFee: List<List<Double>>,
    var bestSpread: Double
) {
    val spreadWithFee: Double
        get() = bestSpread
}
