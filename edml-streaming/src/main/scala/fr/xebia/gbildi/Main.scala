package fr.xebia.gbildi

import java.io.{BufferedOutputStream, ByteArrayInputStream, FileInputStream, FileOutputStream}
import java.nio.file.{Files, Paths}
import java.nio.{ByteBuffer, FloatBuffer, IntBuffer, LongBuffer}
import java.util

import org.slf4j.LoggerFactory
import org.tensorflow.example._
import org.tensorflow.framework.{CollectionDef, GraphDef, MetaGraphDef}
import org.tensorflow.{Graph, Session, Tensor, Tensors}
import org.zeroturnaround.zip.ZipUtil
import tensorflow.SavedObjectGraphOuterClass.SavedResource

import scala.reflect.io.File

/**
 * Created by loicmdivad.
 */
object Main extends App {

  import org.tensorflow.{SavedModelBundle, TensorFlow}

  import scala.collection.JavaConverters._

  val logger = LoggerFactory.getLogger(getClass)

  val modelPath: String = s"${sys.props("user.dir")}/edml-streaming/build/1572294143/"
  val newModel: String = s"${sys.props("user.dir")}/edml-streaming/build/PROTO/saved_model.pb"
  //val zipPath: String = s"${sys.props("user.dir")}/edml-streaming/build/1572294143.zip"
  //val newZip: String = s"${sys.props("user.dir")}/edml-streaming/build/NEW_ZIP.zip"

  logger info TensorFlow.version()
  logger info s"Model path: $modelPath"

  val bundle: SavedModelBundle = SavedModelBundle.load(modelPath, "serve")

  bundle.session()


  def floatList(f: Float) = FloatList
    .newBuilder
    .addAllValue(Iterable(float2Float(f)) asJava)

  def intList(i: Int): Int64List.Builder = Int64List
    .newBuilder
    .addAllValue(Iterable(long2Long(i.toLong)) asJava)

  def instantList(l: Long) = Int64List
    .newBuilder
    .addAllValue(Iterable(long2Long(l)) asJava)

  // 5.1, 3.3, 1.7, 0.5
  // 5.9, 3.0, 4.2, 1.5
  // 6.9, 3.1, 5.4, 2.1

  //model.metaGraphDef()
  //println(model.graph().operation("linear/linear/activation").output(0).dataType().name())//.asScala.foreach(op => println(op.name()))
  //model.graph().operations().asScala.foreach(op => println(op.name()))

  val graph = new Graph()
  graph.importGraphDef(bundle.graph().toGraphDef)
  val session = new Session(graph)

    import scala.collection.JavaConverters._

    //bundle.graph().operations().asScala.foreach(op => println(op.name()))
    val result: Tensor[java.lang.Float] = session.runner()
    .feed("dayofweek", Tensor.create(Array(1L), LongBuffer.wrap(Array(long2Long(4)))))
    .feed("hourofday", Tensor.create(Array(1L), LongBuffer.wrap(Array(long2Long(12)))))
    .feed("pickup_borough", Tensors.create(Array("Queens".getBytes)))
    .feed("dropoff_borough", Tensors.create(Array("Queens".getBytes)))

    .fetch("add")

    .run().get(0).expect(classOf[java.lang.Float])

  val bufferResult = FloatBuffer.allocate(1)
  result.writeTo(bufferResult)

  println(s"RESULT : ${bufferResult.array().mkString}")
}
