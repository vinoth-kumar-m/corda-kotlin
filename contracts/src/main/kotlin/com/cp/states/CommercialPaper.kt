package com.cp.states

import com.cp.contracts.CommercialPaperContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import java.time.Instant
import java.util.*

/**
 * Commercial Paper - State
 * This state is going to be used as shared fact between Nodes
 */
@BelongsToContract(CommercialPaperContract::class)
data class CommercialPaper(val issuer: Party,
                           val owner: AbstractParty,
                           val faceValue: Amount<Currency>,
                           val maturityDate: Instant,
                           val status: String = "Active",
                           override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override val participants: List<AbstractParty> get() = listOf(issuer)

    fun withoutOwner() = copy(owner = AnonymousParty(owningKey = NullKeys.NullPublicKey))

    fun withNewOwner(newOwner: AbstractParty) = copy(owner = newOwner)

}