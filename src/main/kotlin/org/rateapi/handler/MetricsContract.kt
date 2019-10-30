package org.rateapi.handler

import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import org.http4k.contract.ContractRoute
import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Jackson.auto


class MetricsContract(private val registry: MeterRegistry) {

  fun getMetrics(): ContractRoute {
    /**
     * JSON lens for the response body
     */
    val body = Body.auto<List<Meter>>().toLens()
    /**
     * Spec providing the route and queries to perform on the request.
     */
    val spec = "/metrics" meta {
      summary = "Retrieves the metrics for the API endpoints."
    } bindContract Method.GET

    /**
     * Retrieves the metrics from the registry and returns them as JSON
     */
    val getRate: HttpHandler = { request: Request ->
      Response(Status.OK).with(body of registry.meters)
    }
    return spec to getRate
  }
}
