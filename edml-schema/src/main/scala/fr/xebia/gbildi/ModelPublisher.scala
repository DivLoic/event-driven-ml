package fr.xebia.gbildi

import java.io.{ByteArrayOutputStream, File}
import java.util.Properties

import fr.xebia.gbildi.conf.PublisherConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}
import org.slf4j.LoggerFactory
import org.tensorflow.TensorFlow
import org.zeroturnaround.zip.ZipUtil
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.collection.JavaConverters._

/**
 * Created by loicmdivad.
 */
object ModelPublisher extends App {

  val logger = LoggerFactory.getLogger(getClass)

  val tfModelDirKey: String = "tensorflow.saved.model.path"

  ConfigSource.default.load[PublisherConfig].map { publisherConfig =>

    logger info s"TensorFlow version ${TensorFlow.version()}"
    logger info s"Model path: ${publisherConfig.tensorflowConfig.getString(tfModelDirKey)}"

    val modelBundleDir = new File(publisherConfig.tensorflowConfig.getString(tfModelDirKey))

    val mapProps = publisherConfig.kafkaClient.entrySet().asScala
      .map(pair => (pair.getKey, publisherConfig.kafkaClient.getAnyRef(pair.getKey).toString))
      .toMap.asJava

    val properties = new Properties()
    properties.putAll(mapProps)

    val keyModelSerializer = new SpecificAvroSerializer[ModelKey]()
    val valueModelSerializer = new SpecificAvroSerializer[TFSavedModel]()

    valueModelSerializer.configure(mapProps, false)
    keyModelSerializer.configure(mapProps, true)

    val producer = new KafkaProducer[ModelKey, TFSavedModel](properties, keyModelSerializer, valueModelSerializer)

    logger info s"Zipping model bundle directory: ${modelBundleDir.getAbsolutePath}"
    val buffer = new ByteArrayOutputStream()
    ZipUtil.pack(modelBundleDir, buffer)

    logger info "Key / Value creation"
    val key = ModelKey("edml")
    val value = TFSavedModel(
      ziped_model = buffer.toByteArray,
      version = "0.1.0-SNAPSHOT",
      output = TFOutOperation("", ""),
      inputs = Seq(
        TFInOperation("", ""),
        TFInOperation("", ""),
        TFInOperation("", ""),
        TFInOperation("", ""),
      )
    )

    val record = new ProducerRecord(publisherConfig.modelTopic, key, value)

    logger info s"Producer Record publication in ${publisherConfig.modelTopic} topic"
    producer.send(record, new Callback {
      override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit =
        Option(exception).map { _ =>
          logger error("Fail to produce the model!", exception)
          exception.printStackTrace()
          sys.exit(1)
        }.getOrElse {
          logger info "Successfully produce the model:"
          logger info
            s""" Metadata:
               | topic: ${metadata.topic}
               | partition: ${metadata.partition}
               | offset: ${metadata.offset}
               | timestamp: ${metadata.timestamp} """.stripMargin.replace("\n", "")
          metadata.toString
        }
    })

    producer.flush()

  }.left.map { failures =>
    failures.toList.foreach(failure => logger.error(s"Fail to parse configuration: ${failure.description}"))
  }
}
