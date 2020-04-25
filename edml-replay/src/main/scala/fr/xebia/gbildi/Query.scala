package fr.xebia.gbildi

import java.time.ZoneId

import fr.xebia.gbildi.Upload.{getBigQueryClient, queryStream}
import fr.xebia.gbildi.conf.UploadConfig
import fr.xebia.gbildi.replay.TypeMapping
import pureconfig.ConfigSource
import pureconfig.generic.auto._

/**
 * Created by loicmdivad.
 */
object Query extends App with TypeMapping {

  ConfigSource.default.at("upload-config").load[UploadConfig].map { uploadConfig =>

    implicit val zoneId: ZoneId = uploadConfig.timeZone

    val bqClient = getBigQueryClient(uploadConfig.gcpConfig.onGcp).get

    val pickupQuery = uploadConfig.gcpConfig.bigQueryConfig.pickupQuery.replace(";", " LIMIT 10;")

    queryStream(bqClient, pickupQuery)().toVector.foreach { row =>

      println(row.asTaxiTripPickup)

    }

    val dropoffQuery = uploadConfig.gcpConfig.bigQueryConfig.dropoffQuery.replace(";", " LIMIT 10;")

    queryStream(bqClient, dropoffQuery)().toVector.foreach { row =>

      println(row.asTaxiTripPickup)

    }
  }
}
