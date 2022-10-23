package by.derovi.botp2p.exchange

enum class TradingMode(val readableName: String, val description: String) {
    TAKER_TAKER("Тейкер-Тейкер", "Покупка и продажа как тейкер"),
    TAKER_MAKER("Тейкер-Мейкер", "Покупка как тейкер, продажа как мейкер"),
    MAKER_TAKER("Мейкер-Тейкер", "Покупка как мейкер, продажа как тейкер"),
    MAKER_MAKER("Мейкер-Мейкер", "Покупка и продажа как мейкер"),
}