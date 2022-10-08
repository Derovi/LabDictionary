package by.derovi.botp2p.model

import javax.persistence.Embeddable

@Embeddable
data class Maker(
    var name: String,
    var exchange: String
)