package fr.xebia.gbildi

import java.time.{LocalDateTime, LocalTime, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter

import pureconfig.{ConfigConvert, ConfigCursor, ConfigReader}
import pureconfig.configurable.{localDateTimeConfigConvert, localTimeConfigConvert, zonedDateTimeConfigConvert}
import pureconfig.error.{CannotConvert, ConfigReaderFailures, ConvertFailure}

import scala.util.Try

/**
 * Created by loicmdivad.
 */
package object conf {
  trait Implicits {
    implicit val localTimeConvert: ConfigConvert[LocalTime] = localTimeConfigConvert(DateTimeFormatter.ISO_LOCAL_TIME)
    implicit val localDateTimeConvert: ConfigConvert[LocalDateTime] = localDateTimeConfigConvert(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    implicit val zonedDateTimeConvert: ConfigConvert[ZonedDateTime] = zonedDateTimeConfigConvert(DateTimeFormatter.ISO_DATE_TIME)

    implicit object ZonedIdReader extends ConfigReader[ZoneId] {
      def from(cur: ConfigCursor): Either[ConfigReaderFailures, ZoneId] = cur.asString.right.flatMap(s =>
        Try(ZoneId.of(s)).toEither.left.map{ err =>
          ConfigReaderFailures(ConvertFailure(CannotConvert(s, "ZonedId", err.toString), cur))
        }
      )
    }

    //implicit val inputTopicConfReader: ConfigReader[InputTopicConf] = ConfigReader.fromString[InputTopicConf](ConvertHelpers.catchReadError(InputTopicConf))
    //implicit val outputTopicConfReader: ConfigReader[OutputTopicConf] = ConfigReader.fromString[OutputTopicConf](ConvertHelpers.catchReadError(OutputTopicConf))
  }
}
