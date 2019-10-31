package org.rateapi.handler

import org.http4k.contract.ContractRoute
import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.lens.Query
import org.http4k.lens.offsetDateTime
import org.http4k.lens.string
import org.rateapi.dao.RateDao
import org.rateapi.model.RateList
import org.rateapi.model.RateResponse
import org.rateapi.model.RateStatus

/**
 * The contract for the rate API containing the routes for retrieving rate values.
 */
class RateContract(private val rateStoreDao: RateDao) {

  val stringBody = Body.string(TEXT_PLAIN).toLens()

  /**
   * Route definition for retrieving rates from the [RateDao]. The rates are retrieved using
   * the query params `startDateTime` and `endDateTime` within the GET request.
   */
  fun getRate(): ContractRoute {
    /**
     * Query param for the `startDateTime`
     */
    val startDateTimeQuery = Query.offsetDateTime().required(START_DATE_TIME)
    /**
     * Query param for the `endDateTime`
     */
    val endDateTimeQuery = Query.offsetDateTime().required(END_DATE_TIME)

    val body = Body.auto<RateResponse>().toLens()
    /**
     * Spec providing the route and queries to perform on the request.
     */
    val spec = "/rate" meta {
      summary = "Retrieves a rate given a start and end date."
      queries += startDateTimeQuery
      queries += endDateTimeQuery
    } bindContract Method.GET

    /**
     * Retrieves the rate from the [RateDao] based on the provided query params. If the rate returned
     * is null due to it being unavailable, then the status will be set to [RateStatus.UNAVAILABLE]
     * rather than [RateStatus.AVAILABLE]
     */
    val getRate: HttpHandler = { request: Request ->
      val startDateTime = startDateTimeQuery(request)
      val endDateTime = endDateTimeQuery(request)
      val rate = rateStoreDao.getRate(startDateTime, endDateTime)
      val status: RateStatus = rate.let {
        when {
          it != null -> RateStatus.AVAILABLE
          else -> RateStatus.UNAVAILABLE
        }
      }
      Response(OK).with(
          body of RateResponse(rate, status.value, startDateTime.toString(),
              endDateTime.toString()))

    }
    return spec to getRate
  }

  /**
   * Route definition for updating the rates contained within the [RateDao]. This route will
   * parse the JSON body provided in the POST request and update the rates to the rates provided
   * in the JSON body.
   */
  fun updateRates(): ContractRoute {
    val rateList = Body.auto<RateList>().toLens()

    val spec = "/rate" meta {
      summary = "Updates rates given a JSON body containing the new rates."
    } bindContract Method.POST

    val updateRates: HttpHandler = { request: Request ->
      val results = rateStoreDao.updateRates(rateList(request))
      Response(OK).with(stringBody of results.toString())
    }
    return spec to updateRates
  }

  companion object {
    /**
     * Query param key for the start date time for a rate lookup
     */
    const val START_DATE_TIME = "startDateTime"
    /**
     * Query param key for the end date time for a rate lookup
     */
    const val END_DATE_TIME = "endDateTime"
  }
}
