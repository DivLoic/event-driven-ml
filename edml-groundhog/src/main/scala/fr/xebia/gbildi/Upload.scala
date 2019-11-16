package fr.xebia.gbildi

import java.time.ZoneId

import akka.actor.ActorSystem
import akka.event.Logging
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import akka.stream.{ActorAttributes, ActorMaterializer, Supervision}
import cats.implicits._
import fr.xebia.gbildi.conf.UploadConfig
import fr.xebia.gbildi.gcp.{ConfluentCloudProvider, GoogleCloudProvider}
import fr.xebia.gbildi.replay.TypeMapping
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.error.ConfigReaderFailures

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor

/**
 * Created by loicmdivad.
 */
object Upload extends App with TypeMapping with GoogleCloudProvider with ConfluentCloudProvider with conf.Implicits {

  val logger = LoggerFactory.getLogger(getClass)

  implicit val system: ActorSystem = ActorSystem("EDML-HISTORY-UPLOAD")

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  ConfigSource.default.at("upload-config").load[UploadConfig].flatMap { uploadConfig =>

    implicit val zoneId: ZoneId = uploadConfig.timeZone

    val keySerializer: SpecificAvroSerializer[KeyClass] = new SpecificAvroSerializer[KeyClass]()
    val pickupSerializer: SpecificAvroSerializer[TaxiTripPickup] = new SpecificAvroSerializer[TaxiTripPickup]()
    val dropoffSerializer: SpecificAvroSerializer[TaxiTripDropoff] = new SpecificAvroSerializer[TaxiTripDropoff]()

    keySerializer.configure(uploadConfig.kafkaProducer.toMap.asJava, true)
    pickupSerializer.configure(uploadConfig.kafkaProducer.toMap.asJava, false)
    dropoffSerializer.configure(uploadConfig.kafkaProducer.toMap.asJava, false)

    val pickupProducer: ProducerSettings[KeyClass, TaxiTripPickup] =
      ProducerSettings(system, keySerializer, pickupSerializer).withProperties(uploadConfig.kafkaProducer.toMap)

    val dropoffProducer: ProducerSettings[KeyClass, TaxiTripDropoff] =
      ProducerSettings(system, keySerializer, dropoffSerializer).withProperties(uploadConfig.kafkaProducer.toMap)

    getBigQueryClient(uploadConfig.gcpConfig.onGcp).map { bigquery =>

      val pickupQuery = uploadConfig.gcpConfig.bigQueryConfig.pickupQuery
      val dropoffQuery = uploadConfig.gcpConfig.bigQueryConfig.dropoffQuery

      val pickupFlow = Source
        .fromIterator(queryStream(bigquery, pickupQuery))
        .map { row =>
          val topic = s"${uploadConfig.topicPrefix}-PICKUP-${uploadConfig.gcpConfig.bigQueryConfig.date}"
          val key = row.asKeyClass("🚕")
          val value = row.asTaxiTripPickup
          new ProducerRecord(topic, key, value)
        }
        .withAttributes(ActorAttributes.supervisionStrategy(_ => Supervision.Resume))
        .withAttributes(ActorAttributes.logLevels(onFailure = Logging.WarningLevel))

      val dropOffFlow = Source
        .fromIterator(queryStream(bigquery, dropoffQuery))
        .map { row =>
          val topic = s"${uploadConfig.topicPrefix}-DROPOFF-${uploadConfig.gcpConfig.bigQueryConfig.date}"
          val key = row.asKeyClass("🚕")
          val value = row.asTaxiTripDropOff
          new ProducerRecord(topic, key, value)
        }
        .withAttributes(ActorAttributes.supervisionStrategy(_ => Supervision.Resume))
        .withAttributes(ActorAttributes.logLevels(onFailure = Logging.WarningLevel))

      pickupFlow.runWith(Producer.plainSink(pickupProducer))
      dropOffFlow.runWith(Producer.plainSink(dropoffProducer))

    }.toEither

  }.recover {
    case failures: ConfigReaderFailures =>
      failures.toList.foreach(failure => logger.error(failure.description))
      logger.error("Shunting down the uploader now!")
      sys.exit(1)

    case failure =>
      logger.error("Unknown error: ", failure)
      logger.error("Shunting down the uploader now!")
      sys.exit(1)
  }
}