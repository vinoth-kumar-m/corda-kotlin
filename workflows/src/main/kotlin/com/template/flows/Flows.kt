package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.IOUContract
import com.template.states.IOUState
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class IOUFlow(val iouValue: Int, val otherParty: Party) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        // Initiator flow logic goes here.
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val outputState = IOUState(iouValue, ourIdentity, otherParty)
        val command = Command(IOUContract.Create(), listOf(ourIdentity.owningKey, otherParty.owningKey))

        val txBuilder = TransactionBuilder(notary = notary)
                .addOutputState(outputState, IOUContract.ID)
                .addCommand(command)

        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        val otherPartySession = initiateFlow(otherParty)

        val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, listOf(otherPartySession), CollectSignaturesFlow.tracker()))

        subFlow(FinalityFlow(fullySignedTx, otherPartySession))
    }
}

@InitiatedBy(IOUFlow::class)
class IOUFlowResponder(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
        val signTransactionFlow = object: SignTransactionFlow(counterPartySession) {
            override fun checkTransaction(stx: SignedTransaction) {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction." using (output is IOUState)
                val iou = output as IOUState
                "The IOU's value can't be too high." using (iou.value < 1000)
            }

        }
        val expectedTxId = subFlow(signTransactionFlow).id
        subFlow(ReceiveFinalityFlow(counterPartySession, expectedTxId))
    }
}
