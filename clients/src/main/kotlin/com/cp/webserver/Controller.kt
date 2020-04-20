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
        } catch(ex: Exception) {
            logger.error("Exception occurred while creating account: ${ex.message}")
        }
        return ResponseEntity(null, HttpStatus.BAD_REQUEST)
    }

    @GetMapping(value = ["/issue"], produces = ["text/plain"])
    private fun issue(@RequestParam identifier: String, @RequestParam faceValue: Int): ResponseEntity<CommercialPaper> {
        logger.info("Identified: $identifier, Face Value: $faceValue")
        return ResponseEntity(HttpStatus.OK)
    }
}