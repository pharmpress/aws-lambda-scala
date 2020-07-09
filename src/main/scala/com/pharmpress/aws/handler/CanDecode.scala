package com.pharmpress.aws.handler

import com.pharmpress.aws.handler.Lambda.ReadString

trait CanDecode[I] {
  def readString: ReadString[I]
}

object CanDecode {

  def apply[A: CanDecode]: CanDecode[A] =
    implicitly[CanDecode[A]]

  def instance[A](func: ReadString[A]): CanDecode[A] = new CanDecode[A] {
    override def readString: ReadString[A] = func
  }


}
