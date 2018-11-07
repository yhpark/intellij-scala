package org.jetbrains.sbt

import java.util

import com.intellij.ide.actions.runAnything.RunAnythingUtil._
import com.intellij.ide.actions.runAnything.activity.RunAnythingProviderBase
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.util.text.StringUtil
import javax.swing.Icon
import org.jetbrains.plugins.scala.icons.Icons
import org.jetbrains.sbt.SbtRunAnythingProvider._
import org.jetbrains.sbt.project.data.SbtTaskData
import org.jetbrains.sbt.settings.SbtSettings
import org.jetbrains.sbt.shell.SbtShellCommunication
import org.jetbrains.sbt.shell.action.SbtNodeAction

import scala.collection.JavaConverters._

class SbtRunAnythingProvider extends RunAnythingProviderBase[SbtRunItem] {

  override def getValues(dataContext: DataContext, pattern: String): util.Collection[SbtRunItem] = {

    val values = if (canRunSbt(dataContext, pattern)) {
      val project = fetchProject(dataContext)
      val commandString = StringUtil.substringAfter(pattern, " ")

      val modules = ModuleManager.getInstance(project).getModules.toList
//      val moduleDatas = modules.flatMap(SbtUtil.getSbtModuleData(_).toList)
      val moduleDataAndTasks = modules.flatMap { module =>
        val moduleId = ExternalSystemApiUtil.getExternalProjectId(module)
        val maybeModuleData = SbtUtil.getSbtModuleData(project, moduleId)

        maybeModuleData.map { moduleData =>
          val moduleTasks = SbtUtil.getModuleData(project, moduleId, SbtTaskData.Key).toList
          val relevantTasks = moduleTasks.filter(_.name.contains(commandString))

          (moduleData, relevantTasks)
        }
      }

      val suggestions = moduleDataAndTasks.flatMap { case (moduleData, tasks) =>
        val projectId = SbtUtil.makeSbtProjectId(moduleData)
        val basicSuggestion = SbtShellTask(projectId, commandString)
        val taskSuggestions = tasks.sortBy(_.rank).reverse.map { task => SbtShellTask(projectId, task.name) }
        basicSuggestion :: taskSuggestions
      }

      val defaultSuggestion = SbtShellCommandString(commandString)
      defaultSuggestion :: suggestions
    } else Nil

    (values: List[SbtRunItem]).asJava
  }

  override def execute(dataContext: DataContext, value: SbtRunItem): Unit = {
    val project = fetchProject(dataContext)
    val com = SbtShellCommunication.forProject(project)
    com.command(value.command)
  }

  override def getCommand(value: SbtRunItem): String = {
    "sbt " + value.command
  }

  override def findMatchingValue(dataContext: DataContext, pattern: String): SbtRunItem = {
    if (canRunSbt(dataContext, pattern)) {
      val commandString = StringUtil.substringAfter(pattern, " ")
      SbtShellCommandString(commandString)
    } else null
  }

  override def getIcon(value: SbtRunItem): Icon = Icons.SBT_SHELL

  override def getCompletionGroupTitle: String = "sbt"

  private def canRunSbt(dataContext: DataContext, pattern: String) = {
    if (pattern.startsWith("sbt")) {
      val project = fetchProject(dataContext)
      val sbtSettings = SbtSettings.getInstance(project).getLinkedProjectsSettings
      !sbtSettings.isEmpty
    } else false
  }

}

object SbtRunAnythingProvider {
  sealed abstract class SbtRunItem {
    def command: String
  }
  case class SbtShellCommandString(command: String) extends SbtRunItem
  case class SbtShellTask(projectId: String, task: String) extends SbtRunItem {
    override def command: String = SbtNodeAction.scopedKey(projectId, task)
  }
}
