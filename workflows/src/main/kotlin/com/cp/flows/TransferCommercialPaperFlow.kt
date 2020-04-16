package com.cp.flows

import co.paralleluniverse.fibers.Suspendable
import com.cp.contracts.CommercialPaperContract
import com.cp.states.CommercialPaper
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestAccountInfo
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import java.util.*

@InitiatingFlow
@StartableByRPC
class TransferCommercialPaperFlow(
        private val commercialPaperIdentifier: UniqueIdentifier,
        private val fromIdentifier: UUID,
        private val toIdentifier: UUID,
        private val investor: Party
) : FlowLogic<SignedTransaction>() {

    companion object {
        object RETRIEVE_COMMERCIAL_PAPER: Step("Retrieving Commercial Paper using Linear ID")
        object RETRIEVE_ACCOUNT_INFO: Step("Retrieving Account information from local node")
        object IDENTIFYING_NOTARY: Step("Identifying notary service for the flow")
        object TX_BUILDING: Step("Building a transaction.")
        object TX_SIGNING: Step("Signing a transaction.")
        object INITIATING_INVESTOR_FLOW: Step("Initiating Investor flow")
        object TX_FINALIZE: Step("Finalising a transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
                RETRIEVE_COMMERCIAL_PAPER,
                RETRIEVE_ACCOUNT_INFO,
                IDENTIFYING_NOTARY,
                TX_BUILDING,
                TX_SIGNING,
                INITIATING_INVESTOR_FLOW,
                TX_FINALIZE
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {

        progressTracker.currentStep = RETRIEVE_COMMERCIAL_PAPER
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(commercialPaperIdentifier))
        val commercialPaperStateAndRef =  serviceHub.vaultService.queryBy<CommercialPaper>(queryCriteria).states.single()
        val inputState = commercialPaperStateAndRef.state.data

        progressTracker.currentStep= RETRIEVE_ACCOUNT_INFO
        val fromAccount = accountService.accountInfo(fromIdentifier)?.state?.data
                ?: subFlow(RequestAccountInfo(fromIdentifier, investor))
                ?: throw FlowException("Couldn't find account information for $fromIdentifier")

        if(inputState.owner !in fromAccount.participants) throw FlowException("Commercial Paper transfer can only be initiated by Owner")

        logger.debug("Account's Hosting Node: {}, Our Identity: {}", fromAccount.host, ourIdentity)
        if(fromAccount.host != ourIdentity) throw FlowException("Commercial Paper transfer can only be initiated by Account's hosting node")

        val toAccount = accountService.accountInfo(toIdentifier)?.state?.data
                ?: subFlow(RequestAccountInfo(toIdentifier, investor))
                ?: throw FlowException("Couldn't find account information for $toIdentifier")

        val newOwner = subFlow(RequestKeyForAccount(toAccount))

        progressTracker.currentStep = IDENTIFYING_NOTARY
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        progressTracker.currentStep = TX_BUILDING
        val builder = TransactionBuilder(notary = notary)
        val outputState = inputState.withNewOwner(newOwner = newOwner)
        val command = Command(CommercialPaperContract.Commands.Transfer(), listOf(ourIdentity.owningKey, inputState.owner.owningKey))

        builder.withItems(commercialPaperStateAndRef,
                StateAndContract(outputState, CommercialPaperContract.ID),
                command)

        progressTracker.currentStep = TX_SIGNING
        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)
        val sessions = (inputState.participants - ourIdentity + investor).map { initiateFlow(it as Party) }.toSet()
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))

        progressTracker.currentStep = TX_FINALIZE
        return subFlow(FinalityFlow(stx, sessions))
    }
}

// Commercial Paper Responder Flow
@InitiatedBy(TransferCommercialPaperFlow::class)
class TransferCommercialPaperResponderFlow(private val issuerSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val signedTransaction = object: SignTransactionFlow(issuerSession) {
            override fun checkTransaction(stx: SignedTransaction) {
                val outputState = stx.tx.outputs.single().data
                "This must be a Commercial Paper Transaction" using (outputState is CommercialPaper)
            }
        }

        return subFlow(ReceiveFinalityFlow(issuerSession))
    }
}

