package com.pharmpress.aws.keepwarm

case class KeepWarm(
  warmer: Boolean,
  concurrency: Option[Int]
)
