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

        val fromAccount = UUID.fromString("f48a179c-75c8-4ef5-8e97-53e6ded398da")
                ?: throw Exception("Couldn't generate UUID from String")

        val toAccount = UUID.fromString("543e4994-f612-4c8f-a5bd-0ab2c39da077")
                ?: throw Exception("Couldn't generate UUID from String")

        logger.debug("From Account: {}, To Account: {}", fromAccount, toAccount)

        val linearId = UniqueIdentifier(externalId = null, id = UUID.fromString("aad747b9-89f7-49fc-a41a-6d895e7548b5"))
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