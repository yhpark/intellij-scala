package org.jetbrains.plugins.scala
package lang
package psi
package impl
package expr

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScConstrBlock

/**
  * @author Alexander.Podkhalyuzin
  */
final class ScConstrBlockImpl(node: ASTNode) extends ScExpressionImplBase(node) with ScConstrBlock {

  override def toString: String = "ConstructorBlock"
}