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

package org.silkframework.plugins.dataset.rdf.sparql

import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.entity.Path
import org.silkframework.entity.rdf.SparqlRestriction

/**
 * Retrieves the most frequent property paths.
 */
trait SparqlPathsCollector {
  def apply(endpoint: SparqlEndpoint, restrictions: SparqlRestriction, limit: Option[Int]): Seq[Path]
}

object SparqlPathsCollector {
  def apply(endpoint: SparqlEndpoint, restrictions: SparqlRestriction, limit: Option[Int]) = {
    SparqlSamplePathsCollector(endpoint, restrictions, limit)
  }
}