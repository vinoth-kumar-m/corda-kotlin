package com.cp.contracts

import com.cp.states.CommercialPaper
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class CommercialPaperContract : Contract {

    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.cp.contracts.CommercialPaperContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val groups = tx.groupStates(CommercialPaper::withoutOwner)
        val command= tx.commands.requireSingleCommand<Commands>()
        val timeWindow = tx.timeWindow

        for((inputs, outputs, _) in groups)
        when(command.value) {
            is Commands.Issue -> {
                val outputState = outputs.single()
                val time = timeWindow?.fromTime ?: throw IllegalArgumentException("Issuance must be timestamped")
                requireThat {
                    "No inputs should be consumed while issuing a commercial paper" using (inputs.isEmpty())
                    "Maturity date should be in future" using (outputState.maturityDate > time)
                    "Face value should be greater than zero" using (outputState.faceValue.quantity > 0)
                }
            }
            is Commands.Transfer -> {
                val inputState = inputs.single()
                val outputState = outputs.single()
                val time = timeWindow?.fromTime ?: throw IllegalArgumentException("Transfer must be timestamped")
                requireThat {
                    "Maturity date should be in future" using (outputState.maturityDate > time)
                    "Face value should be greater than zero" using (outputState.faceValue.quantity > 0)
                    "Only 'Active' Commercial Paper can be transferred" using (inputState.status == "Active")
                }
            }
            is Commands.Redeem -> {
                val inputState = inputs.single()
                val outputState = outputs.single()
                val time = timeWindow?.fromTime ?: throw IllegalArgumentException("Redeen must be timestamped")
                requireThat {
                    "Maturity date should be in future" using (outputState.maturityDate < time)
                    "Face value should be greater than zero" using (outputState.faceValue.quantity > 0)
                    "Only 'Active' Commercial Paper can be redeemed" using (inputState.status == "Active")
                    "Commercial Paper status should be 'Redeemed'" using (outputState.status == "Redeemed")
                }
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue: TypeOnlyCommandData(), Commands
        class Transfer: TypeOnlyCommandData(), Commands
        class Redeem: TypeOnlyCommandData(), Commands
    }
}