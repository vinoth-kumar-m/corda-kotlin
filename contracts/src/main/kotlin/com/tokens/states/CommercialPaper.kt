package com.tokens.states

import com.tokens.contracts.CommercialPaperContract
import net.corda.core.contracts.*
import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import java.time.Instant
import java.util.*

@BelongsToContract(CommercialPaperContract::class)
data class CommercialPaper(val issuer: Party,
                           val owner: AbstractParty,
                           val faceValue: Amount<Currency>,
                           val maturityDate: Instant) : LinearState {

    override val linearId = UniqueIdentifier()

    override val participants: List<AbstractParty> get() = listOf(issuer, owner)

    fun withoutOwner() = copy(owner = AnonymousParty(owningKey = NullKeys.NullPublicKey))

}