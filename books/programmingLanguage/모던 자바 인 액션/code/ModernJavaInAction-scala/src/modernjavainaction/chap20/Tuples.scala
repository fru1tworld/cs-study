package modernjavainaction.chap20

object Tuples {

  def main(args: Array[String]) {
    val book = (2017, "Java in Action", "Manning")
    val numbers = (42, 1337, 0, 3, 14)
    println(book._1)
    println(numbers._4)
  }

}