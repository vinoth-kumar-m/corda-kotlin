package com.paper.contracts

import com.paper.states.CommercialPaper
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class CommercialPaperContractTests {
    private val issuerCorp = TestIdentity(CordaX500Name("IssuerCorp", "New York", "US"))
    private val buyerCorp = TestIdentity(CordaX500Name("BuyerCorp", "New York", "US"))
    private val ledgerServices = MockServices(listOf("net.corda.finance.schemas"), issuerCorp, buyerCorp)

    @Test
    fun `Empty Ledger`() {

        ledgerServices.ledger {

        }
    }

    @Test
    fun simpleCPDoesntCompile() {
        val inState = CommercialPaper()
        ledger {
            transaction {
                input(CommercialPaperContract.ID) { inState }
            }
        }
            }

}