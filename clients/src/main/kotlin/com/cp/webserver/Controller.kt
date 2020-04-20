package com.cp.webserver

import com.cp.states.CommercialPaper
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(private val service: Service) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    @GetMapping(value = ["/account"], produces = ["application/json"])
    private fun createAccount(@RequestParam name: String): ResponseEntity<AccountInfo> {
        logger.info("Creating account for $name")
        try {
            val accountInfo = service.createAccount(name)
            logger.info("Account successfully created.")
            return ResponseEntity.ok(accountInfo)
        } catch (ex: Exception) {
            logger.error("Exception occurred while creating account: ${ex.message}")
        }
        return ResponseEntity(null, HttpStatus.BAD_REQUEST)
    }

    @GetMapping(value = ["/accounts"], produces = ["application/json"])
    private fun allAccounts(): ResponseEntity<List<AccountInfo>> {
        logger.info("Retrieving all accounts")
        try {
            val accounts = service.allAccounts()
            return ResponseEntity.ok(accounts)
        } catch (ex: Exception) {
            logger.error("Exception occurred while retrieving all accounts: ${ex.message}")
        }
        return ResponseEntity(null, HttpStatus.BAD_REQUEST)
    }

    @GetMapping(value = ["/issue"], produces = ["application/json"])
    private fun issue(@RequestParam identifier: String, @RequestParam faceValue: Int): ResponseEntity<CommercialPaper> {
        logger.info("Identifier: $identifier, Face Value: $faceValue")
        try {
            val commercialPaper = service.issueCommercialPaper(identifier, faceValue)
            return ResponseEntity.ok(commercialPaper)
        } catch (ex: Exception) {
            logger.error("Exception occurred while issuing commercial paper: ${ex.message}")
        }
        return ResponseEntity(HttpStatus.OK)
    }

    @GetMapping(value = ["/retrieve"], produces = ["application/json"])
    private fun retrieve(@RequestParam name: String): ResponseEntity<List<CommercialPaper>> {
        logger.info("Name: $name")
        try {
            val commercialPapers = service.retrieveCommercialPapersByName(name)
            return ResponseEntity.ok(commercialPapers)
        } catch (ex: Exception) {
            logger.error("Exception occurred while retrieving commercial papers: ${ex.message}")
        }
        return ResponseEntity(HttpStatus.OK)
    }

    @GetMapping(value = ["/transfer"], produces = ["application/json"])
    private fun transfer(@RequestParam identifier: String, @RequestParam fromAccount: String, @RequestParam toAccount: String): ResponseEntity<CommercialPaper> {
        logger.info("Transfer $identifier from $fromAccount to $toAccount")
        try {
            val commercialPaper = service.transferCommercialPaper(identifier, fromAccount, toAccount)
            return ResponseEntity.ok(commercialPaper)
        } catch (ex: Exception) {
            logger.error("Exception occurred while transferring commercial paper: ${ex.message}")
        }
        return ResponseEntity(HttpStatus.OK)
    }

    @GetMapping(value = ["/redeem"], produces = ["application/json"])
    private fun redeem(@RequestParam identifier: String, @RequestParam fromAccount: String): ResponseEntity<CommercialPaper> {
        logger.info("Redeem $identifier from $fromAccount")
        try {
            val commercialPaper = service.redeemCommercialPaper(identifier, fromAccount)
            return ResponseEntity.ok(commercialPaper)
        } catch (ex: Exception) {
            logger.error("Exception occurred while redeeming commercial paper: ${ex.message}")
        }
        return ResponseEntity(HttpStatus.OK)
    }

}