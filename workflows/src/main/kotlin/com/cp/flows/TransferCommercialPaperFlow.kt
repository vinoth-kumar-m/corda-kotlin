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
        object RETRIEVE_COMMERCIAL_PAPER : Step("Retrieving Commercial Paper using Linear ID")
        object RETRIEVE_ACCOUNT_INFO : Step("Retrieving Account information from local node")
        object IDENTIFYING_NOTARY : Step("Identifying notary service for the flow")
        object TX_BUILDING : Step("Building a transaction.")
        object TX_SIGNING : Step("Signing a transaction.")
        object COLLECTING_SIGNATURES : Step("Collecting signatures from other parties")
        object TX_FINALIZE : Step("Finalising a transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
                RETRIEVE_COMMERCIAL_PAPER,
                RETRIEVE_ACCOUNT_INFO,
                IDENTIFYING_NOTARY,
                TX_BUILDING,
                TX_SIGNING,
                COLLECTING_SIGNATURES,
                TX_FINALIZE
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {

        progressTracker.currentStep = RETRIEVE_COMMERCIAL_PAPER
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(commercialPaperIdentifier))
        val commercialPaperStateAndRef = serviceHub.vaultService.queryBy<CommercialPaper>(queryCriteria).states.singleOrNull()
                ?: throw IllegalArgumentException("Commercial Paper does not exists for $commercialPaperIdentifier")

        val inputState = commercialPaperStateAndRef.state.data

        progressTracker.currentStep = RETRIEVE_ACCOUNT_INFO
        val fromAccount = accountService.accountInfo(fromIdentifier)?.state?.data
                ?: throw FlowException("Couldn't find account information for $fromIdentifier")

        val accountIdentifier: UUID = accountService.accountIdForKey(inputState.owner.owningKey)
                ?: throw FlowException("Couldn't find account information available in the Commercial Paper")

        logger.info("Account Identifier: {}, State: {}", fromAccount.linearId.id, accountIdentifier)
        "Commercial Paper transfer can only be initiated by Account" using (accountIdentifier.compareTo(fromAccount.linearId.id) == 0)

        logger.info("Account's Hosting Node: {}, Our Identity: {}", inputState.investor.name, ourIdentity.name)
        "Commercial Paper transfer can only be initiated by Account's hosting node" using (ourIdentity.name == inputState.investor.name)

        val toAccount = accountService.accountInfo(toIdentifier)?.state?.data
                ?: subFlow(RequestAccountInfo(toIdentifier, investor))
                ?: throw FlowException("Couldn't find account information for $toIdentifier")

        val newAccount = subFlow(RequestKeyForAccount(toAccount))

        progressTracker.currentStep = IDENTIFYING_NOTARY
        logger.info("Identifying notary service...")
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        progressTracker.currentStep = TX_BUILDING
        logger.info("Building transaction")
        val builder = TransactionBuilder(notary = notary)
        val outputState = inputState.withNewOwner(newOwner = newAccount, newInvestor = investor)
        val command = Command(CommercialPaperContract.Commands.Transfer(),
                listOf(inputState.issuer.owningKey, inputState.investor.owningKey))

        builder.withItems(commercialPaperStateAndRef,
                StateAndContract(outputState, CommercialPaperContract.ID),
                command)

        progressTracker.currentStep = TX_SIGNING
        logger.info("Signing Transaction")
        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        progressTracker.currentStep = COLLECTING_SIGNATURES
        logger.info("Collecting signatures from other parties")
        val issuerSession = initiateFlow(inputState.issuer)
        logger.info("Issuer Session: {}", issuerSession)

        val investorSession = initiateFlow(inputState.investor)
        logger.info("Investor Session: {}", investorSession)

        val sessions = mutableSetOf(issuerSession, investorSession)
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))

        progressTracker.currentStep = TX_FINALIZE
        logger.info("Finalizing flow")
        return subFlow(FinalityFlow(stx, sessions))
    }
}

// Commercial Paper Responder Flow
@InitiatedBy(TransferCommercialPaperFlow::class)
class TransferCommercialPaperResponderFlow(private val issuerSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val signedTransaction = object : SignTransactionFlow(issuerSession) {
            override fun checkTransaction(stx: SignedTransaction) {
                val outputState = stx.tx.outputs.single().data
                "This must be a Commercial Paper Transaction" using (outputState is CommercialPaper)
            }
        }

        return subFlow(ReceiveFinalityFlow(issuerSession))
    }
}

