package fr.xebia.gbildi

import java.util.Properties

import fr.xebia.gbildi.processor.AppyInferenceProcessor
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.scala.{Serdes, StreamsBuilder}
import org.apache.kafka.streams.state.Stores
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.slf4j.LoggerFactory

/**
 * Created by loicmdivad.
 */
object Predict extends App {

  val logger = LoggerFactory.getLogger(getClass)

  val config = new Properties
  config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  config.put(StreamsConfig.APPLICATION_ID_CONFIG, "XEBICON19-PREDICT")
  // config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, ???)
  // config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, ???)
  config.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081")

  import org.apache.kafka.streams.scala.ImplicitConversions._
  import org.apache.kafka.streams.scala.Serdes.{Bytes, String}

  Stores.timestampedKeyValueStoreBuilder[String, Bytes](
    Stores.persistentKeyValueStore("MODELS-STORE"),
    String,
    Bytes
  )

  val builder = new StreamsBuilder()

  val modelStream = builder.stream[Bytes, String]("INPUT-RAWDATA-SETOSA")

  modelStream

    .transformValues(() => AppyInferenceProcessor())

    .filter((_, value: String) => value.nonEmpty)

    .to("OUPUT-PREDITION-SETOSA")(Produced.`with`(Serdes.Bytes, Serdes.String))

  //val assembledFeatures = builder.stream[GenericRecord, Bytes]("CAB-FEATURES")

  val kafkaStream = new KafkaStreams(builder.build(), config)

  sys.addShutdownHook {

    logger error "Ouups!"
    kafkaStream.close()
  }

  kafkaStream.cleanUp()
  kafkaStream.start()
}
