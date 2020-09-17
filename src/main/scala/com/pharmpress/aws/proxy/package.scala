package com.pharmpress.aws

import io.circe.syntax.EncoderOps
import io.circe.{ Encoder, Json }

package object proxy {

  case class RequestContextAuthorizer(
    principalId: String,
    username: String,
    email: String,
    roles: String
  )

  case class RequestContext(
    authorizer: Option[RequestContextAuthorizer] = None
  )

  case class RequestInput(body: String)

  case class ProxyRequest[T](
    path: String,
    pathParameters: Option[Map[String, String]] = None,
    httpMethod: String,
    headers: Option[Map[String, String]] = None,
    queryStringParameters: Option[Map[String, String]] = None,
    stageVariables: Option[Map[String, String]] = None,
    requestContext: RequestContext = RequestContext(),
    body: Option[T] = None
  )

  case class ProxyResponse[T](
    statusCode: Int,
    headers: Option[Map[String, String]] = None,
    body: Response[T] = Response(None, None)
  )

  case class Response[T](success: Option[T] = None, error: Option[String] = None)

  implicit def encoderProxyResponseHandler[T: Encoder]: Encoder[Response[T]] =
    (metadata: Response[T]) =>
      metadata.error match {
        case Some(response) =>
          Json.obj(("error", Json.fromString(response)))
        case None =>
          metadata.success.asJson
      }

  object ProxyResponse {
    private val headers = Some(Map("Access-Control-Allow-Origin" -> "*"))

    def success[B](body: Option[B] = None): ProxyResponse[B] = ProxyResponse[B](
      statusCode = 200,
      headers = headers,
      body = Response(success = body)
    )

    def created[B](body: Option[B] = None): ProxyResponse[B] = ProxyResponse[B](
      statusCode = 201,
      headers = headers,
      body = Response(success = body)
    )

    def badRequest[B](err: Option[String] = None): ProxyResponse[B] = ProxyResponse[B](
      statusCode = 400,
      headers = headers,
      body = Response(error = err)
    )

    def unauthorized[B](err: Option[String] = None): ProxyResponse[B] = ProxyResponse[B](
      statusCode = 401,
      headers = headers,
      body = Response(error = err)
    )

    def forbidden[B](err: Option[String] = None): ProxyResponse[B] = ProxyResponse[B](
      statusCode = 403,
      headers = headers,
      body = Response(error = err)
    )

    def notFound[B](err: Option[String] = None): ProxyResponse[B] = ProxyResponse[B](
      statusCode = 404,
      headers = headers,
      body = Response(error = err)
    )

    def methodNotAllowed[B](err: Option[String] = None): ProxyResponse[B] = ProxyResponse[B](
      statusCode = 405,
      headers = headers,
      body = Response(error = err)
    )
  }

}
