package modernjavainaction.chap20

trait Sized {
  var size: Int = 0
  def isEmpty() = size == 0
}

object SizedRunner {

  def main(args: Array[String]) {
    class Empty extends Sized
    println(new Empty().isEmpty())

    class Box
    val b1 = new Box() with Sized
    println(b1.isEmpty())
    val b2 = new Box()
    // Won't compile
    //b2.isEmpty()
  }

}