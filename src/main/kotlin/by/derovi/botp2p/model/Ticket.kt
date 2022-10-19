package by.derovi.botp2p.model

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.ManyToOne

@Entity
data class Ticket(
    @Id
    @GeneratedValue
    var id: Long,
    @ManyToOne
    var user: ServiceUser,
    var price: Long,
    var promoPrice: Long,
    var role: Role,
    var duration: SubscriptionDuration,
    var address: String,
    var createdAt: Long,
    var approvedByUserAt: Long?
)
