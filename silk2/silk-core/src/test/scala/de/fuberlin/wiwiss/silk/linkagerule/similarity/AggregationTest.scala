package de.fuberlin.wiwiss.silk.linkagerule.similarity

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.plugins.aggegrator.AverageAggregator
import de.fuberlin.wiwiss.silk.util.{DPair, Identifier}
import de.fuberlin.wiwiss.silk.entity.Entity
import de.fuberlin.wiwiss.silk.linkagerule.Index
import de.fuberlin.wiwiss.silk.config.Prefixes
import xml.Node
import de.fuberlin.wiwiss.silk.plugins.util.approximatelyEqualToOption

class AggregationTest extends FlatSpec with ShouldMatchers {
  val aggregator = new AverageAggregator()

  "Aggregation with average aggregator" should "return the average if both operators return a value" in {
    aggregation(
      operator(1, false, Some(0.8)) ::
      operator(1, false, Some(0.6)) :: Nil
    ) should be(approximatelyEqualToOption(Some(0.7)))
  }

  "Aggregation with average aggregator" should "return the average of all values if the operators not returning a value are not required" in {
    aggregation(
      operator(1, false, Some(0.8)) ::
      operator(1, false, Some(0.6)) ::
      operator(1, false, None) :: Nil
    ) should be(approximatelyEqualToOption(Some(0.7)))
  }

  "Aggregation with average aggregator" should "return nothing if a required operator does not return a value" in {
    aggregation(
      operator(1, false, Some(0.8)) ::
      operator(1, false, Some(0.6)) ::
      operator(1, true, None) :: Nil
    ) should be(approximatelyEqualToOption(None))
  }

  private def aggregation(ops: Seq[SimilarityOperator]) = {
    Aggregation(operators = ops, aggregator = aggregator).apply(null)
  }

  private def operator(w: Int, r: Boolean, value: Option[Double]) = {
    new SimilarityOperator {
      val id: Identifier = null
      val weight: Int = w
      val required: Boolean = r
      def apply(entities: DPair[Entity], limit: Double): Option[Double] = value
      def index(entity: Entity, limit: Double): Index = null
      def toXML(implicit prefixes: Prefixes): Node = null
    }
  }
}