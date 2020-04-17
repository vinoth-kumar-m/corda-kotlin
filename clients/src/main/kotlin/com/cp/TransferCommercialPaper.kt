package com.cp

import com.cp.flows.QueryCommercialPaperByAccountName
import com.cp.flows.TransferCommercialPaperFlow
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

        val fromAccount = UUID.fromString("7fa7b893-ed35-4bc8-a175-e3a71ceab536")
                ?: throw Exception("Couldn't generate UUID from String")

        val toAccount = UUID.fromString("13560ac1-e2c7-4e24-8aa3-a5c2a54a7ff0")
                ?: throw Exception("Couldn't generate UUID from String")

        logger.debug("From Account: {}, To Account: {}", fromAccount, toAccount)

        val linearId = UniqueIdentifier(externalId = null, id = UUID.fromString("bedb0399-2562-4228-ac49-db172f463c45"))
        logger.info("Transfer Commercial Paper: {}", linearId)

        val investor = rpcOps.wellKnownPartyFromX500Name(CordaX500Name("Investor", "New York", "US"))
                ?: throw Exception("Investor information not available")
        logger.debug("Investor: {}", investor)

        rpcOps.startFlow(::TransferCommercialPaperFlow, linearId, fromAccount, toAccount, investor)

        val commercialPapers = rpcOps.startFlow(::QueryCommercialPaperByAccountName, "karthigadevi").returnValue.getOrThrow()
        commercialPapers.forEach {
            println(it)
        }

        logger.info("Flow completed successfully")

    }
}