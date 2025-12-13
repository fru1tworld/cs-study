package modernjavainaction.chap20

object Beer {

  def main(args: Array[String]) {
    imperative()
    functional()
  }

  def imperative() {
    var n: Int = 2
    while (n <= 6) {
      println(s"Hello ${n} bottles of beer")
      n += 1
    }
  }

  def functional() {
    2 to 6 foreach { n => println(s"Hello ${n} bottles of beer") }
  }

}
