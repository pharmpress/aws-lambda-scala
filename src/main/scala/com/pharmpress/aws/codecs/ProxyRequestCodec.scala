package com.pharmpress.aws.codecs

import com.pharmpress.aws.handler.CanDecode
import com.pharmpress.aws.proxy.ProxyRequest
import io.circe.generic.auto._
import shapeless.Generic

import scala.language.{ higherKinds, postfixOps }

private[aws] trait ProxyRequestCodec extends AllCodec with FutureCodec {

  /**
    * This is a transformer between case classes and their generic representations [shapeless.HList].
    * Please check Shapeless guide (e.g. https://github.com/underscoreio/shapeless-guide) for more details.
    */
  def GenericProxyRequestOf[T] = shapeless.Generic[ProxyRequest[T]]

  implicit def canDecodeProxyRequest[T](implicit canDecode: CanDecode[T]) = CanDecode.instance[ProxyRequest[T]] { is =>
    {
      def extractBody(s: ProxyRequest[String]) = s.body match {
        case Some(bodyString) => canDecode.readString(bodyString).map(Option.apply)
        case None             => Right(None)
      }

      def produceProxyResponse(decodedRequestString: ProxyRequest[String], bodyOption: Option[T]) = {
        val reqList = Generic[ProxyRequest[String]].to(decodedRequestString)
        Generic[ProxyRequest[T]].from((bodyOption :: reqList.reverse.tail).reverse)
      }

      for (decodedRequest$String <- CanDecode[ProxyRequest[String]].readString(is);
           decodedBodyOption     <- extractBody(decodedRequest$String))
        yield produceProxyResponse(decodedRequest$String, decodedBodyOption)
    }
  }

}
