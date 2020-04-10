package com.tokens.states

import com.tokens.contracts.CommercialPaperContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.OwnableState
import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import java.time.Instant
import java.util.*

@BelongsToContract(CommercialPaperContract::class)
data class CommercialPaper(val issuer: Party,
                           override val owner: AbstractParty,
                           val faceValue: Amount<Currency>,
                           val maturityDate: Instant) : OwnableState {
    override val participants: List<AbstractParty> get() = listOf(issuer, owner)

    fun withoutOwner() = copy(owner = AnonymousParty(owningKey = NullKeys.NullPublicKey))

    override fun withNewOwner(newOwner: AbstractParty) = CommandAndState(CommercialPaperContract.Commands.Move(), copy(owner = newOwner))
}