package com.cp.webserver

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfo
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Define your API endpoints here.
 */
@Service
class Service(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(Service::class.java)
    }

    private val rpcOps = rpc.proxy

    fun createAccount(name: String): AccountInfo {
        val accountInfo = rpcOps.startFlow(::CreateAccount, name).returnValue.getOrThrow()
        val issuer = rpcOps.wellKnownPartyFromX500Name(CordaX500Name("Issuer", "London", "GB"))
                ?: throw Exception("Issuer information not available")
        rpcOps.startFlow(::ShareAccountInfo, accountInfo, listOf(issuer))
        return accountInfo.state.data
    }
}