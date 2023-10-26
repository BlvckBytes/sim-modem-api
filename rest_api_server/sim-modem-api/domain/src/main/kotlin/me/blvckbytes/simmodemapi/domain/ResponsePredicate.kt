package me.blvckbytes.simmodemapi.domain

fun interface ResponsePredicate {

  fun apply(response: String): Boolean

}