package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package elements

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IndexSink, StubElement, StubInputStream, StubOutputStream}
import com.intellij.util.io.StringRef
import org.jetbrains.plugins.scala.extensions.MaybePsiElementExt
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScPatternDefinition, ScValue, ScValueDeclaration}
import org.jetbrains.plugins.scala.lang.psi.stubs.impl.ScValueStubImpl
import org.jetbrains.plugins.scala.lang.psi.stubs.index.ScalaIndexKeys.VALUE_NAME_KEY

/**
  * User: Alexander Podkhalyuzin
  * Date: 17.10.2008
  */
abstract class ScValueElementType[Value <: ScValue](debugName: String)
  extends ScStubElementType[ScValueStub, ScValue](debugName) {
  override def serialize(stub: ScValueStub, dataStream: StubOutputStream): Unit = {
    dataStream.writeBoolean(stub.isDeclaration)
    val names = stub.getNames
    dataStream.writeInt(names.length)
    for (name <- names) dataStream.writeName(name)
    dataStream.writeName(stub.getTypeText)
    dataStream.writeName(stub.getBodyText)
    dataStream.writeName(stub.getBindingsContainerText)
    dataStream.writeBoolean(stub.isImplicit)
    dataStream.writeBoolean(stub.isLocal)
  }

  override def deserialize(dataStream: StubInputStream, parentStub: StubElement[_ <: PsiElement]): ScValueStub = {
    val isDecl = dataStream.readBoolean
    val namesLength = dataStream.readInt
    val names = new Array[StringRef](namesLength)
    for (i <- 0 until namesLength) names(i) = dataStream.readName
    val parent = parentStub.asInstanceOf[StubElement[PsiElement]]
    val typeText = dataStream.readName
    val bodyText = dataStream.readName
    val bindingsText = dataStream.readName
    val isImplicit = dataStream.readBoolean()
    val isLocal = dataStream.readBoolean()
    new ScValueStubImpl(parent, this, names, isDecl, typeText, bodyText, bindingsText, isImplicit, isLocal)
  }

  override def createStub(psi: ScValue, parentStub: StubElement[_ <: PsiElement]): ScValueStub = {
    val isDecl = psi.isInstanceOf[ScValueDeclaration]
    val typeText = psi.typeElement.text
    val bodyText = if (!isDecl) psi.asInstanceOf[ScPatternDefinition].expr.text else ""
    val containerText = if (isDecl) psi.asInstanceOf[ScValueDeclaration].getIdList.getText
    else psi.asInstanceOf[ScPatternDefinition].pList.getText
    val isImplicit = psi.hasModifierProperty("implicit")
    new ScValueStubImpl(parentStub, this,
      (for (elem <- psi.declaredElements) yield elem.name).toArray, isDecl, typeText, bodyText, containerText,
      isImplicit, psi.containingClass == null)
  }

  override def indexStub(stub: ScValueStub, sink: IndexSink): Unit = {
    this.indexStub(stub.getNames, sink, VALUE_NAME_KEY)
    if (stub.isImplicit) {
      this.indexImplicit(sink)
    }
  }
}