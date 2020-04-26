package fr.xebia.gbildi.processor

import java.time.{Duration, Instant}

import cats.implicits._
import fr.xebia.gbildi.AggregatedTripDurationRmse
import org.apache.kafka.common.MetricName
import org.apache.kafka.common.metrics.Sensor
import org.apache.kafka.common.metrics.stats.Value
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.{Transformer, Windowed}
import org.apache.kafka.streams.processor.{ProcessorContext, PunctuationType}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * Created by loicmdivad.
 */
class CostMetricProcessor extends Transformer[Windowed[String], AggregatedTripDurationRmse, KeyValue[Windowed[String], Double]] {

  var context: ProcessorContext = _
  val sensors: mutable.Map[String, TimedSensor] = mutable.Map.empty

  case class TimedSensor(sensor: Sensor, maybeLastRecordTime: Option[Instant] = None) {
    def record(stat: Double, timestamp: Long): TimedSensor = {
      sensor.record(stat, timestamp)
      this.copy(maybeLastRecordTime = Some(Instant.ofEpochMilli(timestamp)))
    }
  }

  def metricName(cost: String, version: String): MetricName =
    new MetricName(s"duration-$cost", "edml-taxi-trip", "Edml Prediction Error Metrics", Map(
        "model-version" -> version,
        "app-id" -> context.applicationId(),
        "task-id" -> context.taskId().toString
      ).asJava
    )

  def addSensor(version: String): Unit = {
    val sensor: Sensor = this.context.metrics.addSensor(s"edml-cost-functions-$version", Sensor.RecordingLevel.INFO)
    sensor.add(metricName("rmse", version), new Value)
    this.sensors += (version -> TimedSensor(sensor))
  }

  override def init(context: ProcessorContext): Unit = {
    this.context = context

    this.context.schedule(Duration.ofMinutes(10), PunctuationType.WALL_CLOCK_TIME, _ => {
      this.sensors.toVector.foreach { case (version, sensor) =>
        sensor.maybeLastRecordTime.foreach { lastRecordTime =>
          if (Instant.now().isAfter(lastRecordTime.plus(Duration.ofSeconds(30)))) {
              this.context.metrics().removeSensor(sensor.sensor)
              this.sensors -= version
          }
        }
      }
    })
  }

  override def transform(key: Windowed[String],
                         value: AggregatedTripDurationRmse): KeyValue[Windowed[String], Double] = {

    val newValue: Double = this.sensors.get(value.version)

      .recoverWith { case _ =>
        addSensor(value.version)
        this.sensors.get(value.version)
      }

      .map { timedSensor =>
        val rmse = Math.sqrt(value.sum_mse / value.count)
        this.sensors += (value.version -> timedSensor.record(rmse, this.context.timestamp()))
        rmse

      }.getOrElse(Double.MinValue)

    new KeyValue(key, newValue)
  }

  override def close(): Unit = ()
}
