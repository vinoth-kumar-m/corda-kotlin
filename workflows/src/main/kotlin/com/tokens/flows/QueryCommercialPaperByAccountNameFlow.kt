package com.tokens.flows

import com.r3.corda.lib.accounts.workflows.accountService
import com.tokens.states.CommercialPaper
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.*
import org.bouncycastle.asn1.x500.style.RFC4519Style.name
import java.lang.IllegalArgumentException

@StartableByRPC
@StartableByService
@InitiatingFlow
class QueryCommercialPaperByAccountName(
       private val name: String
): FlowLogic<List<CommercialPaper>>() {

    companion object {
        object RETRIEVING_ACCOUNT_INFO: Step("Retrieving account information for $name")
        object CONSTRUCT_QUERY_CRITERIA: Step("Constructing query criteria")
        object FETCHING_COMMERCIAL_PAPERS: Step("Fetching commercial papers matches the criteria")

        fun tracker() = ProgressTracker(
                RETRIEVING_ACCOUNT_INFO,
                CONSTRUCT_QUERY_CRITERIA,
                FETCHING_COMMERCIAL_PAPERS
        )
    }

    override val progressTracker = tracker()

    override fun call(): List<CommercialPaper> {

        progressTracker.currentStep = RETRIEVING_ACCOUNT_INFO
        val accountInfo = accountService.accountInfo(name).singleOrNull()?.state?.data ?: throw IllegalArgumentException("Account does not exists")

        progressTracker.currentStep = CONSTRUCT_QUERY_CRITERIA
        val criteria = QueryCriteria.VaultQueryCriteria(
                externalIds = listOf(accountInfo.identifier.id)
        )

        progressTracker.currentStep = FETCHING_COMMERCIAL_PAPERS
        return serviceHub.vaultService.queryBy(
                contractStateType = CommercialPaper::class.java,
                criteria = criteria
        ).states.map {
            it.state.data
        }
    }

}