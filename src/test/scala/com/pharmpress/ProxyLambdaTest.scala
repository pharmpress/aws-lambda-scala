package com.pharmpress

import java.io.ByteArrayOutputStream

import cats.syntax.either._
import com.amazonaws.services.lambda.runtime.Context
import com.pharmpress.LambdaTest.Ping
import com.pharmpress.ProxyLambdaTest.{ ProxyCaseClassHandler, ProxyCaseClassHandlerWithError, ProxyRawHandler, ProxyRawHandlerWithError, _ }
import com.pharmpress.aws.handler.Lambda
import com.pharmpress.aws.proxy.{ Response, ProxyRequest, ProxyResponse }
import com.pharmpress.aws.handler.Lambda._
import io.circe.generic.auto._
import io.circe.parser._
import org.mockito.MockitoSugar
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should

import scala.concurrent.Future
import scala.io.Source

object ProxyLambdaTest {

  class ProxyRawHandler extends Lambda.Proxy[String, String] {
    override protected def handle(input: ProxyRequest[String]) =
      Right(ProxyResponse(200, None, Response(success = input.body.map(_.toUpperCase()))))
  }

  class ProxyRawHandlerWithError extends Lambda.Proxy[String, String] {

    override protected def handle(i: ProxyRequest[String]): Either[Throwable, ProxyResponse[String]] = Left(
      new Error("Could not handle this request for some obscure reasons")
    )
  }

  class ProxyCaseClassHandler extends Lambda.Proxy[Ping, Pong] {
    override protected def handle(input: ProxyRequest[Ping]) = {
      val result: Option[Pong] = input.body.map { ping =>
        Pong(ping.inputMsg.length.toString)
      }

      Right(
        ProxyResponse(200, None, Response(success = result)
        ))
    }
  }

  class ProxyCaseClassHandlerWithError extends Lambda.Proxy[Ping, Pong] {
    override protected def handle(input: ProxyRequest[Ping]) = Left(
      new Error("Oh boy, something went wrong...")
    )
  }

  case class Ping(inputMsg: String)

  case class Pong(outputMsg: String)
}

class ProxyLambdaTest extends AnyFunSuite with should.Matchers with MockitoSugar with Eventually {

  test("should handle request and response classes with body of raw type") {

    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-raw.json")
    val s       = Source.fromURL(jsonUrl)

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    new ProxyRawHandler().handle(is, os, mock[Context])

    os.toString should startWith("{")
    os.toString should include("RAW-BODY")
    os.toString should endWith("}")
  }

  test("should handle request and response classes with body of case classes") {
    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-case-class.json")
    val s       = Source.fromURL(jsonUrl)

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    new ProxyCaseClassHandler().handle(is, os, mock[Context])

    os.toString should startWith("{")
    os.toString should include("{\\\"outputMsg\\\":\\\"4\\\"}")
    os.toString should endWith("}")
  }

  test("should generate error response in case of error in raw handler") {
    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-raw.json")
    val s       = Source.fromURL(jsonUrl)

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    new ProxyRawHandlerWithError().handle(is, os, mock[Context])

    val response = decode[ProxyResponse[String]](os.toString)
    response shouldEqual Right(
      ProxyResponse(
        500,
        Some(Map("Content-Type" -> s"text/plain; charset=UTF-8")),
        Response(error = Some("Could not handle this request for some obscure reasons"))
      ))
  }

  test("should generate error response in case of error in case class handler") {
    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-case-class.json")
    val s       = Source.fromURL(jsonUrl)

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    new ProxyCaseClassHandlerWithError().handle(is, os, mock[Context])

    val response = decode[ProxyResponse[String]](os.toString)

    response shouldEqual Right(
      ProxyResponse(
        500,
        Some(Map("Content-Type" -> s"text/plain; charset=UTF-8")),
        Response(error = Some("Oh boy, something went wrong..."))
      ))
  }

  test("should support Future as output") {
    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-case-class.json")
    val s       = Source.fromURL(jsonUrl)

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    val context = mock[Context]
    when(context.getRemainingTimeInMillis).thenReturn(500 /*ms*/ )

    val function: (ProxyRequest[Ping], Context) => Either[Throwable, ProxyResponse[Future[Pong]]] =
      (_: ProxyRequest[Ping], _) => Right(ProxyResponse.success(Some(Future.successful(Pong("4")))))
    Lambda.Proxy.instance(function).handle(is, os, context)

    eventually {
      os.toString should startWith("{")
      os.toString should include("{\\\"outputMsg\\\":\\\"4\\\"}")
      os.toString should endWith("}")
    }
  }

  test("should support failed Future as output") {
    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-case-class.json")
    val s       = Source.fromURL(jsonUrl)

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    val context = mock[Context]
    when(context.getRemainingTimeInMillis).thenReturn(500 /*ms*/ )

    Lambda.Proxy
      .instance((_: ProxyRequest[Ping], _: Context) => {
        val response = ProxyResponse.success(Some(Future.failed[String](new RuntimeException("Oops"))))
        Either.right(response)
      })
      .handle(is, os, context)

    eventually {
      val response = decode[ProxyResponse[String]](os.toString)
      response shouldEqual Either.right(
        ProxyResponse(
          500,
          Some(Map("Content-Type" -> s"text/plain; charset=UTF-8")),
          Response(error = Some("Oops"))
        ))
    }
  }

  test("should support returning Units") {
    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-units.json")
    val s       = Source.fromURL(jsonUrl)

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    val context = mock[Context]
    when(context.getRemainingTimeInMillis).thenReturn(500 /*ms*/ )

    Lambda.Proxy
      .instance[None.type, None.type]((_, _) => {
        val response = ProxyResponse[None.type](
          statusCode = 200,
          body = Response[None.type]()
        )
        Either.right(response)
      })
      .handle(is, os, context)

    eventually {
      val response = decode[ProxyResponse[None.type]](os.toString)
      response shouldEqual Either.right(
        ProxyResponse(
          statusCode = 200,
          body = Response[None.type]()
        ))
    }
  }

}
