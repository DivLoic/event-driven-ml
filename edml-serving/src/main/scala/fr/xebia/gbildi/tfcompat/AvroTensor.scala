package fr.xebia.gbildi.tfcompat

import java.lang
import java.nio.{FloatBuffer, IntBuffer, LongBuffer}

import org.apache.avro.util.Utf8
import org.tensorflow.{Tensor, Tensors}

/**
 * Created by loicmdivad.
 */
trait AvroTensor[T] {
  def toTensor(value: T): Tensor[T]
}

object AvroTensor {

  implicit val StringAvroTensor: AvroTensor[String] = (value: String) => Tensors.create(Array(value.getBytes))

  implicit val IntAvroTensor: AvroTensor[Integer] = (value: Integer) => Tensor.create(Array(1L), IntBuffer.wrap(Array(value)))

  implicit val LongAvroTensor: AvroTensor[lang.Long] = (value: lang.Long) => Tensor.create(Array(1L), LongBuffer.wrap(Array(value)))

  implicit val FloatAvroTensor: AvroTensor[lang.Float] = (value: lang.Float) => Tensor.create(Array(1L), FloatBuffer.wrap(Array(value)))

  object AvroTensorSyntax {

    implicit class AvroTensorOps[T](value: T) {

      def toTensor(implicit avroTensor: AvroTensor[T]): Tensor[T] = avroTensor.toTensor(value)

    }

    implicit class ObjectTensorOps(value: Any) {

      def castTensor: Tensor[_] = value match {
        case typed: String => StringAvroTensor.toTensor(typed)
        case typed: lang.Long => LongAvroTensor.toTensor(typed)
        case typed: lang.Float => FloatAvroTensor.toTensor(typed)
        case typed: lang.Integer => IntAvroTensor.toTensor(typed)
        case _ => throw new Exception(s"The only types available are String, Long, Float. Found: $value")
      }
    }

  }

}
