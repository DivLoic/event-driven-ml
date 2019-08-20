package fr.xebia.gbildi

import java.nio.LongBuffer

import org.slf4j.LoggerFactory
import org.tensorflow.example.{Example, Feature, Features, FloatList}
import org.tensorflow.{Tensor, Tensors}

/**
 * Created by loicmdivad.
 */
object Worksheet extends App {

  import scala.collection.JavaConverters._
  import org.tensorflow.{SavedModelBundle, TensorFlow}

  val logger = LoggerFactory.getLogger(getClass)

  val modelPath: String = s"${sys.props("user.dir")}/edml-streaming/build/setosa/1565800983/"

  logger info TensorFlow.version()
  logger info s"Model path: $modelPath"

  val model: SavedModelBundle = SavedModelBundle.load(modelPath, "serve")

  def feature(f: Float): FloatList = FloatList
    .newBuilder
    .addAllValue(Iterable(f.asInstanceOf[java.lang.Float]) asJava)
    .build()

  val features: Features = Features.newBuilder()
    .putFeature("SepalLength", Feature.newBuilder().setFloatList(feature(5.1F)).build())        //[5.1, 5.9, 6.9],
    .putFeature("SepalWidth", Feature.newBuilder().setFloatList(feature(3.3F)).build())         //[3.3, 3.0, 3.1],
    .putFeature("PetalLength", Feature.newBuilder().setFloatList(feature(1.7F)).build())        //[1.7, 4.2, 5.4],
    .putFeature("PetalWidth", Feature.newBuilder().setFloatList(feature(0.5F)).build()).build() //[0.5, 1.5, 2.1],

  val example = Example.newBuilder.setFeatures(features).build

  // 5.1, 3.3, 1.7, 0.5
  // 5.9, 3.0, 4.2, 1.5
  // 6.9, 3.1, 5.4, 2.1

  val result: Tensor[java.lang.Long] = model.session().runner()

    .feed("input_example_tensor", Tensors.create(Array(example.toByteArray)))
    .fetch("dnn/head/predictions/class_ids")
    .run().get(0).expect(classOf[java.lang.Long])

  val bufferResult = LongBuffer.allocate(1)
  result.writeTo(bufferResult)

  println(s"RESULT : ${bufferResult.array().mkString}")
}
