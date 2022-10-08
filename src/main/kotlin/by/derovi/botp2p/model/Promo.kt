package by.derovi.botp2p.model

import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Promo(
    @Id
    var id: String,
    var discount: Double,
    var referId: Long?,
    var expirationDate: Long?
)
