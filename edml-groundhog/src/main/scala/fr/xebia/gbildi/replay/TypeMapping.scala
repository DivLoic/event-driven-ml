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

    def asKeyClass(label: String)(implicit zoneId: ZoneId): KeyClass =
      KeyClass(label = label, uuid = row.get("uuid").getStringValue)

    def asTaxiTripPickup(implicit zoneId: ZoneId): TaxiTripPickup = {
      val datetime = row.get("pickup_datetime").getStringValue
      TaxiTripPickup(
        uuid = row.get("uuid").getStringValue,
        pickup_datetime = ZonedDateTime.parse(datetime, formatter.withZone(zoneId)).toInstant,
        dayofweek = row.get("dayofweek").getLongValue.intValue(),
        hourofday = row.get("hourofday").getLongValue.intValue(),
        pickup_borough = row.get("pickup_borough").getStringValue,
        dropoff_borough = row.get("dropoff_borough").getStringValue
      )
    }

    def asTaxiTripDropOff(implicit zoneId: ZoneId): TaxiTripDropoff = {
      val dateTime = row.get("dropoff_datetime").getStringValue
      TaxiTripDropoff(
        dropoff_datetime = ZonedDateTime.parse(dateTime, formatter.withZone(zoneId)).toInstant,
        trip_distance = row.get("trip_distance").getNumericValue.floatValue(),
        trip_duration = row.get("trip_duration").getLongValue.intValue(),
        total_amount = row.get("total_amount").getNumericValue.floatValue()
      )
    }
  }

  def extractGenericInstant(fieldName: String, record: GenericRecord) =
    Try(Instant.ofEpochMilli(record.get(fieldName).asInstanceOf[Long]))


}

object TypeMapping {

}