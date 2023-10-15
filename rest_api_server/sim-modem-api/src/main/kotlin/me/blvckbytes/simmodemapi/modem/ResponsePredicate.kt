package me.blvckbytes.simmodemapi.modem

fun interface ResponsePredicate {

  fun apply(response: String): Boolean

}