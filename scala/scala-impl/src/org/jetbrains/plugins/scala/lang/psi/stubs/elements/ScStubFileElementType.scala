package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package elements

import com.intellij.lang.Language
import com.intellij.psi.stubs.{IndexSink, StubElement, StubInputStream, StubOutputStream}
import com.intellij.psi.tree.IStubFileElementType
import com.intellij.psi.{PsiElement, StubBuilder}
import com.intellij.util.io.StringRef
import org.jetbrains.plugins.scala.decompiler.DecompilerUtil
import org.jetbrains.plugins.scala.lang.psi.stubs.impl.AbstractFileStub

/**
  * @author ilyas
  */
class ScStubFileElementType(language: Language = ScalaLanguage.INSTANCE)
  extends IStubFileElementType[ScFileStub]("file", language) {

  override final def getStubVersion: Int = super.getStubVersion + DecompilerUtil.DECOMPILER_VERSION

  override def getBuilder: StubBuilder = new ScalaFileStubBuilder

  override def getExternalId: String = "scala.file"

  override final def serialize(stub: ScFileStub, dataStream: StubOutputStream): Unit = {
    dataStream.writeBoolean(stub.isScript)
    dataStream.writeBoolean(stub.isCompiled)
    dataStream.writeName(stub.packageName)
    dataStream.writeName(stub.sourceName)
  }

  override final def deserialize(dataStream: StubInputStream, parentStub: StubElement[_ <: PsiElement]): ScFileStub =
    new FileStubImpl(isScript = dataStream.readBoolean,
      isCompiled = dataStream.readBoolean,
      packageName = StringRef.toString(dataStream.readName),
      sourceName = StringRef.toString(dataStream.readName))

  override final def indexStub(stub: ScFileStub, sink: IndexSink): Unit = {}

  private class FileStubImpl(val isScript: Boolean,
                             val isCompiled: Boolean,
                             val packageName: String,
                             val sourceName: String)
    extends AbstractFileStub(null)
}
