package com.pharmpress.aws.keepwarm

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.lambda.model.{ InvokeRequest, InvokeResult }
import org.slf4j.LoggerFactory

class AsyncLambdaHandler extends AsyncHandler[InvokeRequest, InvokeResult] {

  private lazy val log = LoggerFactory.getLogger(this.getClass)

  def onSuccess(req: InvokeRequest, res: InvokeResult): Unit = {
    log.info(s"Lambda function returned: ${new String(res.getPayload.array())} from ${new String(req.getPayload.array())}")
    ()
  }

  def onError(e: Exception): Unit = {
    log.error(e.getMessage)
    ()
  }
}
