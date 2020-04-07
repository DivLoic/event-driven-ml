package fr.xebia.gbildi.tfcompat

import java.lang

import fr.xebia.gbildi.tfcompat.ScalarTensor.ScalarTensorSyntax._
import fr.xebia.gbildi.tfcompat.AvroTensor.AvroTensorSyntax._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.tensorflow.Tensor

/**
 * Created by loicmdivad.
 */
class ScalarTensorSpec extends AnyFlatSpec with Matchers {

  "TensorHead" should "extract the first and only element of tensor as String" in {

    val input = "This is a test".toTensor

    val result: Option[String] = input.headOption

    result should contain("This is a test")
  }

  it should "extract the first and only element of tensor as Float" in {

    val input: Tensor[lang.Float] = float2Float(24F).toTensor

    val result = input.headOption

    result should contain(24F)
  }

  it should "extract the first and only element of tensor as Long" in {

    val input: Tensor[lang.Long] = long2Long(24L).toTensor

    val result = input.headOption

    result should contain(24L)
  }
}
