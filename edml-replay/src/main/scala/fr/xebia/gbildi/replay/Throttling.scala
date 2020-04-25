package fr.xebia.gbildi.replay

import java.time.{Clock, Duration, LocalDateTime, ZoneId, ZonedDateTime}

import fr.xebia.gbildi.TaxiTripPickup
import fr.xebia.gbildi.conf.ReplayConfig
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try

/**
 * Created by loicmdivad.
 */
class Throttling(val replayConfig: ReplayConfig)(implicit zoneId: ZoneId) extends TimeTravel with TypeMapping {

  private val clock: Clock = Clock.tickSeconds(zoneId)
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  //val minTime: ZonedDateTime = replayConfig.timingConfig.minLocalTime.atZone(replayConfig.timingConfig.timeZone)
  //val maxTime: ZonedDateTime = replayConfig.timingConfig.maxLocalTime.atZone(replayConfig.timingConfig.timeZone)

  private def currentZDateTime = ZonedDateTime.now(clock)
  private def currentLDateTime = LocalDateTime.ofInstant(currentZDateTime.toInstant, clock.getZone)

  def translateDateToCurrentDay(from: LocalDateTime): LocalDateTime = translateDate(from, to = currentLDateTime)

  def timeCost(fieldName: String)(record: ConsumerRecord[_, GenericRecord]): Int = {
    val now = currentZDateTime
    val triedCost = extractGenericInstant(fieldName, record.value()).map { instant =>
      val zonedDateTime = instant.atZone(clock.getZone)
      val fakeTime = translateDate(from = zonedDateTime, to = now)
      Math.min(Int.MaxValue, Math.max(0, Duration.between(now, fakeTime).getSeconds)).toInt
    }
    triedCost.getOrElse(-1)
  }
}
