package fr.xebia.gbildi.processor

import java.nio.LongBuffer

import fr.xebia.gbildi.Worksheet.{bufferResult, feature, result}
import fr.xebia.gbildi.processor.AppyInferenceProcessor.Labels
import org.apache.kafka.streams.kstream.ValueTransformer
import org.apache.kafka.streams.processor.ProcessorContext
import org.tensorflow.example.{Example, Feature, Features}
import org.tensorflow.{SavedModelBundle, Tensors}

import scala.util.Try

/**
 * Created by loicmdivad.
 */
class AppyInferenceProcessor extends ValueTransformer[String, String]{

  var context: ProcessorContext = _
  val model: SavedModelBundle = SavedModelBundle.load(getClass.getResource("/model/setosa1").getPath, "serve")


  override def init(context: ProcessorContext): Unit = {
    this.context = context
  }

  override def transform(value: String): String = {

    val Array(sepalLength, sepalWidth, petalLength, petalWidth) = value.split(",").map(_.trim.toFloat)

        val features = Features.newBuilder()
          .putFeature("SepalLength", Feature.newBuilder().setFloatList(feature(sepalLength)).build())
          .putFeature("SepalWidth", Feature.newBuilder().setFloatList(feature(sepalWidth)).build())
          .putFeature("PetalLength", Feature.newBuilder().setFloatList(feature(petalLength)).build())
          .putFeature("PetalWidth", Feature.newBuilder().setFloatList(feature(petalWidth)).build()).build()

        val example = Example.newBuilder.setFeatures(features).build

    val result = model.session().runner()

          .feed("input_example_tensor", Tensors.create(Array(example.toByteArray)))
          .fetch("dnn/head/predictions/class_ids")
          .run().get(0).expect(classOf[java.lang.Long])

        val bufferResult = LongBuffer.allocate(1)
        result.writeTo(bufferResult)

    Labels(bufferResult.array().mkString)

  }

  override def close(): Unit = model.close()
}


object AppyInferenceProcessor {

  def apply(): AppyInferenceProcessor = new AppyInferenceProcessor()

  val Labels = Map("0" -> "Setosa", "1" -> "Versicolor", "2" -> "Virginica")
}
