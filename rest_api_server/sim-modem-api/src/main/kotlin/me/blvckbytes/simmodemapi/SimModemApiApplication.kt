package me.blvckbytes.simmodemapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SimModemApiApplication

fun main(args: Array<String>) {
  runApplication<SimModemApiApplication>(*args)
}
