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

        val fromAccount = UUID.fromString("f7a7ec90-3ae0-4942-b9bb-8535513cf388")
                ?: throw Exception("Couldn't generate UUID from String")

        val toAccount = UUID.fromString("134c06f6-c0d4-4390-af2a-f8b7f6643e1b")
                ?: throw Exception("Couldn't generate UUID from String")

        logger.debug("From Account: {}, To Account: {}", fromAccount, toAccount)

        val linearId = UniqueIdentifier(externalId = null, id = UUID.fromString("bedb0399-2562-4228-ac49-db172f463c45"))
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