package org.rateapi.dao

import com.beust.klaxon.Klaxon
import com.google.common.base.Splitter
import com.google.common.collect.Iterators
import com.google.common.collect.Range
import com.google.common.collect.RangeMap
import com.google.common.collect.TreeRangeMap
import com.google.common.io.Resources
import org.rateapi.model.RateList
import org.rateapi.model.RawRate
import org.slf4j.LoggerFactory
import java.time.DayOfWeek
import java.time.Instant
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneId

/**
 * A data access layer for retrieving stored routes based on the values stored within
 * the json rates source file.
 *
 * Rates are stored in a [Map] with the key being the [DayOfWeek] and the value for
 * each [DayOfWeek] being a [RangeMap<OffsetTime, int>] where the key of the [RangeMap] is
 * a range of times during the day represented as a [OffsetTime] and the value being a
 * rate as an [Int].
 *
 * For example, the following is a diagram of how this is structured.
 *
 *    TUESDAY ->
 *      Range(100, 200) -> 120
 *      Range(201, 300) -> 140
 *    WEDNESDAY ->
 *      Range(100, 200) -> 100
 *      Range(201, 300) -> 120
 *
 * This data structure simplifies the retrieval of rates based on a start and end time
 * and if a rate lookup has a start and end time which overlaps several rates then the
 * map retrieval will return multiple rates for the given range - making it easy to
 * determine if a rate should be `unavailable`.
 *
 * The additional benefit with this is the efficiency of the rate retrieval. Retrieving
 * the given day of week is a trivial [HashMap] lookup and the [RangeMap] value for
 * each [DayOfWeek] allows for simple retrieval of rates while also being stored as
 * tree for efficient retrieval of the underlying rates.
 */
class RateDao(rateFilename: String) {

  /**
   * Rate map which holds the time ranges and rates for each day of the week.
   */
  private var rateMap = parseRates(rateFilename)?.let {
    rateListToMap(it)
  }.orEmpty()

  /**
   * Retrieves a rate given a start and end date and time.
   *
   * @param startDateTime the opening of the range for the lookup
   * @param endDateTime the closing of the range for the lookup
   * @return a rate as an integer if available, or null if the rate is unavailable.
   */
  fun getRate(startDateTime: OffsetDateTime, endDateTime: OffsetDateTime): Int? {
    log.info("Retrieving rate for range of $startDateTime to $endDateTime")
    if (startDateTime.isAfter(endDateTime)) {
      throw IllegalArgumentException(
          "Invalid time range. Start time '$startDateTime' is after the provided end time '$endDateTime'.")
    }
    if (startDateTime.dayOfYear != endDateTime.dayOfYear || startDateTime.year != endDateTime.year) {
      throw IllegalArgumentException(
          "Invalid date range. Start date of '$startDateTime' is not on the same day as '$endDateTime'.")
    }
    val range = Range.openClosed(
        startDateTime.toOffsetTime(), endDateTime.toOffsetTime())
    val ranges = rateMap[startDateTime.dayOfWeek]
    ranges?.asMapOfRanges()?.toList()?.forEach { subRange ->
      log.info("Date ${startDateTime.dayOfWeek} Range $subRange")
    }
    val subRange = ranges?.subRangeMap(range)
    if (subRange?.asMapOfRanges()?.size == 1) {
      return Iterators.getLast(subRange.asMapOfRanges().iterator()).value
    }
    log.info("Returning null for $startDateTime and $endDateTime")
    return null
  }

  /**
   * Overwrites the current rates with the rates provided in the [RateList] object passed to the
   * function.
   *
   * @param rateList the list of new rates
   * @return a reference to this object with the rates updated. Note the map is mutable within this
   * object so the rates will be changed globally. This method returns the updated [RateDao] simply
   * for ease of use for making additional calls to the [RateDao] object.
   */
  fun updateRates(rateList: RateList): RateDao {
    rateMap = rateListToMap(rateList)
    return this
  }

  /**
   * Provides all the rates for a given day of the week.
   *
   * @param dayOfWeek [DayOfWeek] to lookup
   * @return a [RangeMap] containing the time range as a key and the rate as an integer value.
   */
  fun listRatesForDayOfWeek(dayOfWeek: DayOfWeek): RangeMap<OffsetTime, Int>? =
      rateMap[dayOfWeek]

  companion object {

    private val log = LoggerFactory.getLogger(RateDao::class.java)

    private fun rateToRangeEntry(rate: RawRate): Map<DayOfWeek, Range<OffsetTime>> =
        splitDays(rate.days).map { day ->
          val splitTime = rate.times.split("-").toList()
          val range =
              createRange(splitTime.get(0), splitTime.get(1),
                  rate.tz)
          dayStringToDayOfWeek(day) to range
        }.toMap()

    private fun createRange(startTimeStr: String, endTimeStr: String,
                            timezone: String): Range<OffsetTime> {
      val startTime = splitTime(startTimeStr, timezone)
      val endTime = splitTime(endTimeStr, timezone)
      log.info("Creating range ${startTime} to ${endTime}")
      return Range.open(startTime, endTime)
    }

    private fun splitTime(timeStr: String, timezone: String): OffsetTime {
      val hour = timeStr.substring(0, 2).toInt()
      val minute = timeStr.substring(2, 4).toInt()
      val second = 0
      val millis = 0
      return OffsetTime.of(hour, minute, second, millis, ZoneId.of(timezone)
          //TODO using now here is probably not correct with daylist savings
          .rules.getOffset(Instant.now()))
    }

    private fun splitDays(daysString: String) =
        Splitter.on(",").splitToList(daysString)

    private fun dayStringToDayOfWeek(dayString: String): DayOfWeek =
        when (dayString) {
          "mon" -> DayOfWeek.MONDAY
          "tues" -> DayOfWeek.TUESDAY
          "wed" -> DayOfWeek.WEDNESDAY
          "thurs" -> DayOfWeek.THURSDAY
          "fri" -> DayOfWeek.FRIDAY
          "sat" -> DayOfWeek.SATURDAY
          "sun" -> DayOfWeek.SUNDAY
          else -> throw IllegalArgumentException("Unknown day of week $dayString")
        }

    private fun parseRates(filename: String): RateList? {
      log.info("Reading rates from file $filename")
      return Klaxon().parse<RateList>(
          Resources.getResource(filename).openStream())
    }

    private fun rateListToMap(rateList: RateList): Map<DayOfWeek, RangeMap<OffsetTime, Int>> {
      val dayMap = mutableMapOf<DayOfWeek, RangeMap<OffsetTime, Int>>()
      rateList.rates.forEach { rate ->
        rateToRangeEntry(rate).forEach { (day, range) ->
          dayMap.getOrPut(day, {
            TreeRangeMap.create()
          }).put(range, rate.price)
        }
      }
      return dayMap
    }
  }
}
