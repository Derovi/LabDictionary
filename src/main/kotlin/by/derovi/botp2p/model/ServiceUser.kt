package by.derovi.botp2p.model

import javax.persistence.*

@Entity
class ServiceUser(
   @Id
   val userId: Long,
   @Embedded
   var userSettings: UserSettings,
   @Enumerated(EnumType.STRING)
   var role: Role,
   var subscribedUntil: Long,
   var lastDemoUsed: Long,
   var banned: Boolean,
   var login: String?,
   var chatId: Long,
   var referBonus: Long,
   var referNumber: Long,
   @OneToOne(cascade = [CascadeType.ALL])
   var referPromo: Promo?,
   @OneToOne
   var promo: Promo?
)
