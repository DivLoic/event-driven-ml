package fr.xebia.gbildi.conf

import java.time.ZoneId

import com.typesafe.config.Config
import fr.xebia.gbildi.conf.UploadConfig.GcpConfig

/**
 * Created by loicmdivad.
 */
case class UploadConfig(topicPrefix: String,
                        timeZone: ZoneId,
                        gcpConfig: GcpConfig,
                        kafkaConsumer: Config,
                        kafkaProducer: Config)

object UploadConfig {

  case class GcpConfig(onGcp: Boolean = true, bucketName: String, bigQueryConfig: BigQueryConfig)

  case class BigQueryConfig(table: String, dtPredictionCol: String, dtCorrectionCol: String, date: String) {

    def pickupQuery: String = s"""
         |SELECT * FROM `$table`
         |WHERE EXTRACT(DATE FROM $dtPredictionCol) = DATE '$date'
         |ORDER BY $dtPredictionCol;""".stripMargin


    def dropoffQuery: String = s"""
         |SELECT * FROM `$table`
         |WHERE EXTRACT(DATE FROM $dtCorrectionCol) = DATE '$date'
         |ORDER BY $dtCorrectionCol;
         """.stripMargin
  }
}