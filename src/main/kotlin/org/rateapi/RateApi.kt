package org.rateapi

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.HandleUpstreamRequestFailed
import org.http4k.filter.MetricFilters
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson
import org.http4k.routing.RoutingHttpHandler
import org.rateapi.handler.MetricsContract
import org.rateapi.handler.RateContract

/**
 * Top level API definition with the necessary filters defined and routes
 * set for the different server functions.
 */
fun RateApi(rateStoreContract: RateContract): HttpHandler {
  val registry = SimpleMeterRegistry()
  return DebuggingFilters.PrintRequestAndResponse()
      .then(ServerFilters.HandleUpstreamRequestFailed())
      .then(ServerFilters.RequestTracing())
      .then(MetricFilters.Server.RequestCounter(registry))
      .then(MetricFilters.Server.RequestTimer(registry))
      .then(Api(rateStoreContract, MetricsContract(registry)))
}

/**
 * Version 1 (v1) of the routes for the API
 */
fun Api(rateStoreContract: RateContract, metricsContract: MetricsContract): RoutingHttpHandler =
    contract {
      renderer = OpenApi3(ApiInfo("Rate API", "v1.0",
          "This API stores and provides rates for given time ranges."), Jackson)
      descriptionPath = "/api/swagger.json"
      routes += rateStoreContract.getRate()
      routes += rateStoreContract.updateRates()
      routes += metricsContract.getMetrics()
    }
