package com.cp

import com.cp.flows.IssueCommercialPaperFlow
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import java.math.BigDecimal
import java.util.*

/**
 * Connects to a Corda node via RPC and performs RPC operations on the node.
 *
 * The RPC connection is configured using command line arguments.
 */
fun main(args: Array<String>) = IssueCommercialPaper().main(args)

private class IssueCommercialPaper {
    companion object {
        val logger = loggerFor<IssueCommercialPaper>()
    }

    fun main(args: Array<String>) {
        // Create an RPC connection to the node.
        val nodeAddress = "3.83.214.30"
        val port = 10006
        val rpcUsername = "issuer"
        val rpcPassword = "issuer"
        val client = CordaRPCClient(NetworkHostAndPort(nodeAddress, port))
        val rpcOps = client.start(rpcUsername, rpcPassword).proxy

        val identifier = UUID.fromString("7935fc71-431f-41d5-ac61-d9126ed0650f")
                ?: throw Exception("Couldn't generate UUID from String")
        logger.debug("Identifier: {}", identifier)

        val investor = rpcOps.wellKnownPartyFromX500Name(CordaX500Name("Investor", "New York", "US"))
                ?: throw Exception("Investor information not available")
        logger.debug("Investor: {}", investor)

        logger.info("Issuing commercial paper..")
        val signedTransaction = rpcOps.startFlow(
                ::IssueCommercialPaperFlow,
                Amount.fromDecimal(BigDecimal(500), Currency.getInstance(Locale.US)),
                identifier,
                investor).returnValue.getOrThrow()

        logger.info("Commercial Paper Issued with Identifier: {}", signedTransaction.tx.outputs.single())

        logger.info("Flow completed successfully")


    }
}