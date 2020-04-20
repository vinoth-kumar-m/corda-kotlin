package com.cp

import com.cp.flows.RedeemCommercialPaperFlow
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import java.util.*

/**
 * Connects to a Corda node via RPC and performs RPC operations on the node.
 *
 * The RPC connection is configured using command line arguments.
 */
fun main(args: Array<String>) = RedeemCommercialPaper().main(args)

private class RedeemCommercialPaper {
    companion object {
        val logger = loggerFor<RedeemCommercialPaper>()
    }

    fun main(args: Array<String>) {
        // Create an RPC connection to the node.
        val nodeAddress = "3.83.214.30"
        val port = 10009
        val rpcUsername = "investor"
        val rpcPassword = "investor"
        val client = CordaRPCClient(NetworkHostAndPort(nodeAddress, port))
        val rpcOps = client.start(rpcUsername, rpcPassword).proxy

        val fromAccount = UUID.fromString("f8d9a0d8-78ee-4d30-816a-cd218b8ad2c2")
                ?: throw Exception("Couldn't generate UUID from String")

        logger.debug("From Account: {}", fromAccount)

        val linearId = UniqueIdentifier(externalId = null, id = UUID.fromString("13c6a154-bc76-4d3e-8570-672c9196e3eb"))
        logger.info("Redeem Commercial Paper: {}", linearId)

        val investor = rpcOps.wellKnownPartyFromX500Name(CordaX500Name("Investor", "New York", "US"))
                ?: throw Exception("Investor information not available")
        logger.debug("Investor: {}", investor)

        val signedTransaction = rpcOps.startFlow(
                ::RedeemCommercialPaperFlow,
                linearId,
                fromAccount).returnValue.getOrThrow()

        logger.info("Commercial Paper redeemed with Identifier: {}", signedTransaction.tx.outputs.single())

        logger.info("Flow completed successfully")

    }
}