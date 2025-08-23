package modernjavainaction.chap20

class Insurance(val name: String)
class Car(val insurance: Option[Insurance])
class Person(val car: Option[Car], val age: Int)

object OptionTest {

  def main(args: Array[String]) {
    val insurance = Option(new Insurance("Rock Bottom"))
    val car = Option(new Car(insurance))
    val person = new Person(car, 40)

    val search1 = getCarInsuranceName(Option(person), 25)
    println(search1)
    val search2 = getCarInsuranceName(Option(person), 50)
    println(search2)
  }

  def getCarInsuranceName(person: Option[Person], minAge: Int) =
    person.filter(_.age >= minAge)
        .flatMap(_.car)
        .flatMap(_.insurance)
        .map(_.name)
        .getOrElse("Unknown")

}