package fr.xebia.gbildi.processor

import fr.xebia.gbildi.AggregatedTripDurationRmse
import org.apache.kafka.common.MetricName
import org.apache.kafka.common.metrics.Sensor
import org.apache.kafka.common.metrics.stats.{Avg, Value}
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.{Transformer, Windowed}
import org.apache.kafka.streams.processor.ProcessorContext

import scala.collection.JavaConverters._
import scala.collection.mutable

import cats.implicits._

/**
 * Created by loicmdivad.
 */
class CostMetricProcessor extends Transformer[Windowed[String], AggregatedTripDurationRmse, KeyValue[Windowed[String], Double]] {

  var context: ProcessorContext = _
  val sensors: mutable.Map[String, Sensor] = mutable.Map.empty

  def metricName(cost: String, stat: String, version: String): MetricName =
    new MetricName(
      s"duration-$cost-$stat", "edml-taxi-trip",
      "Edml Prediction Error Metrics",
      Map(
        "model-version" -> version,
        "app-id" -> context.applicationId(),
        "task-id" -> context.taskId().toString
      ).asJava
    )

  def addSensor(version: String): Unit = {
    val sensor = this.context.metrics.addSensor("edml-cost-functions", Sensor.RecordingLevel.INFO)
    sensor.add(metricName("rmse", "value", version), new Value)
    sensor.add(metricName("rmse", "avg", version), new Avg())

    this.sensors += (version -> sensor)
  }

  override def init(context: ProcessorContext): Unit = {
    this.context = context
  }

  override def transform(key: Windowed[String],
                         value: AggregatedTripDurationRmse): KeyValue[Windowed[String], Double] = {

    val newValue: Double = this.sensors.get(value.version)

      .recoverWith { case _ =>
        addSensor(value.version)
        this.sensors.get(value.version)
      }

      .map { sensor =>
        val rmse = Math.sqrt(value.sum_mse / value.count)
        sensor.record(rmse, this.context.timestamp())
        rmse

      }.getOrElse(Double.MinValue)

    new KeyValue(key, newValue)
  }

  override def close(): Unit = ()
}
