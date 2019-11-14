package fr.xebia.gbildi

import java.time._

import akka.actor.ActorSystem
import akka.event.Logging
import akka.kafka.ProducerMessage.{Message, Result}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import akka.stream.scaladsl.Sink
import akka.stream.{ActorAttributes, ActorMaterializer, ThrottleMode}
import cats.syntax.either._
import fr.xebia.gbildi.conf.ReplayConfig
import fr.xebia.gbildi.gcp.ConfluentCloudProvider
import fr.xebia.gbildi.replay.TimeTravel.formatter
import fr.xebia.gbildi.replay.{Throttling, TimeTravel, TypeMapping}
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.auto._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

/**
 * Created by loicmdivad.
 */
object Replay extends App with ConfluentCloudProvider with TimeTravel with conf.Implicits with TypeMapping {

  val logger = LoggerFactory.getLogger(getClass)

  implicit val system: ActorSystem = ActorSystem("EDML-REPLAY")

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val ignore = Sink.ignore
  val print = Sink.foreach{ println }

  val breadth = 1
  val timeCost = 1
  val maxBurst = 1

  ConfigSource.default.at("replay-config").load[ReplayConfig].map { replayConf =>

    implicit val zoneId: ZoneId = replayConf.timeZone

    val avroKeySerde = new GenericAvroSerde()
    val avroValueSerde = new GenericAvroSerde()

    avroKeySerde.configure(replayConf.kafkaProducer.toMap.asJava, true)
    avroValueSerde.configure(replayConf.kafkaProducer.toMap.asJava, false)

    val producerSettings: ProducerSettings[GenericRecord, GenericRecord] =
      ProducerSettings(system, avroKeySerde.serializer(), avroValueSerde.serializer())
        .withProperties(replayConf.kafkaProducer.toMap)

    val consumerConfig =
      ConsumerSettings(system, avroKeySerde.deserializer(), avroValueSerde.deserializer())
        .withProperties(replayConf.kafkaConsumer.toMap)

    val throttling = new Throttling(replayConf)

    Consumer
      .plainPartitionedSource(consumerConfig, Subscriptions.topics(replayConf.sourceTopic))

      .flatMapMerge(breadth, {case (_, source) => source })

      .filter(record => extractGenericInstant(replayConf.dtFiled, record.value()).map(isFutureEvent).getOrElse(false))

      .throttle(breadth, 1 second, maxBurst, throttling.timeCost(replayConf.dtFiled), ThrottleMode.Shaping)

      .map { consumerRecord =>

        ProducerMessage.Message(new ProducerRecord(replayConf.replayTopic,
          null, Instant.now().toEpochMilli,
          consumerRecord.key(),
          consumerRecord.value()
        ), consumerRecord.offset())
      }

      .via(Producer.flexiFlow(producerSettings))

      .withAttributes(
        ActorAttributes
          .logLevels(
            onFailure = Logging.ErrorLevel,
            onElement = Logging.DebugLevel,
            onFinish = Logging.InfoLevel
          )
      )

      .runWith(Sink.foreach {
        case Result(metadata, Message(record, passThrough)) =>
          if(passThrough % 100 == 0) {
            logger info s"Produced the ${passThrough}th record in ${record.topic()} at ts: ${metadata.timestamp()}"
            extractGenericInstant(replayConf.dtFiled, record.value()).foreach { instant =>
              logger info s"Datetime: ${instant.atZone(zoneId).format(formatter)} -  Value: ${record.value()}"
            }
          }
      }
  )

  }.recover {
    case failures: ConfigReaderFailures =>
      failures.toList.foreach(failure => logger.error(failure.description))
      materializer.shutdown()
      system.terminate()

    case failures =>
      logger.error("Unknown error: ", failures)
      materializer.shutdown()
      system.terminate()
  }
}
