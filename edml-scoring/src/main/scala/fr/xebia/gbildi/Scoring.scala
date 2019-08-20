package fr.xebia.gbildi

import java.time.Duration
import java.util.Properties

import cats.implicits._
import fr.xebia.gbildi.config.{Configs, ScoringConfig}
import fr.xebia.gbildi.joiner.PredictionJoiner
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.kstream.{JoinWindows, Printed}
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream.{Consumed, Joined, KStream, Produced}
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

    implicit val correctionSerde: SpecificAvroSerde[TripDurationCorrection] = new SpecificAvroSerde()

    keySerde.configure(config.kafkaClient.toMap.asJava, true)
    dropoffSerde.configure(config.kafkaClient.toMap.asJava, false)
    predictionSerde.configure(config.kafkaClient.toMap.asJava, false)

    correctionSerde.configure(config.kafkaClient.toMap.asJava, false)

    val builder = new StreamsBuilder()

    implicit val dropoffConsumer: Consumed[KeyClass, TaxiTripDropoff] = Consumed.`with`

    implicit val predictionConsumer: Consumed[KeyClass, TripDurationPrediction] = Consumed.`with`

    implicit val correctionProducer: Produced[KeyClass, TripDurationCorrection] = Produced.`with`

    val dropoffs: KStream[KeyClass, TaxiTripDropoff] = builder.stream(config.dropoffTopic)(dropoffConsumer)

    val predictions: KStream[KeyClass, TripDurationPrediction] = builder.stream(config.predictionTopic)

    val window = JoinWindows.of(Duration.ofHours(1))

    val joined = Joined.`with`(keySerde, predictionSerde, dropoffSerde)

    val corrections: KStream[KeyClass, TripDurationCorrection] =

      predictions.join(dropoffs)(PredictionJoiner.predictionJoiner, window)(joined)

    corrections

        //.print(Printed.toSysOut[KeyClass, TripDurationCorrection])
      .to(config.correctionTopic)

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
