package org.rateapi

import io.kotlintest.shouldBe
import org.http4k.client.OkHttp
import org.http4k.cloudnative.env.Environment
import org.http4k.core.Method
import org.http4k.core.Request
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
  private val environment = Environment.fromResource("test.properties")
  private val server = RateApiServer(environment)

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
    val response = client(Request(Method.GET, "http://localhost:${server.port()}/v1/rate")
        .query(START_DATE_TIME, "2015-07-01T07:00:00-05:00").query(END_DATE_TIME,
            "2015-07-01T12:00:00-05:00"))
    approver.assertApproved(response)
  }

  @Test
  fun `2000`(approver: Approver) {
    approver.assertApproved(client(Request(Method.GET, "http://localhost:${server.port()}/v1/rate")
        .query(START_DATE_TIME, "2015-07-04T15:00:00+00:00").query(END_DATE_TIME,
            "2015-07-04T20:00:00+00:00")))
  }

  @Test
  fun `unavailable`(approver: Approver) {
    approver.assertApproved(client(Request(Method.GET, "http://localhost:${server.port()}/v1/rate")
        .query(START_DATE_TIME, "2015-07-04T07:00:00+05:00").query(END_DATE_TIME,
            "2015-07-04T20:00:00+05:00")))
  }

  @Test
  fun `invalidStartDate`(approver: Approver) {
    approver.assertApproved(client(Request(Method.GET, "http://localhost:${server.port()}/v1/rate")
        .query(START_DATE_TIME, "2015-07-40T07:00:00+05:00").query(END_DATE_TIME,
            "2015-07-04T20:00:00+05:00")))
  }

  @Test
  fun `invalidEndDate`(approver: Approver) {
    approver.assertApproved(client(Request(Method.GET, "http://localhost:${server.port()}/v1/rate")
        .query(START_DATE_TIME, "2015-07-04T07:00:00+05:00").query(END_DATE_TIME,
            "2015-07-40T20:00:00+05:00")))
  }

  @Test
  fun invalidDateOrdering() {
    client(Request(Method.GET, "http://localhost:${server.port()}/v1/rate")
        .query(START_DATE_TIME, "2015-07-04T20:00:00+05:00").query(END_DATE_TIME,
            "2015-07-04T07:00:00+05:00")).status.code shouldBe 500
  }

  @Test
  fun `missingEndDate`(approver: Approver) {
    approver.assertApproved(client(Request(Method.GET, "http://localhost:${server.port()}/v1/rate")
        .query(START_DATE_TIME, "2015-07-05T07:00:00+05:00")))
  }

  @Test
  fun `missingStartDate`(approver: Approver) {
    approver.assertApproved(client(Request(Method.GET, "http://localhost:${server.port()}/v1/rate")
        .query(END_DATE_TIME, "2015-07-05T07:00:00+05:00")))
  }

  @Test
  fun `rateUpdate`(approver: Approver) {
    client(Request(Method.POST, "http://localhost:${server.port()}/v1/rate")
        .body("{\n" +
            "    \"rates\": [\n" +
            "        {\n" +
            "            \"days\": \"mon,tues,thurs,fri,sat,sun\",\n" +
            "            \"times\": \"0100-2300\",\n" +
            "            \"tz\": \"America/Chicago\",\n" +
            "            \"price\": 9999\n" +
            "        }]" +
            "}")).status.code shouldBe 200
    approver.assertApproved(client(Request(Method.GET, "http://localhost:${server.port()}/v1/rate")
        .query(START_DATE_TIME, "2015-07-04T15:00:00+00:00").query(END_DATE_TIME,
            "2015-07-04T20:00:00+00:00")))
  }
}
