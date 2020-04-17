package com.cp

import com.cp.flows.TransferCommercialPaperFlow
import com.cp.states.CommercialPaper
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.loggerFor
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

        val fromAccount = UUID.fromString("e0dca312-20b6-476d-bce2-d6be528a2d70")
                ?: throw Exception("Couldn't generate UUID from String")

        val toAccount = UUID.fromString("85d80bc9-0f97-4e1d-843c-b1e2d1f24551")
                ?: throw Exception("Couldn't generate UUID from String")

        logger.debug("From Account: {}, To Account: {}", fromAccount, toAccount)

        val linearId = UniqueIdentifier(externalId = null, id = UUID.fromString("b7eb97fd-d41c-453c-a92e-ac442a085fcb"))
        logger.info("Transfer Commercial Paper: {}", linearId)

        val investor = rpcOps.wellKnownPartyFromX500Name(CordaX500Name("Investor", "New York", "US"))
                ?: throw Exception("Investor information not available")
        logger.debug("Investor: {}", investor)

        rpcOps.startFlow(::TransferCommercialPaperFlow, linearId, fromAccount, toAccount, investor)

        rpcOps.vaultQuery(CommercialPaper::class.java).states.map { it -> it.state.data }.forEach {
            println(it)
        }

        logger.info("Flow completed successfully")

    }
}