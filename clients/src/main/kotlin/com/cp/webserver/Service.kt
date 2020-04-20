package com.cp.webserver

import com.cp.flows.IssueCommercialPaperFlow
import com.cp.flows.QueryCommercialPaperByAccountName
import com.cp.flows.RedeemCommercialPaperFlow
import com.cp.flows.TransferCommercialPaperFlow
import com.cp.states.CommercialPaper
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.AllAccounts
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfo
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

/**
 * Define your API endpoints here.
 */
@Service
class Service(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(Service::class.java)
    }

    private val rpcOps = rpc.proxy

    fun createAccount(name: String): AccountInfo {
        val accountStateAndRef = rpcOps.startFlow(::CreateAccount, name).returnValue.getOrThrow()
        val issuer = rpcOps.wellKnownPartyFromX500Name(CordaX500Name("Issuer", "London", "GB"))
                ?: throw Exception("Issuer information not available")
        rpcOps.startFlow(::ShareAccountInfo, accountStateAndRef, listOf(issuer))
        val accountInfo = accountStateAndRef.state.data
        logger.info("Account Info: {}", accountInfo)
        return accountInfo
    }

    fun allAccounts(): List<AccountInfo> {
        val accounts = rpcOps.startFlow(::AllAccounts).returnValue.getOrThrow()
        logger.info("Total Accounts: {}", accounts.size)
        return accounts.map { it -> it.state.data }
    }

    fun issueCommercialPaper(identifier: String, faceValue: Int): CommercialPaper{
        val identifier = UUID.fromString(identifier)
                ?: throw Exception("Couldn't generate UUID from String")
        logger.debug("Identifier: {}", identifier)

        val investor = rpcOps.wellKnownPartyFromX500Name(CordaX500Name("Investor", "New York", "US"))
                ?: throw Exception("Investor information not available")
        logger.debug("Investor: {}", investor)

        logger.info("Issuing commercial paper..")
        val signedTransaction = rpcOps.startFlow(
                ::IssueCommercialPaperFlow,
                Amount.fromDecimal(BigDecimal(faceValue), Currency.getInstance(Locale.US)),
                identifier,
                investor).returnValue.getOrThrow()

        val commercialPaper = signedTransaction.tx.outputs.single().data as CommercialPaper
        logger.info("Commercial Paper Issued with Identifier: {}", commercialPaper)

        return commercialPaper
    }

    fun retrieveCommercialPapersByName(name: String): List<CommercialPaper> {
        logger.info("Retrieving Commercial Papers By AccountName: $name")
        val commercialPapers = rpcOps.startFlow(::QueryCommercialPaperByAccountName, name).returnValue.getOrThrow()
        logger.info("Total Commercial Papers for $name: {}", commercialPapers.size)
        return commercialPapers
    }

    fun transferCommercialPaper(identifier: String, fromAccount: String, toAccount: String): CommercialPaper {
        val fromAccountIdentifier = UUID.fromString(fromAccount)
                ?: throw Exception("Couldn't generate UUID from String")

        val toAccountIdentifier = UUID.fromString(toAccount)
                ?: throw Exception("Couldn't generate UUID from String")

        logger.debug("From Account: {}, To Account: {}", fromAccount, toAccount)

        val linearId = UniqueIdentifier(externalId = null, id = UUID.fromString(identifier))
        logger.info("Transfer Commercial Paper: {}", linearId)

        val investor = rpcOps.wellKnownPartyFromX500Name(CordaX500Name("Investor", "New York", "US"))
                ?: throw Exception("Investor information not available")
        logger.debug("Investor: {}", investor)

        val signedTransaction = rpcOps.startFlow(
                ::TransferCommercialPaperFlow,
                linearId,
                fromAccountIdentifier,
                toAccountIdentifier,
                investor).returnValue.getOrThrow()

        val commercialPaper = signedTransaction.tx.outputStates.single() as CommercialPaper

        logger.info("Commercial Paper transferred with Identifier: {}", commercialPaper)

        return commercialPaper
    }

    fun redeemCommercialPaper(identifier: String, fromAccount: String): CommercialPaper {
        val fromAccountIdentifier = UUID.fromString(fromAccount)
                ?: throw Exception("Couldn't generate UUID from String")

        logger.debug("From Account: {}", fromAccount)

        val linearId = UniqueIdentifier(externalId = null, id = UUID.fromString(identifier))
        logger.info("Redeem Commercial Paper: {}", linearId)

        val signedTransaction = rpcOps.startFlow(
                ::RedeemCommercialPaperFlow,
                linearId,
                fromAccountIdentifier).returnValue.getOrThrow()

        val commercialPaper = signedTransaction.tx.outputStates.single() as CommercialPaper

        logger.info("Commercial Paper redeemed with Identifier: {}", commercialPaper)

        return commercialPaper
    }
}