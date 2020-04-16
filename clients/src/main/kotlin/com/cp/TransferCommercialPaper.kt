package com.cp

import com.cp.flows.TransferCommercialPaperFlow
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.loggerFor
import net.corda.finance.workflows.getCashBalance
import java.util.*

/**
 * Connects to a Corda node via RPC and performs RPC operations on the node.
 *
 * The RPC connection is configured using command line arguments.
 */
fun main(args: Array<String>) = TransferCommercialPaper().main(args)

private class TransferCommercialPaper {
    companion object {
        val logger = loggerFor<TransferCommercialPaper>()
    }

    fun main(args: Array<String>) {
        // Create an RPC connection to the node.
        val nodeAddress = "3.83.214.30"
        val port = 10009
        val rpcUsername = "investor"
        val rpcPassword = "investor"
        val client = CordaRPCClient(NetworkHostAndPort(nodeAddress, port))
        val rpcOps = client.start(rpcUsername, rpcPassword).proxy

        val fromAccount = UUID.fromString("b78bb5df-d1e5-4852-a700-5575abe6c3b0")
                ?: throw Exception("Couldn't generate UUID from String")

        val toAccount = UUID.fromString("3596f691-9ae5-4a2b-a24d-bad08a80b9d5")
                ?: throw Exception("Couldn't generate UUID from String")

        logger.debug("From Account: {}, To Account: {}", fromAccount, toAccount)

        val linearId = UniqueIdentifier(externalId = null, id = UUID.fromString("85e9dc60-c73c-4cce-a19f-d63f8fe745c1"))
        logger.info("Transfer Commercial Paper: {}", linearId)

        val investor = rpcOps.wellKnownPartyFromX500Name(CordaX500Name("Investor", "New York", "US"))
                ?: throw Exception("Investor information not available")
        logger.debug("Investor: {}", investor)

        rpcOps.startFlow(::TransferCommercialPaperFlow, linearId, fromAccount, toAccount, investor)


        logger.info("Flow completed successfully")

    }
}