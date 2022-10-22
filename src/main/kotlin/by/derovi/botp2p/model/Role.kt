package by.derovi.botp2p.model

enum class Role(val readableName: String, val description: String) {
    UNSUBSCRIBED("Без подписки", ""),
    STANDARD("\uD83D\uDC68\u200D\uD83D\uDCBC Стандарт", """
       📈 Поиск связок между биржами и разными токенами
       💱 Поддержка Binance, Bitzlato, Bybit, OKX, Huobi, Kucoin
       💳 Выбор любых существующих платежных систем РФ
       🪙 Выбор любых криптовалют
       🔔 Функция уведомлений о возникновении актуальных связок типа "Тейкер-Тейкер"
       👥 Выбор режима торговли (Тейкер-Тейкер, Тейкер-Мейкер, Мейкер-Мейкер)
       ⚙ Гибкие персональные настройки, которые позволяют настроить абсолютно ВСE 
       🔐 Блокировка недобросовестных торговцев
    """.trimIndent()),
    ADVANCED("\uD83D\uDC68\u200D\uD83D\uDCBB Продвинутый", """
        ➕ Все из тарифа СТАНДАРТ
        🥇 Доступ к режиму "Лучшие Цены" - показывает топ офферов на покупку и продажу по всему рынку и позволяет создавать связки вручную
    """.trimIndent()),
    ADMIN("Админ", "");

    val isTariff: Boolean
        get() = this != UNSUBSCRIBED && this != ADMIN

    fun hasPermission(role: Role) = role.ordinal >= this.ordinal
}
