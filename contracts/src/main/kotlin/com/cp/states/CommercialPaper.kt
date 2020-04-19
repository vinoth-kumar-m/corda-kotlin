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
data class CommercialPaper(val faceValue: Amount<Currency>,
                           val maturityDate: Instant,
                           val issuer: Party,
                           val owner: AbstractParty,
                           val investor: Party,
                           val status: String = "Active",
                           override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override val participants: List<AbstractParty> get() = listOf(issuer, owner)

    fun withoutOwner() = copy(owner = AnonymousParty(owningKey = NullKeys.NullPublicKey))

    fun withNewOwner(newOwner: AbstractParty, newInvestor: Party) = copy(owner = newOwner, investor = newInvestor)

    fun redeemed() = copy(owner = this.issuer, status = "Redeemed")

    override fun toString(): String {
        return "CommercialPaper(faceValue=$faceValue, maturityDate=$maturityDate, issuer=$issuer, owner=$owner, investor=$investor, status='$status', linearId=$linearId)"
    }


}