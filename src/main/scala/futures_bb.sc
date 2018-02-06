import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

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

val first = slowAddition(1, 1)
val second = slowAddition(2, 2)
val third = slowerAddition(3, 3)

// 1) Simple Futures - async programming, ec, return type, awaits

// 2) Combining Futures - map, flatMap, for-comp (and parallel running)

// 3) Collections

// 4) Error handling

// 5) Anything else??
