package modernjavainaction.chap20

class Hello {

  def sayThankYou(){
    println("Thanks for reading our book")
  }

}

object Hello {

  def main(args: Array[String]) {
    val h = new Hello()
    h.sayThankYou()
  }

}
