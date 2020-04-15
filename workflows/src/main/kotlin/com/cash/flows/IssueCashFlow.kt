package com.cash.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestAccountInfo
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.issuedBy
import java.util.*

@InitiatingFlow
@StartableByRPC
class IssueCashFlow(private val amount: Amount<Currency>,
                    private val accountIdentifier: UUID) : FlowLogic<Cash.State>() {

    companion object {
        object RETRIEVE_ACCOUNT_INFO: Step("Retrieving Account Information")
        object IDENTIFYING_NOTARY: Step("Identifying notary service for the flow")
        object TX_BUILDING: Step("Building a transaction.")
        object TX_SIGNING: Step("Signing a transaction.")
        object TX_FINALIZE: Step("Finalising a transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
                RETRIEVE_ACCOUNT_INFO,
                IDENTIFYING_NOTARY,
                TX_BUILDING,
                TX_SIGNING,
                TX_FINALIZE
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): Cash.State {

        progressTracker.currentStep = RETRIEVE_ACCOUNT_INFO
        val accountInfo = accountService.accountInfo(accountIdentifier)?.state?.data
                ?: subFlow(RequestAccountInfo(accountIdentifier, ourIdentity))
                ?: throw FlowException("Couldn't find account information for $accountIdentifier")

        val owner = subFlow(RequestKeyForAccount(accountInfo))

        progressTracker.currentStep = IDENTIFYING_NOTARY
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        val builder = TransactionBuilder(notary)
        val issuer = ourIdentity.ref(OpaqueBytes.of("1".toByte()))
        val signers = Cash().generateIssue(builder, amount.issuedBy(issuer), owner, notary)

        progressTracker.currentStep = TX_SIGNING
        val tx = serviceHub.signInitialTransaction(builder, signers)

        progressTracker.currentStep = TX_FINALIZE
        val signedTx = subFlow(FinalityFlow(tx, emptySet<FlowSession>()))

        return signedTx.tx.outputs.single().data as Cash.State
    }

}