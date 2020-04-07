package fr.xebia.gbildi.processor

import java.time.Duration

import com.spotify.zoltar.Model
import com.spotify.zoltar.tf.{TensorFlowLoader, TensorFlowModel}
import fr.xebia.gbildi.tfcompat.AvroTensor.AvroTensorSyntax._
import fr.xebia.gbildi.tfcompat.Dtypes.projectDtype
import fr.xebia.gbildi.tfcompat.ScalarTensor.ScalarTensorSyntax._
import fr.xebia.gbildi.{TFSavedModel, _}
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.streams.kstream.ValueTransformer
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.{Failure, Try}

/**
 * Created by loicmdivad.
 */
class PredictorTransformer(modelStoreName: String,
                           modelSignatureDef: String,
                           modelSelector: ModelKey,
                           modelOpts: TensorFlowModel.Options)

  extends ValueTransformer[TaxiTripPickup, TripDurationPrediction] {

  private val logger = LoggerFactory.getLogger(getClass)

  private var model: TensorFlowModel = _
  private var context: ProcessorContext = _
  private var modelStore: KeyValueStore[ModelKey, TFSavedModel] = _

  override def init(context: ProcessorContext): Unit = {
    this.context = context
    this.modelStore = this.context
      .getStateStore(modelStoreName)
      .asInstanceOf[KeyValueStore[ModelKey, TFSavedModel]]

    getSaved.flatMap(setModel) match {
      case Failure(exception) => logger warn s"Fail to initialize model due to: ${exception.getMessage}"
      case _ =>
    }
  }

  override def transform(value: TaxiTripPickup): TripDurationPrediction = {

    getSaved.flatMap {
      case saved if Option(this.model) map (_.id.value) contains saved.version => Try((saved, this.model))
      case saved => setModel(saved).map((saved, _))

    }.map { case (saved, model) =>

      val bundle = model.instance()

      val prediction: Option[Float] = saved
        .inputs
        .foldLeft(bundle.session().runner())((session, feature) => {
          session.feed(feature.name, projectDtype(feature.`type`, value.get(feature.name)).castTensor)
        })
        .fetch("add")
        .run()
        .asScala
        .head
        .expect(classOf[java.lang.Float])
        .headOption
        .map(Float2float)

      this.context.headers.add(new RecordHeader("version", saved.version.getBytes))

      TripDurationPrediction(prediction.getOrElse(-1F), version = saved.version)

    }.recoverWith { case exception =>
      logger error s"Prediction fail due to: ${exception.getMessage}"
      Failure(exception)

    }.getOrElse(PredictionError)

  }

  def getModelId(tfModel: TFSavedModel): Model.Id = Model.Id.create(tfModel.version)

  def getLoader(tfModel: TFSavedModel): TensorFlowLoader =
    TensorFlowLoader.create(getModelId(tfModel), tfModel.gcs_path, modelOpts, modelSignatureDef)

  def getSaved: Try[TFSavedModel] = Try(this.modelStore.get(modelSelector)).filter(Option(_).isDefined)

  def setModel(tfModel: TFSavedModel): Try[TensorFlowModel] = {
    val maybeModel = Try(getLoader(tfModel).get(Duration.ofSeconds(30)))
    maybeModel.foreach(this.model = _)
    maybeModel
  }

  override def close(): Unit = {}
}
