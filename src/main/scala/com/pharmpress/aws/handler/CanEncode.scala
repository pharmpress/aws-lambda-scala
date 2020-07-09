package com.pharmpress.aws.handler

import Lambda.WriteStream

trait CanEncode[O] {
  def writeStream: WriteStream[O]
}

object CanEncode {

  def apply[A: CanEncode]: CanEncode[A] =
    implicitly[CanEncode[A]]

  def instance[A](func: WriteStream[A]): CanEncode[A] = new CanEncode[A] {
    override def writeStream: WriteStream[A] = func
  }

}
