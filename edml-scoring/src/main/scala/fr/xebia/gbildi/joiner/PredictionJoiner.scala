package fr.xebia.gbildi.joiner

import fr.xebia.gbildi.{TaxiTripDropoff, TripDurationMse, TripDurationPrediction}

/**
 * Created by loicmdivad.
 */
object PredictionJoiner {

  def mse(predicted: Float, actual: Int): Float = Math.pow(predicted - actual, 2).toFloat

  val predictionJoiner: (TripDurationPrediction, TaxiTripDropoff) => TripDurationMse =

    (prediction: TripDurationPrediction, dropoff: TaxiTripDropoff) => TripDurationMse(
      dropoff.dropoff_datetime,
      dropoff.trip_duration,
      prediction.prediction,
      prediction.version,
      mse(prediction.prediction, dropoff.trip_duration)
    )

}
