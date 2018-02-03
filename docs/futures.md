#### [Back to home page](index.md)
# Working with Futures
### Contents:
* [Intro](#asynchronous-programming)
* [Basics of using Futures](#using-futures)
* [Managing lots of Futures](#combining-collections-of-futures)
* [Transforming the result](#other-methods-on-futures)
* [Error handling](#error-handling)
* [Testing with Futures](#testing-with-futures)
* [Execution contexts](#execution-contexts-and-thread-pools)
* [References](#references)

### Function cheat sheet:

| Name | Description |
| --- | --- |
| [Map](#map) | Transform the result of a Future |
| [FlatMap](#flatMap) | Transform the result of a Future to another Future |
| [For Comprehensions](#for-comprehensions) | An easy way to use Map and FlatMap |
| [Sequence](#sequence) | Transform a collection of Futures to a Future of a collection |
| [Fold](#fold) | Collapse a collection of Futures to a Future of a single value |
| [Traverse](#traverse) | A generalised version of Sequence |

## Asynchronous programming
A simple _synchronous_ application will run all commands one after the other on the main app _thread_.

This is okay in simple cases but: 
* It doesn't make best use of system resources - modern computers have enough processing power to run many commands at the same time
* The client/user calling a block of synchronous code has to wait for the response to come back before they can continue

_Asynchronous_ programming allows us to get around these issues by allowing multiple operations to be run in parallel

Examples of when asynchronous programming can be useful:
* CPU intensive operations
* I/O operations
* Http requests

Traditionally writing asynchronous code has been tedious and difficult - which is why we have Futures!

## Using Futures

In Scala a `Future` is an abstraction that makes it easy to write async code.

Any block of code can be made to run asynchronously by wrapping it in a Future. For example:
```scala
scala> import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
 
scala> import scala.concurrent.Future
import scala.concurrent.Future
 
scala> val fut = Future{ 21 - 1 }
fut: scala.concurrent.Future[Int] = Future(<not completed>)
 
scala> fut.value
res1: Option[scala.util.Try[Int]] = Some(Success(20))
```
 
A few things to note:
* The import brings in the default Scala _ExecutionContext_. Any Future code needs an ExecutionContext to tell it how to run.
Usually you don't need to worry too much about this but see [Execution contexts](#execution-contexts-and-thread-pools) for the details.
* The computation `21-1` is then computed asynchronously - on creation we don't know the result (\<not completed>).
* Checking back later with `.value` gives us the result. It has type `Option[Try[Int]]` because:
    * If the Future had not completed it would return `None`
    * The Future can complete with either a `Success` or `Failure`


There may be some situations you want to construct a Future that has already been completed (mainly in testing). This can
be done with the `Future.successful` and `Future.failed` methods:
```scala
scala> Future.successful(21+1).value
res8: Option[scala.util.Try[Int]] = Some(Success(22))
 
scala> Future.failed(new Throwable("Test")).value
res9: Option[scala.util.Try[Nothing]] = Some(Failure(java.lang.Throwable: Test))
```
This has the same result as just using Future but is slightly more efficient since it doesn't have to manage the asynchronous calculation.
(Note the second case didn't throw a fatal exception. See [Error handling](#error-handling))

### Awaiting
In general you should *never* have an `Await.result` (or `Await.ready`) in your production code (it actually says so in the documentation).

Doing this will block the main thread to wait for the result of the Future, defeating the point of using it in the first place.

Even if you have a good reason to there is usually a better way. Most simple cases should hopefully be covered in here somewhere.

### Map
In most cases we will want to do something with the result of the future. Using a `map` makes it possible to do this without
having to block and wait for the result of the Future

Map has the following signature:
```scala
 def map[S](f: T => S)(implicit executor: ExecutionContext): Future[S]
```
* `T` is the type of the Future. In all examples here `T` = `Int`
* `S` is the type of the Future after the transformation. This can be anything and isn't related to `T`

So for a map all we need is to provide a function `f: T => S`. Here's a simple example:

```scala
scala> def double(x: Int): Int = x*2
double: (x: Int)Int
 
scala> fut
res2: scala.concurrent.Future[Int] = Future(Success(20))
 
scala> fut2 = fut.map(double)
res3: scala.concurrent.Future[Int] = Future(<not completed>)
 
scala> fut2.value
res4: Option[scala.util.Try[Int]] = Some(Success(40))
 
scala> val fut3 = fut2.map(res => "Result:" + res.toString)
fut3: scala.concurrent.Future[String] = Future(<not completed>)
 
scala> fut3.value
res5: Option[scala.util.Try[String]] = Some(Success(Result:40))
```
Mapping will actually return a _new_ Future containing the result of mapping on the first.

### FlatMap
What if we want to run one bit of asynchronous code after another. To chain two methods returning Futures you can use a `flatMap`.

FlatMap has the following signature:
```scala
 def flatMap[S](f: T => Future[S])(implicit executor: ExecutionContext): Future[S]
```
Which is almost exactly the same as `map` except the function `f` returns a `Future[S]` instead of just `S`.
Here's another example:
```scala
scala> def doubleAsync(x: Int): Future[Int] = Future{x*2}
doubleAsync: (x: Int)scala.concurrent.Future[Int]
 
scala> val fut1 = Future{21 + 1}
fut1: scala.concurrent.Future[Int] = Future(<not completed>)
 
scala> val fut2 = fut1.flatMap(doubleAsync)
fut2: scala.concurrent.Future[Int] = Future(<not completed>)
 
scala> fut2.value
res6: Option[scala.util.Try[Int]] = Some(Success(44))
```

Map and FlatMap can be combined to create more complex methods:
```scala
scala> val fut1 = Future{21 + 1}
fut1: scala.concurrent.Future[Int] = Future(<not completed>)
 
scala> val fut2 = Future{31 + 2}
fut2: scala.concurrent.Future[Int] = Future(<not completed>)
 
scala> val res = fut1.flatMap{ res1 => fut2.map { res2 => res1 + res2 } }
res: scala.concurrent.Future[Int] = Future(<not completed>)
 
scala> res.value
res7: Option[scala.util.Try[Int]] = Some(Success(55))
```

### For-Comprehensions
Map and FlatMap are great, but they can get messy to use in complex flows. For example:

```scala
scala> val fut9 = Future(21)
fut9: scala.concurrent.Future[Int] = Future(<not completed>)
 
scala> val fut10 = Future(32)
fut10: scala.concurrent.Future[Int] = Future(<not completed>)
 
scala> val fut11 = Future(43)
fut11: scala.concurrent.Future[Int] = Future(<not completed>)
 
scala> val result = fut9.flatMap { res1 =>
     |   fut10.flatMap { res2 =>
     |     fut11.map { res3 =>
     |       (res1 + res2) * res3
     |     }
     |   }
     | }
result: scala.concurrent.Future[Int] = Future(<not completed>)
 
scala> result.value
res23: Option[scala.util.Try[Int]] = Some(Success(2279))
```

This can be cleaned up using a _for-comprehension_:
```scala
scala> val result2 = for {
     |   res1 <- fut9
     |   res2 <- fut10
     |   res3 <- fut11
     | } yield {
     |   (res1 + res2) * res3
     | }
result2: scala.concurrent.Future[Int] = Future(<not completed>)
 
scala> result2.value
res24: Option[scala.util.Try[Int]] = Some(Success(2279))
```
Internally this will "flatten" the Futures using `map` and `flatMap` as appropriate, but can be much cleaner and easier to read.

#### Important note:
In this example the 3 Futures were defined _before_ the for-comprehension.

This means a thread will start computing the result(s) as soon as each value is assigned and all 3 will run in parallel.

If instead they were defined inside the for-comprehension they would actually be run in series (i.e. work wouldn't start on computing `fut10` until `fut9` had finished and so-on).
The same applies to using `map` and `flatMap`.

If one Future depends on the result of another they will be run in series automatically so you don't need to worry.
For example:
```scala
scala> val result3 = for {
     |   res1 <- fut11
     |   res2 <- doubleAsync(res1)
     | } yield {
     |   res2
     | }
result3: scala.concurrent.Future[Int] = Future(<not completed>)
 
scala> result3.value
res25: Option[scala.util.Try[Int]] = Some(Success(86))
```
would _always_ wait for `fut11` to complete before computing `doubleAsync(res1)` wherever it was defined.

## Combining collections of Futures
Quite often you can end up dealing with a lot of Futures running at the same time. To keep track of them all Scala has
a few functions to operate on entire collections of Futures

### Sequence
By far the most common (in my experience anyway) is `Future.sequence`. This lets you transform a collection of Futures
into a Future of a collection, which is usually much easier to work with.
```scala
scala> val seqOfFutures: Seq[Future[Int]] = Seq(
     |   Future(11 + 2),
     |   Future(22 + 3),
     |   Future(33 + 4),
     |   Future(44 + 5)
     | )
seqOfFutures: Seq[scala.concurrent.Future[Int]] = List(Future(Success(13)), Future(Success(25)), Future(Success(37)), Future(Success(49)))
 
scala> val result4 = Future.sequence(seqOfFutures)
result4: scala.concurrent.Future[Seq[Int]] = Future(<not completed>)
 
scala> result4.value
res26: Option[scala.util.Try[Seq[Int]]] = Some(Success(List(13, 25, 37, 49)))
```
This will evaluate every Future in the collection at the same time on different threads (or at least as many as it can)
and return a Future containing the result of them all in a collection. If _any_ of them fail the whole Future will fail.

### Fold
The `Future.fold` method will collapse or "fold" a collection of Futures down to a single Future
```scala
scala> val result5 = Future.fold(seqOfFutures)(10) { (x, y) =>
     |   x + y
     | }
result5: scala.concurrent.Future[Int] = Future(<not completed>)
 
scala> result5.value
res27: Option[scala.util.Try[Int]] = Some(Success(134))
```
This will run all Futures serially, using the result of one to feed into the next.

Note that this is deprecated for `foldLeft` in Scala-2.12. It works basically the same but _requires_ an immutable collection
```scala
scala> val seqOfFutures2: immutable.Iterable[Future[Int]] = scala.collection.immutable.Iterable(
     |   Future(11 + 2),
     |   Future(22 + 3),
     |   Future(33 + 4),
     |   Future(44 + 5)
     | )
seqOfFutures2: scala.collection.immutable.Iterable[scala.concurrent.Future[Int]] = List(Future(Success(13)), Future(Success(25)), Future(Success(37)), Future(Success(49)))
 
scala> val result6 = Future.foldLeft(seqOfFutures2)(10){ (x, y) =>
     |   x + y
     | }
result6: scala.concurrent.Future[Int] = Future(<not completed>)
 
scala> result6.value
res30: Option[scala.util.Try[Int]] = Some(Success(134))
```

### Traverse
This one is less likely to be useful, but is basically a generalised `Future.sequence`. The difference is you start with a
collection of whatever you like and give a function to turn each into a Future. Then it sequences the result:
```scala
scala> val seqOfInt = Seq(
     |   11 + 2,
     |   22 + 3,
     |   33 + 4,
     |   44 + 5
     | )
seqOfInt: Seq[Int] = List(13, 25, 37, 49)
 
scala> val result7 = Future.traverse(seqOfInt)(x => Future.apply(x))
result7: scala.concurrent.Future[Seq[Int]] = Future(<not completed>)
 
scala> result7.value
res31: Option[scala.util.Try[Seq[Int]]] = Some(Success(List(13, 25, 37, 49)))
```

Note that `result4` (the sequence) is the same as `result7` (the traverse). That's because:
```scala
Future.sequence(seqOfFutures) === Future.traverse(seqOfFutures)(identity)
```
## Other methods on Futures
### Filter
### Collect
### Transform

## Error handling

## Testing with futures
### Awaiting the result
### Async testing with ScalaTest

## Execution contexts and thread pools

## References
* [Programming in Scala](https://booksites.artima.com/programming_in_scala_3ed) (3rd edition)
