package com.tokens.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.getOrThrow

@StartableByRPC
@StartableByService
@InitiatingFlow
class CreateAccount(private val acctName: String): FlowLogic<String>() {

    companion object {
        object CREATE_NEW_ACCOUNT: ProgressTracker.Step("Creating new account")
    }

    override val progressTracker = ProgressTracker(CREATE_NEW_ACCOUNT)

    @Suspendable
    override fun call(): String {
        // Create New Account
        progressTracker.currentStep = CREATE_NEW_ACCOUNT
        val account = accountService.createAccount(name = acctName).toCompletableFuture().getOrThrow()
        val (name, identifier) = account.state.data
        return "Account: $name is created with UUID: $identifier"
    }

}