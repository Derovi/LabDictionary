package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

fun <T> deferred(block: DefferScope<T>.() -> T): T {
    val scope = DefferScope<T>()
    return scope.block().also { scope.funcs.forEach(::run) }
}

class DefferScope <T> {
    val funcs = mutableListOf<() -> Unit>()

    fun defer(func: () -> Unit) {
        funcs.add(func)
    }
}

fun test(boolean: Boolean): String = deferred {
    defer { println("test 1") }
    defer { println("test 2") }
    if (boolean) {
        return@deferred "a"
    }  else {
        return@deferred "b"
    }
}

//fun main() {
//    test(true)
//}

@Component
class OrderCommand : Command {
    override val name = "/order"
    override val role = Role.UNSUBSCRIBED

    @Autowired
    lateinit var context: ApplicationContext

    override fun use(user: BotUser, vararg args: String): Unit = deferred {
        defer { context.getBean(CommandService::class.java).back(user) }
        val role = Role.values().filter(Role::isTariff).find { it.name == args[0] } ?: run {
            user.sendMessage("Не найден тариф <b>${args[0]}</b>")
            return@deferred
        }
        // abacaba
    }
}