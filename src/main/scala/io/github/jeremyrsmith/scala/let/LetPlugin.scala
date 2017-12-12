package io.github.jeremyrsmith.scala.let

import scala.tools.nsc
import nsc.Global
import nsc.plugins.Plugin
import nsc.plugins.PluginComponent
import scala.collection.immutable.Queue
import scala.tools.nsc.ast.TreeDSL
import scala.tools.nsc.transform.{Transform, TypingTransformers}

final class LetPlugin(val global: Global) extends Plugin {
  import global._

  val name: String = "let-bindings"
  val description: String = "Allow let binding expressions"

  val components: List[PluginComponent] = DesugarLet :: Nil

  private object DesugarLet extends PluginComponent with Transform with TypingTransformers with TreeDSL {
    val global: LetPlugin.this.global.type = LetPlugin.this.global
    val phaseName: String = "desugar-let"
    val runsAfter: List[String] = "parser" :: Nil
    override val runsBefore: List[String] = "namer" :: Nil

    private val Let = newTermName("let")
    private val In = newTermName("in")

    class DesugarLetTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {
      override def transform(tree: Tree): Tree = tree match {
        case Apply(Select(Apply(Ident(Let), bindings), In), List(expr)) =>
          val bindingsList = bindings match {
            case List(Block(firstN, last)) => firstN :+ last
            case args => args
          }

          bindingsList.foldLeft[Either[Tree, Queue[Tree]]](Right(Queue.empty)) {
            (accumE, next) => accumE match {
              case l @ Left(_) => l
              case Right(accum) => next match {
                case AssignOrNamedArg(Ident(valName: TermName), rhs) => Right(accum enqueue q"val $valName = $rhs")
                case Assign(Ident(valName: TermName), rhs) => Right(accum enqueue q"val $valName = $rhs")
                case v: ValDef => Right(accum enqueue v)
                case Literal(Constant(())) => Right(accum)  // block terminated by ()
                case other => Left(other)
              }
            }
          } match {
            case Left(errTree) =>
              global.warning(errTree.pos, s"Not a valid let binding $errTree (${errTree.getClass}); abandoning desugar")
              tree
            case Right(bindVals) =>
              q"""
                ..$bindVals
                ..$expr
              """
          }
        case other => super.transform(other)
      }
    }

    protected def newTransformer(unit: CompilationUnit): Transformer = new DesugarLetTransformer(unit)
  }
}
