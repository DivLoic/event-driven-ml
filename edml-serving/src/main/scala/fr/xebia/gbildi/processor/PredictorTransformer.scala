package fr.xebia.gbildi.processor

import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.{FloatBuffer, LongBuffer}
import java.time.Duration
import java.util.UUID

import fr.xebia.gbildi._
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.streams.kstream.ValueTransformer
import org.apache.kafka.streams.processor.{ProcessorContext, PunctuationType}
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory
import org.tensorflow.{SavedModelBundle, Tensor, Tensors}
import org.zeroturnaround.zip.ZipUtil

import scala.reflect.io.File

/**
 * Created by loicmdivad.
 */
class PredictorTransformer(modelSelector: ModelKey, modelStoreName: String, bundleStorePath: String)

  extends ValueTransformer[TaxiTripPickup, TripDurationPrediction] {

  private val logger = LoggerFactory.getLogger(getClass)

  private var model: TFSavedModel = _
  private var bundle: SavedModelBundle = _
  private var context: ProcessorContext = _
  private var modelStore: KeyValueStore[ModelKey, TFSavedModel] = _

  override def init(context: ProcessorContext): Unit = {
    this.context = context
    this.modelStore = this.context
      .getStateStore(modelStoreName)
      .asInstanceOf[KeyValueStore[ModelKey, TFSavedModel]]

    this.context.schedule(Duration.ofSeconds(10), PunctuationType.WALL_CLOCK_TIME, timestamp => {
      logger debug s"Cached model verification at: $timestamp, current version: ${Option(this.model).map(_.version)}"
      checkCurrent.foreach(this.updateModel)
    })

    checkCurrent.foreach(this.updateModel)
  }


  override def transform(value: TaxiTripPickup): TripDurationPrediction = {
    checkCurrent.foreach(this.updateModel)

    Option(this.bundle).map { bundle =>

      val dayofweek = Tensor.create(Array(1L), LongBuffer.wrap(Array(value.get("dayofweek").toString.toLong)))
      val hourofday = Tensor.create(Array(1L), LongBuffer.wrap(Array(value.get("hourofday").toString.toLong)))
      val pickupBorough = Tensors.create(Array(value.get("pickup_zone_name").toString.getBytes))
      val dropoffBorough = Tensors.create(Array(value.get("dropoff_zone_name").toString.getBytes))
      val passengerCount = Tensor.create(Array(1L), FloatBuffer.wrap(Array(value.get("passenger_count").toString.toFloat)))

      val result: Tensor[java.lang.Float] = bundle.session().runner()
        .feed("dayofweek", dayofweek)
        .feed("hourofday", hourofday)
        .feed("pickup_zone_name", pickupBorough)
        .feed("dropoff_zone_name", dropoffBorough)
        .feed("passenger_count", passengerCount)

        .fetch("add")

        .run().get(0).expect(classOf[java.lang.Float])

      val buffer = FloatBuffer.allocate(1)
      result.writeTo(buffer)

      this.context.headers.add(new RecordHeader("version", this.model.version.getBytes))

      TripDurationPrediction(buffer.array().headOption.getOrElse(-1), version = this.model.version)

    }.getOrElse {
      logger debug "Fail to predict duration trip due to missing model"
      PredictionError
    }
  }

  def checkCurrent: Option[TFSavedModel] = Option(this.modelStore.get(modelSelector)).filterNot { latest =>
      Option(this.model).map(_.version).contains(latest.version)
    }

  def updateModel(model: TFSavedModel): Unit = {
    logger info s"Update model to the version: ${model.version}"
    val root = bundleStorePath
    val group = this.context.applicationId()
    val app = modelSelector.application
    val edge = UUID.randomUUID().toString.substring(0, 8)
    val directory = File(s"$root/$group/$app/$edge")

    logger info s"Unpack bundle and load from: ${directory.path}"
    Files.createDirectories(directory.jfile.toPath)

    logger debug "Unpack zipped model starts"
    ZipUtil.unpack(new ByteArrayInputStream(model.ziped_model), directory.jfile)

    this.bundle = SavedModelBundle.load(directory.path, "serve")
    this.model = model

    logger debug s"Deleting: ${directory.path}"
    directory.deleteRecursively()

    logger debug "Model update completed"
  }

  override def close(): Unit = {
    val root = bundleStorePath
    val group = this.context.applicationId()
    val app = modelSelector.application
    val directory = File(s"$root/$group/$app")
    directory.deleteRecursively()
  }

}
