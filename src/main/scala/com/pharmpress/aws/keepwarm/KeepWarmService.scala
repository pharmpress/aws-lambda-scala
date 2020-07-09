package com.pharmpress.aws.keepwarm

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import com.amazonaws.services.lambda.AWSLambdaAsyncClientBuilder
import com.amazonaws.services.lambda.model.{ InvokeRequest, InvokeResult }
import io.circe.generic.auto._
import io.circe.parser
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ Await, Future }

abstract class KeepWarmService {

  private lazy val log = LoggerFactory.getLogger(this.getClass)

  private lazy val lambdaFunctionName: String = sys.env("AWS_LAMBDA_FUNCTION_NAME")

  def keepWarm(input: String): Unit = {
    parser.decode[KeepWarm](input) match {
      case Right(keepWarm) =>
        log.info(s"Received keep warm message with concurrency ${keepWarm.concurrency.getOrElse("---")}")
        concurrentExecutionHandler(keepWarm.concurrency)
      case _ =>
        ()
    }
  }

  private def concurrentExecutionHandler(concurrency: Option[Int]): Unit = {
    concurrency match {
      case Some(count: Int) =>
        for(_ <- 1 until count) {
          Await.result(invokeLambda(), FiniteDuration(10, TimeUnit.SECONDS))
        }

      case None =>
        log.info("Received a concurrent keep warm message")
        ()
    }
  }

  private def invokeLambda(): Future[InvokeResult] = {
    val ayncLambdaClient = AWSLambdaAsyncClientBuilder.defaultClient()

    val asyncRequest = new InvokeRequest
    asyncRequest.setFunctionName(s"$lambdaFunctionName")
    asyncRequest.withPayload("""{ "warmer":true }""")

    Future {
      ayncLambdaClient.invokeAsync(asyncRequest, new AsyncLambdaHandler()).get()
    }
  }

}
