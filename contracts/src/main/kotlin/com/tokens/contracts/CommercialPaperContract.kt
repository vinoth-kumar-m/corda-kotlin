package com.tokens.contracts

import com.tokens.states.CommercialPaper
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class CommercialPaperContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.tokens.contracts.CommercialPaperContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val groups = tx.groupStates(CommercialPaper::withoutOwner)
        val command= tx.commands.requireSingleCommand<CommercialPaperContract.Commands>()
        val timeWindow = tx.timeWindow

        for((inputs, outputs, _) in groups)
        when(command.value) {
            is Commands.Issue -> {
                val output = outputs.single()
                val time = timeWindow?.fromTime ?: throw IllegalArgumentException("Issuance must be timestamped")
                requireThat {
                    "There should not be any inputs" using (inputs.isEmpty())
                    "Maturity date should be in future" using(output.maturityDate > time)
                    "FaceValue should be greater than Zero" using(output.faceValue.quantity > 0)
                }
            }
        }
    }






















    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue: TypeOnlyCommandData(), Commands
        class Move: TypeOnlyCommandData(), Commands
        class Redeem: TypeOnlyCommandData(), Commands
    }
}