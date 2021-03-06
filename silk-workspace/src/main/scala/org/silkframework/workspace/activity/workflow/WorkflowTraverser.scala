package org.silkframework.workspace.activity.workflow

import org.silkframework.config.TaskSpec
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, ProjectTask}

import scala.reflect.ClassTag

/**
  * Allows to traverse through the workflow.
  *
  * @param operator The current workflow node
  * @param workflow The workflow task
  */
class WorkflowTraverser(operator: WorkflowNode)(implicit workflow: ProjectTask[Workflow]) {

  def task: ProjectTask[_] = workflow.project.anyTask(operator.task)

  def taskOption[T <: TaskSpec : ClassTag](taskName: Identifier): Option[ProjectTask[T]] = {
    workflow.project.taskOption[T](taskName)
  }

  def inputs: Seq[WorkflowTraverser] = {
    operator.inputs.map(input => new WorkflowTraverser(workflow.data.node(input)))
  }

  def outputs: Seq[WorkflowTraverser] = {
    operator.outputs.map(output => new WorkflowTraverser(workflow.data.node(output)))
  }
}