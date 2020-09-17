package com.pharmpress.aws.codecs

import java.nio.charset.StandardCharsets.UTF_8

import com.pharmpress.aws.handler.{ CanDecode, CanEncode }
import com.pharmpress.aws.proxy.Response
import io.circe._
import io.circe.parser.decode
import io.circe.syntax._

import scala.reflect.ClassTag

private[aws] trait AllCodec {

  implicit def canDecodeAll[T: ClassTag](implicit decoder: Decoder[T]) =
    CanDecode.instance[T](
      implicitly[ClassTag[T]] match {
        case ct if ct.runtimeClass == classOf[String] =>
          str =>
            Right(str.asInstanceOf[T])
        case _ =>
          str =>
            decode[T](if (str.isEmpty) "null" else str)
      }
    )

  implicit def canEncodeAll[T: ClassTag](implicit encoder: Encoder[T]) = CanEncode.instance[T](
    implicitly[ClassTag[T]] match {
      case ct if ct.runtimeClass == classOf[String] =>
        (output, handledEither, _) =>
          handledEither.map { s =>
            output.write(s.asInstanceOf[String].getBytes)
          }

      case _ =>
        (output, handledEither, _) =>
          handledEither map { handled =>
            val jsonString = handled.asJson.noSpaces
            output.write(jsonString.getBytes(UTF_8))
          }
    }
  )

  implicit def encoderProxyResponseHandler[T: Encoder]: Encoder[Response[T]] =
    (metadata: Response[T]) =>
      metadata.error match {
        case Some(response: String) =>
          Json.obj(("error", Json.fromString(response)))
        case None =>
          metadata.success.asJson
      }
}
