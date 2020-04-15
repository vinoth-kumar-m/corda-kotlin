package com.cp.webserver

import com.cp.states.CommercialPaper
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
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val rpcOps = rpc.proxy

    @GetMapping(value = ["/issue"], produces = ["text/plain"])
    private fun issue(@RequestParam identifier: String, @RequestParam faceValue: Int): ResponseEntity<CommercialPaper> {
        logger.info("Identified: $identifier, Face Value: $faceValue")
        return ResponseEntity(HttpStatus.OK)
    }
}