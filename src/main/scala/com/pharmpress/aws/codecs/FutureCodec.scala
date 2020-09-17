package com.pharmpress.aws.codecs

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

import com.pharmpress.aws.handler.CanEncode
import com.pharmpress.aws.proxy.{ ProxyResponse, Response }
import io.circe.{ Encoder, Json, _ }
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{ Failure, Success, Try }
import io.circe.parser.decode
import io.circe.syntax._


private[aws] trait FutureCodec {
  implicit def canEncodeFuture[I: Encoder](implicit canEncode: Encoder[I]) =
    CanEncode.instance[Future[I]]((os, responseEither, ctx) => {
      (for {
        response     <- responseEither.toTry
        futureResult <- Try(Await.result(response, ctx.getRemainingTimeInMillis millis))
        json         <- Try(canEncode(futureResult).noSpaces.getBytes)
        _            <- Try(os.write(json))
      } yield {
        ()
      }) match {
        case Success(v) => Right(v)
        case Failure(e) => Left(e)
      }
    })

  implicit def canEncodeProxyResponse[T](implicit canEncode: CanEncode[T]): CanEncode[ProxyResponse[T]] = CanEncode.instance[ProxyResponse[T]](
    func = (output, proxyResponseEither, ctx) => {

      def writeBody(body: Response[T])(implicit encoder: Encoder[Response[T]]): Either[Throwable, (Option[String], Option[String])] = {
        val successOpt: Either[Throwable, Option[String]] = body.success match {
          case None => Right[Throwable, Option[String]](None)
          case Some(success) =>
            val os = new ByteArrayOutputStream()
            val result: Either[Throwable, Unit] = canEncode.writeStream(os, Right(success), ctx)
            os.close()
            result.map(_ => Some(os.toString()))
        }

        for {
          success <- successOpt
        } yield {
          (success, body.error)
        }
      }

      implicit def encoderProxyResponseHandler: Encoder[Response[T]] =
        (metadata: Response[T]) =>
          metadata.error match {
            case Some(response: String) =>
              Json.obj(("error", Json.fromString(response)))
            case None =>
              metadata.success.asJson
          }

      val proxyResposeOrError: Either[Throwable, ProxyResponse[String]] = for {
        proxyResponse <- proxyResponseEither
        tuple <- writeBody(proxyResponse.body)
      } yield
        ProxyResponse[String](
          proxyResponse.statusCode,
          proxyResponse.headers,
          Response[String](tuple._1, tuple._2)
        )

      val response = proxyResposeOrError match {
        case Right(proxyRespose) =>
          proxyRespose
        case Left(e) =>
          ProxyResponse[String](
            500,
            Some(Map("Content-Type" -> s"text/plain; charset=${Charset.defaultCharset().name()}")),
            Response(error = Some(e.getMessage))
          )
      }


      output.write(response.asJson.noSpaces.getBytes)

      Right(())
    }
  )
}
