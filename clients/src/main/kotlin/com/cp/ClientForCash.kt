package com.cp

import com.cash.flows.IssueCashFlow
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.Amount
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.loggerFor
import net.corda.finance.workflows.getCashBalances
import java.math.BigDecimal
import java.util.*

/**
 * Connects to a Corda node via RPC and performs RPC operations on the node.
 *
 * The RPC connection is configured using command line arguments.
 */
fun main(args: Array<String>) = ClientForCash().main(args)

private class ClientForCash {
    companion object {
        val logger = loggerFor<ClientForCash>()
    }

    fun main(args: Array<String>) {
        // Create an RPC connection to the node.
        val nodeAddress = "3.83.214.30"
        val port = 10009
        val rpcUsername = "investor"
        val rpcPassword = "investor"
        val client = CordaRPCClient(NetworkHostAndPort(nodeAddress, port))
        val rpcOps = client.start(rpcUsername, rpcPassword).proxy

        val identifier = UUID.fromString("a89a1c68-e7cc-4e1e-8519-81d804d14d71")
                ?: throw Exception("Couldn't generate UUID from String")

        logger.debug("Identifier: {}", identifier)

        logger.info("Issuing cash..")
        rpcOps.startFlow(::IssueCashFlow, Amount.fromDecimal(BigDecimal(500), Currency.getInstance(Locale.US)), identifier)


        logger.info("Flow completed successfully")

        logger.info(rpcOps.getCashBalances().size.toString())
    }
}