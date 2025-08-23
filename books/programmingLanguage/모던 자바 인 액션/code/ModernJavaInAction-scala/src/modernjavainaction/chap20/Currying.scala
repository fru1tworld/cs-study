package modernjavainaction.chap20

object Currying {

  def main(args: Array[String]) {
    def multiply(x : Int, y: Int) = x * y
    val r1 = multiply(2, 10)
    println(r1)

    def multiplyCurry(x :Int)(y : Int) = x * y
    val r2 = multiplyCurry(2)(10)
    println(r2)

    val multiplyByTwo : Int => Int = multiplyCurry(2)
    val r3 = multiplyByTwo(10)
    println(r3)
  }

}