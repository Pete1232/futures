import org.scalatest.{AsyncWordSpec, MustMatchers}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class FuturesSpec extends AsyncWordSpec with MustMatchers {

  val testFuture: Future[Int] = Future.successful(32)
  val slowTestFuture: Future[Int] = Future.successful{
    Thread.sleep(3000)
    32
  }

  "ScalaTest" must {
    "be able to test async code by awaiting the result" in {
      Await.result(testFuture, 10 seconds) mustBe 32
    }
    "be able to test async code without awaits" in {
      testFuture.map(
        _ mustBe 32
      )
    }
    "be able to test slow async code without awaits" in {
      slowTestFuture.map(
        _ mustBe 32
      )
    }
  }
}
