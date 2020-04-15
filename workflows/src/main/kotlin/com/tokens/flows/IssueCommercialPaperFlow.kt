package com.tokens.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestAccountInfo
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.tokens.contracts.CommercialPaperContract
import com.tokens.states.CommercialPaper
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.TimeWindow
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@InitiatingFlow
@StartableByRPC
class IssueCommercialPaperFlow(
        private val faceValue: Amount<Currency>,
        private val accountIdentifier: UUID,
        private val investor: Party
) : FlowLogic<Unit>() {

    companion object {
        object RETRIEVE_ACCOUNT_INFO: Step("Retrieving Account information from local node")
        object IDENTIFYING_NOTARY: Step("Identifying notary service for the flow")
        object TX_BUILDING : Step("Building a transaction.")
        object TX_SIGNING : Step("Signing a transaction.")
        object INITIATING_INVESTOR_FLOW: Step("Initiating Investor flow")
        object FINALISATION : Step("Finalising a transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
                RETRIEVE_ACCOUNT_INFO,
                IDENTIFYING_NOTARY,
                TX_BUILDING,
                TX_SIGNING,
                INITIATING_INVESTOR_FLOW,
                FINALISATION
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call() {

        progressTracker.currentStep= RETRIEVE_ACCOUNT_INFO
        val accountInfo = accountService.accountInfo(accountIdentifier)?.state?.data
                ?: subFlow(RequestAccountInfo(accountIdentifier, investor))
                ?: throw FlowException("Couldn't find account information for $accountIdentifier")

        val owner = subFlow(RequestKeyForAccount(accountInfo))

        progressTracker.currentStep = IDENTIFYING_NOTARY
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        val commercialPaper = CommercialPaper(
                ourIdentity,
                owner,
                faceValue,
                maturityDate()
        )
        val command = Command(CommercialPaperContract.Commands.Issue(), listOf(ourIdentity.owningKey))

        progressTracker.currentStep = TX_BUILDING
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(commercialPaper)
                .addCommand(command)
                .setTimeWindow(TimeWindow.fromOnly(Instant.now()))

        progressTracker.currentStep = TX_SIGNING
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        val investorSession = initiateFlow(investor)

        progressTracker.currentStep = FINALISATION
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

