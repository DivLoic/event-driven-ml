package fr.xebia.gbildi.processor

import java.time.ZoneId
import java.time.format.DateTimeFormatter

import fr.xebia.gbildi.{ModelKey, TFSavedModel}
import org.apache.kafka.streams.processor.{Processor, ProcessorContext, ProcessorSupplier}
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory

/**
 * Created by loicmdivad.
 */
class ModelReceiver(storeName: String) extends Processor[ModelKey, TFSavedModel] {

  private val logger =  LoggerFactory.getLogger(getClass)

  private var context: ProcessorContext = _
  private var modelStore: KeyValueStore[ModelKey, TFSavedModel] = _

  override def init(context: ProcessorContext): Unit = {
    this.context = context
    this.modelStore = this.context
      .getStateStore(storeName)
      .asInstanceOf[KeyValueStore[ModelKey, TFSavedModel]]
  }

  override def process(key: ModelKey, value: TFSavedModel): Unit = {
    logger debug  s"A new model is loaded with key: $key and version ${value.version}"
    this.modelStore.put(key, value)
  }

  override def close(): Unit = {}
}

object ModelReceiver {

  def modelReceiverSupplier(storeName: String): ProcessorSupplier[ModelKey, TFSavedModel] =
    () => new ModelReceiver(storeName)

  val versionHeaderKey: String = "version"

  val zoneId: ZoneId = ZoneId.systemDefault()

  val formatter: DateTimeFormatter =
    DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HH:mm:ss.Z")
      .withZone(zoneId)
}