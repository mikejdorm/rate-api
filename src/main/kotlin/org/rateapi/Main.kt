package org.rateapi

import org.http4k.cloudnative.env.Environment

/**
 * Main entry point of the application. This will create the server and then start it up.
 */
fun main() {
  RateApiServer(Environment.fromResource("prod.properties")).start()
}
