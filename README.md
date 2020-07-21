Writing a handler for AWS lambda in Scala can be as easy as...

```scala
import io.circe.generic.auto._
import com.pharmpress.aws.handler.Lambda._
import com.amazonaws.services.lambda.runtime.Context

case class Ping(inputMsg: String)

case class Pong(outputMsg: String)

class PingPongHandler extends Lambda[Ping, Pong] {

  override def handle(ping: Ping, context: Context) = Right(Pong(ping.inputMsg.reverse))

}
```
The input JSON will be automatically de-serialized into `Ping`, and the output into `Pong`. The `handle()` method is supposed to return `Either[Throwable, Pong]`: `Right` if the input was handled correctly, and `Left` otherwise. 

This handler can be used in AWS Lambda as: `io.github.mkotsur.example::handle`.

Features:

* Return Futures right from the handler!
* JSON (de)serialization of case classes;
* Plain strings are supported too;
* [AWS API Gateway proxy integration](http://docs.aws.amazon.com/apigateway/latest/developerguide/integrating-api-with-aws-services-lambda.html);
* Uncaught errors are logged with SLF4J and re-thrown.

More docs are coming soon... Feel free to look at `src/test/scala` if you want to use it right now.

## Examples

### Returning futures

```scala
import io.circe.generic.auto._
import com.pharmpress.aws.handler.Lambda._
import com.amazonaws.services.lambda.runtime.Context
import com.pharmpress.aws.handler.Lambda

import scala.concurrent.Future

case class Ping(inputMsg: String)

class PingFuturePongHandler extends Lambda[Ping, Future[Int]] {

  override def handle(ping: Ping, context: Context) = 
    Right(Future.successful(ping.inputMsg.length))

}
```

### Not receiving and not returning any value

This lambda will accept an empty string, or string with `null` as an input.

```scala
import io.circe.generic.auto._
import com.pharmpress.aws.handler.Lambda._
import com.amazonaws.services.lambda.runtime.Context

class NothingToNothingHandler extends Lambda[None.type, None.type] {

  override def handle(_: None.type , context: Context) = {
    println("Only sinde effects") 
    Right(None)
  }
    
}
```

### StackOverflow Error

When using the AWS Scala library, you might get the following error:

```
[error] (monograph / Compile / compileIncremental) java.lang.StackOverflowError
```

This is due to a known [issue](https://github.com/circe/circe/issues/1281) in circle. A simple fix is to increase the JVM Stack size:

```
sbt -J-Xss64m
```
