package org.rateapi

import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.server.Http4kServer
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.rateapi.Settings.PORT_QUERY
import org.rateapi.Settings.RATE_FILENAME_QUERY
import org.rateapi.dao.RateDao
import org.rateapi.handler.RateContract
import org.slf4j.LoggerFactory

val log = LoggerFactory.getLogger(RateDao::class.java)

/**
 * Sets up the server with the routes defined in [RateApi]
 */
fun RateApiServer(env: Environment): Http4kServer {
  log.info("Creating server with port ${PORT_QUERY(env)} and rate file ${RATE_FILENAME_QUERY(env)}")
  val rateStoreContract = RateContract(
      RateDao(RATE_FILENAME_QUERY(env)))
  return RateApi(rateStoreContract).asServer(Undertow(PORT_QUERY(env)))
}

/**
 * Settings for spinning up the server.
 */
object Settings {
  val PORT_QUERY = EnvironmentKey.int().required("port")
  val RATE_FILENAME_QUERY = EnvironmentKey.string().required("rate.filename")
}
