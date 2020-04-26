package fr.xebia.gbildi

import java.time.Duration
import java.util.Properties

import cats.implicits._
import fr.xebia.gbildi.config.{Configs, ScoringConfig}
import fr.xebia.gbildi.joiner.PredictionJoiner
import fr.xebia.gbildi.processor.CostMetricProcessor
import fr.xebia.gbildi.schema._
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.kstream.{JoinWindows, TimeWindows}
import org.apache.kafka.streams.scala.kstream._
import org.apache.kafka.streams.scala.{ByteArrayWindowStore, Serdes, StreamsBuilder}
import org.slf4j.LoggerFactory
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.auto._

import scala.collection.JavaConverters._


/**
 * Created by loicmdivad.
 */
object Scoring extends App with Configs {

  private val logger = LoggerFactory.getLogger(getClass)

  ConfigSource.default.at("streams-config").load[ScoringConfig].map { config =>

    val properties = new Properties()
    properties.putAll(config.kafkaClient.toMap.asJava)

    implicit val keySerde: SpecificAvroSerde[KeyClass] = new SpecificAvroSerde()
    implicit val dropoffSerde: SpecificAvroSerde[TaxiTripDropoff] = new SpecificAvroSerde()
    implicit val predictionSerde: SpecificAvroSerde[TripDurationPrediction] = new SpecificAvroSerde()

    implicit val tripDurationMseSerde: SpecificAvroSerde[TripDurationMse] = new SpecificAvroSerde()

    implicit val aggregatedRmse: SpecificAvroSerde[AggregatedTripDurationRmse] = new SpecificAvroSerde()

    keySerde.configure(config.kafkaClient.toMap.asJava, true)
    dropoffSerde.configure(config.kafkaClient.toMap.asJava, false)
    predictionSerde.configure(config.kafkaClient.toMap.asJava, false)

    tripDurationMseSerde.configure(config.kafkaClient.toMap.asJava, false)
    aggregatedRmse.configure(config.kafkaClient.toMap.asJava, false)

    val builder = new StreamsBuilder()

    implicit val dropoffConsumer: Consumed[KeyClass, TaxiTripDropoff] = Consumed.`with`

    implicit val predictionConsumer: Consumed[KeyClass, TripDurationPrediction] = Consumed.`with`

    implicit val mseProducer: Produced[KeyClass, TripDurationMse] = Produced.`with`

    implicit val rmseMaterializer: Materialized[String, AggregatedTripDurationRmse, ByteArrayWindowStore] =
      Materialized.`with`(Serdes.String, aggregatedRmse)

    val dropoffs: KStream[KeyClass, TaxiTripDropoff] = builder.stream(config.dropoffTopic)(dropoffConsumer)

    val predictions: KStream[KeyClass, TripDurationPrediction] = builder.stream(config.predictionTopic)

    val window = JoinWindows.of(Duration.ofHours(1))

    val joined = Joined.`with`(keySerde, predictionSerde, dropoffSerde)

    val grouped: Grouped[String, TripDurationMse] = Grouped.`with`("grouped-score")(Serdes.String, tripDurationMseSerde)

    predictions

      .join(dropoffs)(PredictionJoiner.predictionJoiner, window)(joined)

      .through(config.correctionTopic)

      .groupBy[String]((_, value) => value.version)(grouped)

      .windowedBy(TimeWindows.of(config.windowSize.toJava).advanceBy(config.windowSize.toJava))

      .aggregate[AggregatedTripDurationRmse](EmptyScore) { case (_, score, scores) =>

        new AggregatedTripDurationRmse(
          version = score.version,
          count = scores.count + 1,
          sum_mse = scores.sum_mse + score.mse,
          sum_duration = scores.sum_duration + score.trip_duration,
          sum_predicted = scores.sum_predicted + score.prediction
        )
      }

      .suppress(Suppressed.untilTimeLimit(Duration.ofMinutes(10), Suppressed.BufferConfig.maxRecords(5000)))

      .toStream.transform(() => new CostMetricProcessor)

    val topology = builder.build()

    logger.info(topology.describe().toString)

    val streams: KafkaStreams = new KafkaStreams(topology, properties)

    streams.start()

    sys.addShutdownHook {
      streams.close()
    }

  }.recover {
    case failures: ConfigReaderFailures =>
      failures.toList.foreach(failure => logger.error(s"Fail to parse the configuration: ${failure.description}"))
  }

}
