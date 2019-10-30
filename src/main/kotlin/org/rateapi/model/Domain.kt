package org.rateapi.model

/**
 * A list of [RawRate] values.
 */
data class RateList(val rates: List<RawRate>)

/**
 * A data class encapsulating the values for each rate value in the rate json file source.
 */
data class RawRate(val days: String, val times: String, val tz: String, val price: Int)

/**
 * A data class encapsulating the results of a rate query.
 */
data class RateResponse(val price: Int?, val status: String, val startDateTime: String,
                        val endDateTime: String)

/**
 * A enum representing the status of a given rate to be used in the [RateResponse]
 */
enum class RateStatus(val value: String) {
  AVAILABLE("available"),
  UNAVAILABLE("unavailable")
}
