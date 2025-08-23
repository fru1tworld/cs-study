package modernjavainaction.chap19

object PatternMatching {

  def main(args: Array[String]) = {
    val e1 = BinOp("+", new Number(5), new Number(0))
    println(simplifyExpression(e1))

    val e2 = BinOp("+", Number(5), BinOp("*", Number(3), Number(4)))
    val result = evaluate(e2)
    println(e2 + " = " + result)
  }

  abstract class Expr
  case class Number(value: Int) extends Expr
  case class BinOp(opname: String, left: Expr, right: Expr) extends Expr

  def simplifyExpression(expr: Expr): Expr = expr match {
    case BinOp("+", e, Number(0)) => e   // Adding zero
    case BinOp("*", e, Number(1)) => e   // Multiplying by one
    case BinOp("/", e, Number(1)) => e   // Dividing by one
    case _ => expr                       // Can't simplify expr
  }

  def evaluate(expr: Expr): Int = expr match {
    case Number(a) => a
    case BinOp("+", Number(a), Number(b)) => a + b
    case BinOp("+", Number(a), e : Expr) => a + evaluate(e)
    case BinOp("+", e : Expr, Number(b)) => evaluate(e) + b
    case BinOp("+", e1 : Expr, e2 : Expr) => evaluate(e1) + evaluate(e2)
    case BinOp("*", Number(a), Number(b)) => a * b
    case BinOp("*", Number(a), e : Expr) => a * evaluate(e)
    case BinOp("*", e : Expr, Number(b)) => evaluate(e) * b
    case BinOp("*", e1 : Expr, e2 : Expr) => evaluate(e1) * evaluate(e2)
    case _ => 0
  }

}