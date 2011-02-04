package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.util.Helpers._
import net.liftweb.http.SHtml
import net.liftweb.json.JsonAST.{JArray, JValue}
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.json.JsonDSL._
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.json.JsonAST
import net.liftweb.http.js.JsCmds.{Script, OnLoad}
import xml.{Node, NodeSeq}
import net.liftweb.http.js.{JsCmd, JsCmds}

/**
 * Workspace snippet.
 *
 * Injects the following functions:
 *
 * def openLinkingTask(
 *     projectName : The name of the project,
 *     taskName : The name of the task to be removed,
 * )
 *
 * def removeLinkingTask(
 *     projectName : The name of the project,
 *     taskName : The name of the task to be removed,
 * )
 *
 * Whenever the workspace changes, the following function will be called:
 *
 * def updateWorkspace(
 *     workspace : JSON containing the workspace contents
 * )
 *
 */
class Workspace
{
  def toolbar(xhtml : NodeSeq) : NodeSeq =
  {
    def showCreateDialog() =
    {
      JsRaw("$('#createLinkingTaskDialog').dialog('open')").cmd
    }

    val initCreateDialog = Script(OnLoad(JsRaw("$('#createLinkingTaskDialog').dialog({ autoOpen: false, width: 700, modal: true })").cmd))

    bind("entry", xhtml,
         "new" -> (initCreateDialog ++ SHtml.ajaxButton("New", showCreateDialog _)))
  }

  def content(xhtml : NodeSeq) : NodeSeq =
  {
    val injectedJavascript = updateWorkspaceCmd &
                             injectFunction("openLinkingTask", openLinkingTask _, true) &
                             injectFunction("removeLinkingTask", removeLinkingTask _)


    bind("entry", xhtml,
         "injectedJavascript" -> (Script(injectedJavascript)))
  }

  /**
   * Opens a linking task from the workspace.
   */
  private def openLinkingTask(projectName : String, taskName : String)
  {
    //Ignoring the projectName for now until we have an complete workspace
    User().project.linkingModule.tasks.find(_.name == taskName) match
    {
      case Some(linkingTask) => User().linkingTask = linkingTask
      case None => throw new IllegalArgumentException("Linking Task '" + taskName + "' not found in project '" + projectName + "'.")
    }
  }

  /**
   * Removes a linking task from the workspace.
   */
  private def removeLinkingTask(projectName : String, taskName : String)
  {
    //Ignoring the projectName for now until we have an complete workspace
    User().project.linkingModule.remove(taskName)
  }

  /*
   * Injects a Javascript function.
   */
  private def injectFunction(name : String, func : (String, String) => Unit, reload : Boolean = false) : JsCmd =
  {
    //Callback which executes the provided function
    def callback(args : String) : JsCmd =
    {
      try
      {
        val Array(projectName, taskName) = args.split(',')

        func(projectName, taskName)

        if(reload)
          updateWorkspaceCmd & JsRaw("window.location.reload();").cmd
        else
          updateWorkspaceCmd
      }
      catch
      {
        case ex : Exception => JsRaw("alert('" + ex.getMessage.encJs + "');").cmd
      }
    }

    //Ajax Call which executes the callback
    val ajaxCall = SHtml.ajaxCall(JsRaw("projectName + ',' + taskName"), callback _)._2.cmd

    //JavaScript function definition
    JsCmds.Function(name, "projectName" :: "taskName" :: Nil, ajaxCall)
  }

  /**
   * JS Command which updates the workspace view.
   */
  private def updateWorkspaceCmd : JsCmd =
  {
    JsRaw("var workspaceVar = " + pretty(JsonAST.render(workspaceJson)) + "; updateWorkspace(workspaceVar);").cmd
  }

  /**
   * Generates a JSON which contains the workspace contents.
   */
  private def workspaceJson : JValue =
  {
    val project = User().project

    val sources : JArray = for(task <- project.sourceModule.tasks.toSeq) yield
    {
      ("name" -> task.name.toString) ~
      ("url" -> task.source.dataSource.toString)
    }

    val linkingTasks : JArray = for(task <- project.linkingModule.tasks.toSeq) yield
    {
      ("name" -> task.name.toString) ~
      ("source" -> task.linkSpec.datasets.source.sourceId.toString) ~
      ("target" -> task.linkSpec.datasets.target.sourceId.toString) ~
      ("sourceDataset" -> task.linkSpec.datasets.source.restriction) ~
      ("targetDataset" -> task.linkSpec.datasets.target.restriction)
    }

    val projects : JArray = for(p <- project :: Nil) yield
    {
      ("name" -> project.name.toString) ~
      ("dataSource" -> sources) ~
      ("linkingTask" -> linkingTasks)
    }

    ("workspace" -> ("project" -> projects))
  }
}