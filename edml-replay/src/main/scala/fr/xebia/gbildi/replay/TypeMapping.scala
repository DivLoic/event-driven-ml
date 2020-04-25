package fr.xebia.gbildi.replay

import java.time.{Instant, ZoneId, ZonedDateTime}

import com.google.cloud.bigquery.FieldValueList
import fr.xebia.gbildi.replay.TimeTravel.formatter
import fr.xebia.gbildi.{KeyClass, TaxiTripDropoff, TaxiTripPickup}
import org.apache.avro.generic.GenericRecord

import scala.util.Try

/**
 * Created by loicmdivad.
 */
trait TypeMapping {

  implicit class tableResultParserOps(row: FieldValueList) {

    def asKeyClass(implicit zoneId: ZoneId): KeyClass =
      KeyClass(uuid = row.get("uuid").getStringValue)

    def asTaxiTripPickup(implicit zoneId: ZoneId): TaxiTripPickup = {
      val datetime = row.get("pickup_datetime").getStringValue
      TaxiTripPickup(
        uuid = row.get("uuid").getStringValue,
        year = row.get("year").getLongValue.intValue(),
        weekofyear = row.get("weekofyear").getLongValue.intValue(),
        distance = row.get("distance").getDoubleValue.floatValue(),
        pickup_datetime = ZonedDateTime.parse(datetime, formatter.withZone(zoneId)).toInstant,
        dayofweek = row.get("dayofweek").getLongValue.intValue(),
        hourofday = row.get("hourofday").getLongValue.intValue(),
        pickup_zone_name = row.get("pickup_zone_name").getStringValue,
        dropoff_zone_name = row.get("dropoff_zone_name").getStringValue,
        passenger_count = row.get("passenger_count").getLongValue.intValue(),
        borough = s"🚕${row.get("pu_borough").getStringValue}"
      )
    }

    def asTaxiTripDropOff(implicit zoneId: ZoneId): TaxiTripDropoff = {
      val dateTime = row.get("dropoff_datetime").getStringValue
      TaxiTripDropoff(
        dropoff_datetime = ZonedDateTime.parse(dateTime, formatter.withZone(zoneId)).toInstant,
        trip_duration = row.get("trip_duration").getLongValue.intValue(),
        borough = s"🚕${row.get("do_borough").getStringValue}"
      )
    }
  }

  def extractGenericInstant(fieldName: String, record: GenericRecord) =
    Try(Instant.ofEpochMilli(record.get(fieldName).asInstanceOf[Long]))
}