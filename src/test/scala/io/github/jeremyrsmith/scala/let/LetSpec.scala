package io.github.jeremyrsmith.scala.let

import org.scalatest.{FreeSpec, Matchers}

class LetSpec extends FreeSpec with Matchers {

  "single binding" in {
    val result = let(x = 10) in {
      x + 1
    }

    result shouldEqual 11
  }

  "multiple comma-separated bindings" in {
    val result = let (x = 10, y = 11) in {
      x + y
    }

    result shouldEqual 21
  }

  "braced bindings" in {
    val result = let {
      x = 10
      y = 11
    } in x + y

    result shouldEqual 21
  }

  "braced bindings with lazy val" in {
    var hit = false
    def access = {
      hit = true
      10
    }

    val result = let {
      lazy val x = access
      y = 20
    } in y

    result shouldEqual 20
    hit shouldEqual false

    val result2 = let {
      lazy val x = access
      y = 20
    } in x + y

    result2 shouldEqual 30
    hit shouldEqual true
  }

}
