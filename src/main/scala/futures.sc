import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Example 1
val fut1 = Future {
  21 - 1
}

fut1.value

// Example 2
Future.successful(21 + 1).value

Future.failed(new Throwable("Test")).value


// Example 3
def double(x: Int): Int = x * 2

fut1

val fut2 = fut1.map(double)

fut2.value

val fut3 = fut2.map(res => "Result:" + res.toString)

fut3.value


// Example 4
def doubleAsync(x: Int): Future[Int] = Future {
  x * 2
}

val fut4 = Future {
  21 + 1
}

val fut5 = fut4.flatMap(doubleAsync)

fut5.value


// Example 5
val fut6 = Future {
  21 + 5
}

val fut7 = Future {
  31 + 2
}

val fut8 = fut6.flatMap { res1 => fut7.map { res2 => res1 + res2 } }

fut8.value


// Example 6
val fut9 = Future(21)
val fut10 = Future(32)
val fut11 = Future(43)

val result = fut9.flatMap { res1 =>
  fut10.flatMap { res2 =>
    fut11.map { res3 =>
      (res1 + res2) * res3
    }
  }
}

result.value


// Example 7
val result2 = for {
  res1 <- fut9
  res2 <- fut10
  res3 <- fut11
} yield {
  (res1 + res2) * res3
}

result2.value


// Example 8
val result3 = for {
  res1 <- fut11
  res2 <- doubleAsync(res1)
} yield {
  res2
}

result3.value

// Example 9
val seqOfFutures: Seq[Future[Int]] = Seq(
  Future(11 + 2),
  Future(22 + 3),
  Future(33 + 4),
  Future(44 + 5)
)

val result4 = Future.sequence(seqOfFutures)

result4.value


//noinspection ScalaDeprecation
// Example 10
val result5 = Future.fold(seqOfFutures)(10){ (x, y) =>
  x + y
}

result5.value


//Example 11
val seqOfFutures2: immutable.Iterable[Future[Int]] = scala.collection.immutable.Iterable(
  Future(11 + 2),
  Future(22 + 3),
  Future(33 + 4),
  Future(44 + 5)
)

val result6 = Future.foldLeft(seqOfFutures2)(10){ (x, y) =>
  x + y
}

result6.value


// Example 12
val seqOfInt = Seq(
  11 + 2,
  22 + 3,
  33 + 4,
  44 + 5
)

val result7 = Future.traverse(seqOfInt)(x => Future.apply(x))

result7.value


