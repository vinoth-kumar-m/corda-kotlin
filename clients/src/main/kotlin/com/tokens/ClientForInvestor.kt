package com.tokens

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByUUID
import com.r3.corda.lib.accounts.workflows.flows.RequestAccountInfo
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.tokens.flows.IssueCommercialPaperFlow
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.loggerFor
import java.math.BigDecimal
import java.util.*

/**
 * Connects to a Corda node via RPC and performs RPC operations on the node.
 *
 * The RPC connection is configured using command line arguments.
 */
fun main(args: Array<String>) = ClientForInvestor().main(args)

private class ClientForInvestor {
    companion object {
        val logger = loggerFor<ClientForInvestor>()
    }

    fun main(args: Array<String>) {
        // Create an RPC connection to the node.
        require(args.size == 4) { "Usage: Client <node address> <port> <rpc username> <rpc password>" }
        val nodeAddress = "3.83.214.30"
        val port: Int = 10006
        val rpcUsername = "issuer"
        val rpcPassword = "issuer"
        val client = CordaRPCClient(NetworkHostAndPort(nodeAddress, port))
        val rpcOps = client.start(rpcUsername, rpcPassword).proxy

        val identifier = UUID.fromString("6840641e-9c93-443d-88a6-8f349fb159d5")
        logger.debug("Identifier: {}", identifier)

        val investor: Party? = rpcOps.wellKnownPartyFromX500Name(CordaX500Name("Investor", "New York", "US"))
        logger.debug("Investor: {}", investor)

        var accountInfo: AccountInfo? = rpcOps.startFlow(::AccountInfoByUUID, identifier).returnValue.get()?.state?.data
        logger.debug("Account available in Issuer Node: {}", accountInfo)

        if(accountInfo == null && investor != null) {
            accountInfo = rpcOps.startFlow(::RequestAccountInfo, identifier, investor).returnValue.get()
            logger.debug("Account requested from Investor Node: {}", accountInfo)
        }

        if(accountInfo != null && investor != null) {
            val key = rpcOps.startFlow(::RequestKeyForAccount, accountInfo).returnValue.get()
            logger.info("Key requested from Investor Node: {}", key)
            if(key != null) {
                logger.info("Issuing commercial paper..")
                rpcOps.startFlow(::IssueCommercialPaperFlow, Amount.fromDecimal(BigDecimal(100), Currency.getInstance(Locale.US)), key, investor)
            }
        } else {
            throw Exception("Either Account / Investor information not available")
        }


        logger.info("Flow completed successfully")
    }
}