package com.cp.webserver

import com.fasterxml.jackson.databind.ObjectMapper
import net.corda.client.jackson.JacksonSupport
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class Plugin {

    @Bean
    open fun registerModule(): ObjectMapper {
        return JacksonSupport.createNonRpcMapper()
    }
}