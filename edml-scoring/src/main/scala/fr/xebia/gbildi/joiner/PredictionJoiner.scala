package fr.xebia.gbildi.joiner

import fr.xebia.gbildi.{TaxiTripDropoff, TripDurationCorrection, TripDurationPrediction}

/**
 * Created by loicmdivad.
 */
object PredictionJoiner {

  def mse(predicted: Float, actual: Int): Float = Math.pow(predicted - actual, 2).toFloat

  val predictionJoiner: (TripDurationPrediction, TaxiTripDropoff) => TripDurationCorrection =

    (prediction: TripDurationPrediction, dropoff: TaxiTripDropoff) => TripDurationCorrection(
      dropoff.dropoff_datetime,
      dropoff.trip_duration,
      prediction.prediction,
      prediction.version,
      mse(prediction.prediction, dropoff.trip_duration)
    )

}
