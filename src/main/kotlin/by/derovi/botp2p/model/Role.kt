package by.derovi.botp2p.model

enum class Role(val readableName: String, val description: String) {
    UNSUBSCRIBED("Без подписки", ""),
    STANDARD("Стандарт", """
        - Поиск связок на Binance, Bitzlato, Bybit, Huobi, OKX, Kucoin
        - Поиск межбиржевых связок
        - Поиск связок со спотом (разные токены покупки и продажи)
        - Режимы Тейкер-Тейкер, Тейкер-Мейкер, Мейкер-Мейкер
        - Поддержка всех российских банков и всех токенов
        - Гибкие индивидуальные настройки
        - Уведомления
    """.trimIndent()),
    ADVANCED("Продвинутый", """
        - Все из тарифа СТАНДАРТ
        - Доступ к режиму Лучшая цена
    """.trimIndent()),
    ADMIN("Админ", "");

    val isTariff: Boolean
        get() = this != UNSUBSCRIBED && this != ADMIN

    fun hasPermission(role: Role) = role.ordinal >= this.ordinal
}
