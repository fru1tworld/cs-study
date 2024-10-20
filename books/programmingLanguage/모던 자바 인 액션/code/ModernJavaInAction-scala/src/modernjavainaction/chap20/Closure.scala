package modernjavainaction.chap20

object Closure {

  def main(args: Array[String]) {
    var count = 0
    val inc = () => count += 1
    inc()
    println(count)
    inc()
    println(count)
  }

}