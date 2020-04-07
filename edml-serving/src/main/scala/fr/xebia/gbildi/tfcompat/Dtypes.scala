package fr.xebia.gbildi.tfcompat

import org.apache.avro.util.Utf8

import scala.reflect.runtime.universe._
import scala.util.Try

/**
 * Created by loicmdivad.
 */
object Dtypes {

  def projectDtype(dtypes: String, value: AnyRef): Any = dtypes match {

      case "DT_INT32" => projectInt(value).get
      case "DT_INT64" => projectLong(value).get
      case "DT_FLOAT" => projectFloat(value).get
      case "DT_STRING" => projectString(value).get
      case _ => value
    }


  def projectInt(anyValue: Any): Try[java.lang.Integer] = Try(anyValue match {
    case value: Utf8 => int2Integer(java.lang.Integer.parseInt(value.toString))
    case value: String => int2Integer(java.lang.Integer.parseInt(value))
    case value: Integer => value
    case value: java.lang.Long => int2Integer(value.toInt)
    case value: java.lang.Float => int2Integer(value.toInt)
    case value: Int => int2Integer(value)
    case value: Long => int2Integer(value.toInt)
    case value: Float => int2Integer(value.toInt)
    case _ => throw new Exception(s"Couldn't find any converters to Integer for: $anyValue")
  })

  def projectLong(anyValue: Any): Try[java.lang.Long] = Try(anyValue match {
    case value: Utf8 => long2Long(java.lang.Long.parseLong(value.toString))
    case value: String => long2Long(java.lang.Long.parseLong(value))
    case value: Integer => long2Long(value.toLong)
    case value: java.lang.Long => value
    case value: java.lang.Float => long2Long(value.toLong)
    case value: Int => long2Long(value.toLong)
    case value: Long => long2Long(value)
    case value: Float => long2Long(value.toLong)
    case _ => throw new Exception(s"Couldn't find any converters to Float for: $anyValue")
  })

  def projectFloat(anyValue: Any): Try[java.lang.Float] = Try(anyValue match {
    case value: Utf8 => Float2float(java.lang.Float.parseFloat(value.toString))
    case value: String => Float2float(java.lang.Float.parseFloat(value))
    case value: Integer => Float2float(value.toFloat)
    case value: java.lang.Long => Float2float(value.toFloat)
    case value: java.lang.Float => value
    case value: Int => Float2float(value.toFloat)
    case value: Long => Float2float(value.toFloat)
    case value: Float => Float2float(value)
    case _ => throw new Exception(s"Couldn't find any converters to Float for: $anyValue")
  })

  def projectString(anyValue: Any): Try[java.lang.String] = Try(anyValue.toString)

}
