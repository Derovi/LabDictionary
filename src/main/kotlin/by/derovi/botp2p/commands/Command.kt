package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.model.Role

interface Command {
    val name: String
    val role: Role
    fun use(user: BotUser, vararg args: String)
    fun removeAffect(user: BotUser) {}
}
