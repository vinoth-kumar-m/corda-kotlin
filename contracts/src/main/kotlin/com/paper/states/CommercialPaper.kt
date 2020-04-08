package com.paper.states

import com.paper.contracts.CommercialPaperContract
import net.corda.core.contracts.*
import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import java.time.Instant
import java.util.*

// *********
// * State *
// *********
@BelongsToContract(CommercialPaperContract::class)
data class CommercialPaper(val issuance: PartyAndReference,
                           override val owner: AbstractParty,
                           val faceValue: Amount<Issued<Currency>>,
                           val maturityDate: Instant
): OwnableState {
    override val participants = listOf(owner)

    fun withoutOwner() = copy(owner = AnonymousParty(NullKeys.NullPublicKey))

    override fun withNewOwner(newOwner: AbstractParty): CommandAndState = CommandAndState(CommercialPaperContract.Commands.Move(), copy(owner = newOwner))
}

