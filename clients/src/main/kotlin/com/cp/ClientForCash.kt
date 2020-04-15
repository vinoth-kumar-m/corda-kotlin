package com.cp

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.loggerFor
import net.corda.finance.flows.CashIssueFlow
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
        require(args.size == 4) { "Usage: Client <node address> <port> <rpc username> <rpc password>" }
        val nodeAddress = "3.83.214.30"
        val port = 10009
        val rpcUsername = "investor"
        val rpcPassword = "investor"
        val client = CordaRPCClient(NetworkHostAndPort(nodeAddress, port))
        val rpcOps = client.start(rpcUsername, rpcPassword).proxy

        val identifier = UUID.fromString("d1f26bd5-698f-4532-a6d8-c2ca37e414af")
                ?: throw Exception("Couldn't generate UUID from String")
        logger.debug("Identifier: {}", identifier)

        logger.info("Issuing cash..")
        // rpcOps.startFlow(::CashIssueFlow)


        logger.info("Flow completed successfully")
    }
}