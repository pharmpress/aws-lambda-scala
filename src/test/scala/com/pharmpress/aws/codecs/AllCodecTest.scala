package com.pharmpress.aws.codecs

import java.io.ByteArrayOutputStream

import com.amazonaws.services.lambda.runtime.Context
import org.mockito.MockitoSugar
import org.scalatest.EitherValues._
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should

class AllCodecTest extends AnyFunSuite with should.Matchers with MockitoSugar with Eventually {

  test("should decode null") {
    new AllCodec {
      val is = """null"""

      val value = canDecodeAll[None.type].readString(is)
      value.right.value shouldBe Option.empty[None.type]
    }
  }

  test("should decode empty string") {
    new AllCodec {
      val is = ""

      val value = canDecodeAll[None.type].readString(is)
      value.right.value shouldBe Option.empty[None.type]
    }
  }

  test("should encode null") {
    new AllCodec {
      val os = new ByteArrayOutputStream()

      val context: Context = mock[Context]

      canEncodeAll[None.type].writeStream(os, Right(None), context)
      os.toString shouldBe "null"
    }
  }

}
