/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.dataset

import java.util.logging.Logger

import org.silkframework.config.{Prefixes, Task}
import org.silkframework.entity.Link
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.util.Identifier

import scala.xml.{Node, Text}

/**
 * A dataset of entities.
 */
class DatasetTask(id: Identifier, val plugin: Dataset, val minConfidence: Option[Double] = None, val maxConfidence: Option[Double] = None) extends Task(id, plugin) with SinkTrait {

  private val log = Logger.getLogger(DatasetTask.getClass.getName)

  def source = plugin.source

  lazy val entitySink: EntitySink = new EntitySinkWrapper

  lazy val linkSink: LinkSink = new LinkSinkWrapper

  def clear(): Unit = plugin.clear()

  private class EntitySinkWrapper extends EntitySink {

    private var entityCount: Int = 0

    private var isOpen = false

    private val writer = plugin.entitySink

    /**
     * Initializes this writer.
     */
    override def open(properties: Seq[String]) {
      if (isOpen) {
        writer.close()
        isOpen = false
      }

      writer.open(properties)
      entityCount = 0
      isOpen = true
    }

    override def writeEntity(subject: String, values: Seq[Seq[String]]) {
      require(isOpen, "Output must be opened before writing statements to it")
      writer.writeEntity(subject, values)
      entityCount += 1
    }

    /**
     * Closes this writer.
     */
    override def close() {
      if (writer != null) writer.close()
      isOpen = false
      log.info(s"Wrote $entityCount entities.")
    }
  }

  private class LinkSinkWrapper extends LinkSink {

    private var linkCount: Int = 0

    private var isOpen = false

    private val writer = plugin.linkSink

    override def init(): Unit = {
      if(isOpen) {
        writer.close()
        isOpen = false
      }
      writer.init()
      isOpen = true
    }

    /**
     * Writes a new link to this writer.
     */
    override def writeLink(link: Link, predicateUri: String) {
      //require(isOpen, "Output must be opened before writing statements to it")

      if ((minConfidence.isEmpty || link.confidence.getOrElse(-1.0) >= minConfidence.get) &&
          (maxConfidence.isEmpty || link.confidence.getOrElse(-1.0) < maxConfidence.get)) {
        writer.writeLink(link, predicateUri)
        linkCount += 1
      }
    }

    /**
     * Closes this writer.
     */
    override def close() {
      if (writer != null) writer.close()
      isOpen = false
      log.info(s"Wrote $linkCount links.")
    }
  }
}

object DatasetTask {

  implicit def fromTask(task: Task[Dataset]): DatasetTask = new DatasetTask(task.id, task.data)

  def empty = {
    new DatasetTask("empty", EmptyDataset)
  }

  /**
   * XML serialization format.
   */
  implicit object DatasetFormat extends XmlFormat[DatasetTask] {

    def read(node: Node)(implicit readContext: ReadContext): DatasetTask = {
      implicit val prefixes = readContext.prefixes
      implicit val resources = readContext.resources

      // Check if the data source still uses the old outdated XML format
      if(node.label == "DataSource" || node.label == "Output") {
        // Read old format
        val id = (node \ "@id").text
        new DatasetTask(
          id = if(id.nonEmpty) id else Identifier.random,
          plugin = Dataset((node \ "@type").text, readParams(node)),
          minConfidence = (node \ "@minConfidence").headOption.map(_.text.toDouble),
          maxConfidence = (node \ "@maxConfidence").headOption.map(_.text.toDouble)
        )
      } else {
        // Read new format
        val id = (node \ "@id").text
        // In outdated formats the plugin parameters are nested inside a DatasetPlugin node
        val sourceNode = (node \ "DatasetPlugin").headOption.getOrElse(node)
        new DatasetTask(
          id = if(id.nonEmpty) id else Identifier.random,
          plugin = Dataset((sourceNode \ "@type").text, readParams(sourceNode)),
          minConfidence = (node \ "@minConfidence").headOption.map(_.text.toDouble),
          maxConfidence = (node \ "@maxConfidence").headOption.map(_.text.toDouble)
        )
      }
    }

    private def readParams(element: Node): Map[String, String] = {
      (element \ "Param" map (p => ((p \ "@name").text, (p \ "@value").text))).toMap
    }

    def write(value: DatasetTask)(implicit writeContext: WriteContext[Node]): Node = {
      val minConfidenceNode = value.minConfidence.map(c => Text(c.toString))
      val maxConfidenceNode = value.maxConfidence.map(c => Text(c.toString))

      value.plugin match {
        case Dataset(pluginDesc, params) =>
          <Dataset id={value.id} type={pluginDesc.id} minConfidence={minConfidenceNode} maxConfidence={maxConfidenceNode}>
            { params.map {
            case (name, v) => <Param name={name} value={v}/>
          }}
          </Dataset>
      }
    }
  }
}