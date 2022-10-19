package by.derovi.botp2p.dialogs

import by.derovi.botp2p.BotUser

interface Dialog {
    fun start(user: BotUser, args: List<String>)
    fun update(user: BotUser): Boolean // true - keep dialog
}