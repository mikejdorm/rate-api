package org.rateapi

import org.http4k.client.OkHttp
import org.http4k.cloudnative.env.Environment
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.format.Jackson
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.rateapi.handler.RateContract.Companion.END_DATE_TIME
import org.rateapi.handler.RateContract.Companion.START_DATE_TIME

@ExtendWith(JsonApprovalTest::class)
class RateServerTest {
  val client = OkHttp()
  val environment = Environment.fromResource("test.properties")
  val json = Jackson
  val server = RateApiServer(environment)

  @BeforeEach
  fun setup() {
    server.start()
  }

  @AfterEach
  fun teardown() {
    server.stop()
  }

  @Test
  fun `1750`(approver: Approver) {
    val response = client(Request(Method.GET, "http://localhost:${server.port()}/rate")
        .query(START_DATE_TIME, "2015-07-01T07:00:00-05:00").query(END_DATE_TIME,
            "2015-07-01T12:00:00-05:00"))
    approver.assertApproved(response)
  }

  @Test
  fun `2000`(approver: Approver) {
    approver.assertApproved(client(Request(Method.GET, "http://localhost:${server.port()}/rate")
        .query(START_DATE_TIME, "2015-07-04T15:00:00+00:00").query(END_DATE_TIME,
            "2015-07-04T20:00:00+00:00")))
  }

  @Test
  fun `unavailable`(approver: Approver) {
    approver.assertApproved(client(Request(Method.GET, "http://localhost:${server.port()}/rate")
        .query(START_DATE_TIME, "2015-07-04T07:00:00+05:00").query(END_DATE_TIME,
            "2015-07-04T20:00:00+05:00")))
  }
}
