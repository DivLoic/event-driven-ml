package fr.xebia.gbildi.tfcompat

import java.nio.charset.Charset
import java.nio.{ByteBuffer, FloatBuffer, LongBuffer}

import org.tensorflow.Tensor

import scala.util.Try

/**
 * Created by loicmdivad.
 */
trait ScalarTensor[T] {
  def headOption(tensor: Tensor[T]): Option[T]
}

object ScalarTensor {

  implicit val StringScalarTensor: ScalarTensor[String] = new ScalarTensor[String] {
    override def headOption(tensor: Tensor[String]) = {
      val buffer: ByteBuffer = ByteBuffer.allocate(tensor.numBytes())
      tensor.writeTo(buffer)
      Try(new String(buffer.array(), Charset.forName("UTF-8")).trim).toOption
    }
  }

  implicit val LongScalarTensor: ScalarTensor[java.lang.Long] = new ScalarTensor[java.lang.Long] {
    override def headOption(tensor: Tensor[java.lang.Long]): Option[java.lang.Long] = {
      val buffer = LongBuffer.allocate(1)
      tensor.writeTo(buffer)
      buffer.array().headOption.map(long2Long)
    }
  }

  implicit val FloatScalarTensor: ScalarTensor[java.lang.Float] = new ScalarTensor[java.lang.Float] {
    override def headOption(tensor: Tensor[java.lang.Float]): Option[java.lang.Float] = {
      val buffer = FloatBuffer.allocate(1)
      tensor.writeTo(buffer)
      buffer.array().headOption.map(float2Float)
    }
  }

  object ScalarTensorSyntax {
    implicit class ScalarTensorOps[T](value: Tensor[T]) {
      def headOption(implicit tensorHead: ScalarTensor[T]): Option[T] = tensorHead.headOption(value)
    }
  }

}