package fr.xebia.gbildi.tfcompat

import java.lang

import com.sksamuel.avro4s.{Record, RecordFormat}
import fr.xebia.gbildi.tfcompat.AvroTensor.AvroTensorSyntax._
import fr.xebia.gbildi.tfcompat.ScalarTensor.ScalarTensorSyntax._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.tensorflow.Tensor

/**
 * Created by loicmdivad.
 */
class AvroTensorSpec extends AnyFlatSpec with Matchers {

  "AvroTensor" should "transform a avro string into TF tensor" in {

    val input: String = "This is a test"

    val result: Tensor[String] = input.toTensor

    result.numBytes() shouldBe 23
    result.numElements() shouldBe 1
    result.numDimensions() shouldBe 1
  }

  it should "transform a avro float into TF tensor" in {

    val input: Float = 24F

    val result: Tensor[lang.Float] = float2Float(input).toTensor

    result.numBytes() shouldBe 4
    result.numElements() shouldBe 1
    result.numDimensions() shouldBe 1
  }

  it should "transform a avro long into TF tensor" in {

    val input: Long = 24L

    val result: Tensor[lang.Long] = long2Long(input).toTensor

    result.numBytes() shouldBe 8
    result.numElements() shouldBe 1
    result.numDimensions() shouldBe 1
  }

  it should "transform objects from avro records into tf tensor" in {

    val input: Record = RecordFormat[TestPojo].to(TestPojo("test value", 24, 6))

    val result1: Tensor[String] =  input.get("key1").toString.castTensor.expect(classOf[String])
    val result2: Tensor[java.lang.Long] =  input.get("key2").castTensor.expect(classOf[java.lang.Long])
    val result3: Tensor[java.lang.Float] =  input.get("key3").castTensor.expect(classOf[java.lang.Float])

    result1.headOption should contain("test value")
    result2.headOption should contain(24)
    result3.headOption should contain(6)
  }
}
