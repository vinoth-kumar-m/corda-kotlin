package com.tokens.flows

import co.paralleluniverse.fibers.Suspendable
import com.tokens.contracts.CommercialPaperContract
import com.tokens.states.CommercialPaper
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.TimeWindow
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@InitiatingFlow
@StartableByRPC
class IssueCommercialPaperFlow(
        private val faceValue: Amount<Currency>,
        private val owner: AbstractParty,
        private val investor: Party) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        val commercialPaper = CommercialPaper(
                ourIdentity,
                owner,
                faceValue,
                maturityDate()
        )
        val command = Command(CommercialPaperContract.Commands.Issue(), listOf(ourIdentity.owningKey))

        val txBuilder = TransactionBuilder(notary)
                .addOutputState(commercialPaper)
                .addCommand(command)
                .setTimeWindow(TimeWindow.fromOnly(Instant.now()))

        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        val investorSession = initiateFlow(investor)

        subFlow(FinalityFlow(signedTx, investorSession))
    }

    private fun maturityDate(): Instant = LocalDate.of(2020, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()

}

// Commercial Paper Responder Flow
@InitiatedBy(IssueCommercialPaperFlow::class)
class IssueCommercialPaperResponderFlow(private val issuerSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(issuerSession))
    }
}

