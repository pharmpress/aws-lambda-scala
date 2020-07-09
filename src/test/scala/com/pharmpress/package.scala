package com

import java.nio.charset.StandardCharsets

package object pharmpress {

  import java.io.{ ByteArrayInputStream, UnsupportedEncodingException }

  class StringInputStream @throws[UnsupportedEncodingException]
  (val string: String) extends ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)) {
    def getString: String = this.string
  }
}
