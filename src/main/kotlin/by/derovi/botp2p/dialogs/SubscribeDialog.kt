package by.derovi.botp2p.dialogs

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.model.CurrencyAndPaymentMethod
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.model.UserRepository
import by.derovi.botp2p.services.CommandService
import by.derovi.botp2p.services.SubscriptionService
import by.derovi.botp2p.services.UpdateService
import by.derovi.botp2p.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
@Scope("prototype")
class SubscribeDialog(var state: State = State.USERNAME) : Dialog {

    @Autowired
    lateinit var commandService: CommandService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var subscribtionService: SubscriptionService

    @Autowired
    lateinit var updateService: UpdateService

    @Autowired
    lateinit var userService: UserService

    enum class State {
        USERNAME, ROLE, DAYS
    }

    var userId: Long? = null
    lateinit var role: Role
    override fun start(user: BotUser, args: List<String>) {}

    override fun update(user: BotUser): Boolean {
       if (state == State.USERNAME) {
           val id = user.message.toLongOrNull()
           if (id == null) {
               user.sendMessage("<b>\"${user.message}\" не является числом!</b>")
               commandService.back(user)
               return false
           } else if (!userRepository.existsById(id)) {
               user.sendMessage("<b>Пользователь с id <i>$id</i> не найден!</b>")
               return false
           } else {
               userId = id
               state = State.ROLE
               user.sendMessage(
                   "<b>Укажите роль</b>",
                   InlineKeyboardMarkup.builder().keyboardRow(
                       Role.values().map {
                           InlineKeyboardButton.builder().text(it.readableName).callbackData(it.name).build()
                       }
                   ).build()
               )
               return true
           }
       } else if (state == State.ROLE) {
           val role = Role.values().find { it.name.equals(user.message, ignoreCase = true) }
           if (role == null) {
               user.sendMessage("<b>Роль \"${user.message}\" не существует!</b>")
               return false
           } else {
               if (role == Role.UNSUBSCRIBED || role == Role.ADMIN) {
                   user.sendMessage("<b>Для пользователя</b> <i>${userId!!}</i> <b>установлена роль</b> <i>${role.readableName}</i>")
                   subscribtionService.subscribe(userId!!, role, 1)
                   return false
               }
               state = State.DAYS
               this.role = role
               user.sendMessage(
                   "<b>Введите количество дней</b>",
                   InlineKeyboardMarkup.builder().keyboardRow(mutableListOf(
                       InlineKeyboardButton.builder().text("7").callbackData("7").build(),
                       InlineKeyboardButton.builder().text("30").callbackData("30").build(),
                       InlineKeyboardButton.builder().text("180").callbackData("180").build(),
                       InlineKeyboardButton.builder().text("365").callbackData("365").build(),
                   )).build()
               )
               return true
           }
       } else {
           val days = user.message.toIntOrNull()
           if (days == null) {
               user.sendMessage("<b>\"${user.message}\" не является числом!</b>")
               updateService.useCommandWithArguments("/user#${userId}", user)
               return false
           } else {
               val botUser = userService.getBotUserById(userId!!)
               user.sendMessage("<b>Пользователь @${botUser.loginOrId} подписан на</b> <i>${role.readableName}</i> <b>на $days дней!</b>")
               subscribtionService.subscribe(userId!!, role, days)
//               updateService.useCommandWithArguments("/user#${userId}", user)
               botUser.sendMessage(when(role) {
                   Role.ADMIN -> "<b>Держи админку щегол!</b>"
                   Role.UNSUBSCRIBED -> "<b>Ваша подписка отменена!</b>"
                   else -> "<b>Подписка</b> <i>${role.readableName}</i> <b>оплачена на $days дней!</b>"
               })
               return true
           }
       }
    }
}
