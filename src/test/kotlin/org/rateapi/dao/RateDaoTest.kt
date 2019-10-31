package org.rateapi.dao

import io.kotlintest.matchers.startWith
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import org.http4k.cloudnative.env.Environment
import org.rateapi.model.RateList
import org.rateapi.model.RawRate
import java.time.DayOfWeek
import java.time.OffsetDateTime
import java.time.ZoneOffset

class RateDaoTest : StringSpec({

  val rateStore = RateDao("rates.json")
  val year = 2019
  val month = 10
  val dayOfMonth = 28

  "Rate file should create map without an exception" {
    Environment.fromResource("test.properties")
    rateStore.listRatesForDayOfWeek(DayOfWeek.MONDAY)?.let {
      it.asMapOfRanges().size
    }?.or(0) shouldBe 2
  }

  "Rates should be retrievable by a start and end date" {
    rateStore.getRate(
        OffsetDateTime.of(year, month, dayOfMonth, 9, 0, 0, 0, ZoneOffset.ofHours(-5)),
        OffsetDateTime.of(year, month, dayOfMonth, 12, 0, 0, 0,
            ZoneOffset.ofHours(-5))) shouldBe 1500
  }

  "User input can span multiple rates, but the API shouldn't return a valid rate" {
    rateStore.getRate(
        OffsetDateTime.of(year, month, dayOfMonth, 3, 0, 0, 0, ZoneOffset.ofHours(-5)),
        OffsetDateTime.of(year, month, dayOfMonth, 12, 0, 0, 0,
            ZoneOffset.ofHours(-5))) shouldBe null
  }

  "User input can span more than one day, but the API shouldn't return a valid rate" {
    val exception = shouldThrow<IllegalArgumentException> {
      val start = OffsetDateTime.of(year, month, dayOfMonth, 3, 0, 0, 0, ZoneOffset.ofHours(-5))
      val end = OffsetDateTime.of(year, month, dayOfMonth + 2, 12, 0, 0, 0, ZoneOffset.ofHours(-5))
      rateStore.getRate(start, end)
    }
    exception.message should startWith("Invalid date range")
  }

  "User input spanning more than one year should not return a valid rate." {
    val exception = shouldThrow<IllegalArgumentException> {
      val start = OffsetDateTime.of(year, month, dayOfMonth, 3, 0, 0, 0, ZoneOffset.ofHours(-5))
      val end = OffsetDateTime.of(year + 1, month, dayOfMonth, 12, 0, 0, 0, ZoneOffset.ofHours(-5))
      rateStore.getRate(start, end)
    }
    exception.message should startWith("Invalid date range")
  }

  "Rate requests for an invalid range (end time before start time) should come back as null" {
    val exception = shouldThrow<IllegalArgumentException> {
      rateStore.getRate(
          OffsetDateTime.of(year, month, dayOfMonth, 12, 0, 0, 0, ZoneOffset.ofHours(-5)),
          OffsetDateTime.of(year, month, dayOfMonth, 9, 0, 0, 0, ZoneOffset.ofHours(-5))
      )
    }
    exception.message should startWith("Invalid time range")
  }

  "Posting a new list of rates results in new rates being present" {
    val price = 99999
    val rates = RateList(rates = listOf(
        RawRate("mon,tues,wed,thurs,fri,sat,sun", "0100-2300", "America/Chicago", price)))
    rateStore.updateRates(rates)
        .getRate(OffsetDateTime.of(year, month, dayOfMonth, 3, 0, 0, 0, ZoneOffset.ofHours(-5)),
            OffsetDateTime.of(year, month, dayOfMonth, 12, 0, 0, 0,
                ZoneOffset.ofHours(-5))) shouldBe price
  }
})
