package com.tokens

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateAndRef
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor

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
        require(args.size == 3) { "Usage: Client <node address> <rpc username> <rpc password>" }
        val nodeAddress = parse(args[0])
        val rpcUsername = args[1]
        val rpcPassword = args[2]
        val client = CordaRPCClient(nodeAddress)
        val rpcOps = client.start(rpcUsername, rpcPassword).proxy

        val account: StateAndRef<AccountInfo> = rpcOps.startFlow(::CreateAccount, "vinoth-kumar-m").returnValue.getOrThrow()

        logger.info("Account created successfully.")
    }
}