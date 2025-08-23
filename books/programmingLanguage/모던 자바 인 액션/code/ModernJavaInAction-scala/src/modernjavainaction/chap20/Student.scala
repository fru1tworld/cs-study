package modernjavainaction.chap20

class Student(var name: String, var id: Int)

object Student {

  def main(args: Array[String]) {
    val s = new Student("Raoul", 1)
    println(s.name)
    s.id = 1337
    println(s.id)

  }

}
