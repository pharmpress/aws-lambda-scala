package com.pharmpress.aws

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
    body: Option[T] = None
  )

  object ProxyResponse {
    private val headers = Some(Map("Access-Control-Allow-Origin" -> "*"))

    def success[B](body: Option[B] = None): ProxyResponse[B] = ProxyResponse[B](
      statusCode = 200,
      headers = Some(Map("Access-Control-Allow-Origin" -> "*")),
      body = body
    )

    def created[B](body: Option[B] = None): ProxyResponse[B] = ProxyResponse[B](
      statusCode = 201,
      headers = headers,
      body = body
    )

    def badRequest[B](body: Option[B] = None): ProxyResponse[B] = ProxyResponse[B](
      statusCode = 400,
      headers = headers,
      body = body
    )

    def unauthorized[B](body: Option[B] = None): ProxyResponse[B] = ProxyResponse[B](
      statusCode = 401,
      headers = headers,
      body = body
    )

    def forbidden[B](body: Option[B] = None): ProxyResponse[B] = ProxyResponse[B](
      statusCode = 403,
      headers = headers,
      body = body
    )

    def notFound[B](body: Option[B] = None): ProxyResponse[B] = ProxyResponse[B](
      statusCode = 404,
      headers = headers,
      body = body
    )

    def methodNotAllowed[B](body: Option[B] = None): ProxyResponse[B] = ProxyResponse[B](
      statusCode = 405,
      headers = headers,
      body = body
    )
  }

}
