package fr.xebia.gbildi

import java.util.Properties

import cats.implicits._
import fr.xebia.gbildi.config.{Configs, ServingConfig}
import fr.xebia.gbildi.processor.{ModelReceiver, PredictorTransformer}
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream.{Consumed, KStream, Produced}
import org.apache.kafka.streams.state.{KeyValueBytesStoreSupplier, KeyValueStore, StoreBuilder, Stores}
import org.slf4j.LoggerFactory
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.auto._

import scala.collection.JavaConverters._

/**
 * Created by loicmdivad.
 */
object Serving extends App with Configs {

  val bundleKey: String = "tensorflow.saved.model.path"

  private val logger = LoggerFactory.getLogger(getClass)

  ConfigSource.default.at("streams-config").load[ServingConfig].map { config =>

    implicit val keySerde: SpecificAvroSerde[KeyClass] = new SpecificAvroSerde()
    implicit val modelKeySerde: SpecificAvroSerde[ModelKey] = new SpecificAvroSerde()
    implicit val pickupSerde: SpecificAvroSerde[TaxiTripPickup] = new SpecificAvroSerde()
    implicit val savedModelSerde: SpecificAvroSerde[TFSavedModel] = new SpecificAvroSerde()
    implicit val predictionSerde: SpecificAvroSerde[TripDurationPrediction] = new SpecificAvroSerde()

    keySerde.configure(config.kafkaClient.toMap.asJava, true)
    modelKeySerde.configure(config.kafkaClient.toMap.asJava, true)
    pickupSerde.configure(config.kafkaClient.toMap.asJava, false)
    savedModelSerde.configure(config.kafkaClient.toMap.asJava, false)
    predictionSerde.configure(config.kafkaClient.toMap.asJava, false)

    val properties = new Properties()
    properties.putAll(config.kafkaClient.toMap.asJava)

    val builder: StreamsBuilder = new StreamsBuilder()

    implicit val producer: Produced[KeyClass, TripDurationPrediction] = Produced.`with`
    implicit val consumer: Consumed[KeyClass, TaxiTripPickup] = Consumed.`with`
    implicit val modelConsumer: Consumed[ModelKey, TFSavedModel] = Consumed.`with`

    val modelStore: KeyValueBytesStoreSupplier =
      Stores.persistentKeyValueStore(config.modelStore.toLowerCase())

    val modelStoreBuilder: StoreBuilder[KeyValueStore[ModelKey, TFSavedModel]] =
      Stores.keyValueStoreBuilder(modelStore, modelKeySerde, savedModelSerde).withLoggingDisabled()

    logger info s"Creation of the global state store: ${modelStore.name()}"

    builder.addGlobalStore(
      modelStoreBuilder,
      config.modelTopic,
      modelConsumer,
      ModelReceiver.modelReceiverSupplier(modelStore.name())
    )

    val pickups: KStream[KeyClass, TaxiTripPickup] = builder.stream(config.pickupTopic)

    val applyedModel: KStream[KeyClass, TripDurationPrediction] = pickups.transformValues(
      () => new PredictorTransformer(
        ModelKey("edml"),
        config.modelStore.toLowerCase(),
        config.tensorflowConfig.getString(bundleKey)
      )
    )

    val Array(errors, prediction) = applyedModel.branch(
      (_, value) => value == PredictionError,
      (_, _) => true
    )

    prediction

      .to(config.predictionTopic)

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
