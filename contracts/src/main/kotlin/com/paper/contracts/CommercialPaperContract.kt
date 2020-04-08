package com.paper.contracts

import com.paper.states.CommercialPaper
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class CommercialPaperContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.paper.contracts.CommercialPaperContract"
    }


    interface Commands: CommandData {
        class Issue: TypeOnlyCommandData(), Commands
        class Move: TypeOnlyCommandData(), Commands
        class Redeem: TypeOnlyCommandData(), Commands
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val groups = tx.groupStates(CommercialPaper::withoutOwner)
        val command = tx.commands.requireSingleCommand<CommercialPaperContract.Commands>()

        println(groups)

        println(command)
    }

}