import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

def currentTime = {
  OffsetDateTime.now()
    .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
}

def slowAddition(a: Int, b: Int): Int = {

  println(s"started adding $a to $b at $currentTime")

  Thread.sleep(2000)

  println(s"finished adding $a to $b at $currentTime")

  a + b
}

def slowerAddition(a: Int, b: Int): Int = {
  println(s"started adding $a to $b at $currentTime")

  Thread.sleep(3000)

  println(s"finished adding $a to $b at $currentTime")

  a + b
}

//val first = slowAddition(1, 1)
//val second = slowAddition(2, 2)
//val third = slowerAddition(3, 3)

// 1) Simple Futures - async programming, ec, return type, awaits
val firstFut = Future {
  slowAddition(1, 1)
}

//Await.result(firstFut, Duration.Inf)

val secFut = Future {
  slowAddition(2, 2)
}

//Await.result(secFut, Duration.Inf)

val thrdFut = Future {
  slowerAddition(3, 3)
}

Thread.sleep(3000)

//firstFut.value


//Await.result(thrdFut, Duration.Inf)

// 2) Combining Futures - map, flatMap, for-comp (and parallel running)

val res: Future[Int] = firstFut.map { x =>
  x + 5
}

Await.result(res, Duration.Inf)

val result: Future[Int] = firstFut.flatMap { x =>
  secFut.map { y =>
    x + y
  }
}

Await.result(result, Duration.Inf)

val numbers = Seq(1,2,3,4,5)

val seqResult = for {
  num <- numbers if num >= 4
} yield {
  3 * num
}

numbers.map { num =>
  3 * num
}

def run1Now = Future(slowAddition(1,2))
def run2Now = Future(slowAddition(3,4))

val resultAgain = for {
  x <- run1Now
  y <- run2Now
} yield {
  x + y
}

Await.result(resultAgain, Duration.Inf)

// 3) Collections
val sequence: Seq[Future[Int]] = for {
  num <- numbers
} yield {
  Future(num + 5)
}

Future.sequence(sequence)

Future.fold(sequence)(Future.successful(0)){ (left, right) =>
  left.map(_ + right)
}

Future.traverse(numbers)(x => Future.successful(x))

// 4) Error handling

Future.successful(5)

val badResult = Future(5/0)

badResult.value

val fixedResult = badResult.recover {
  case e: ArithmeticException => 400
  case _ => 500
}

fixedResult.value

Await.result(fixedResult, Duration.Inf)

// 5) Anything else??
