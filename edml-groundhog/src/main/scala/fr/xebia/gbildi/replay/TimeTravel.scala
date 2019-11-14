package fr.xebia.gbildi.replay

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId, ZonedDateTime}

/**
 * Created by loicmdivad.
 */
trait TimeTravel {

  protected def translateDate(from: LocalDateTime, to: LocalDateTime): LocalDateTime =
    LocalDateTime.of(to.toLocalDate, from.toLocalTime)

  protected def translateDate(from: ZonedDateTime, to: ZonedDateTime): ZonedDateTime = ZonedDateTime.of(
    translateDate(from.withZoneSameInstant(to.getZone).toLocalDateTime, to.toLocalDateTime),
    to.getZone
  )

  protected def isFutureEvent(event: Instant)(implicit zoneId: ZoneId) = {
    val now =  ZonedDateTime.now().withZoneSameInstant(zoneId)
    translateDate(event.atZone(zoneId), now)
    .toLocalTime
    .isAfter(now.toLocalTime)
  }
}

object TimeTravel {

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
}
