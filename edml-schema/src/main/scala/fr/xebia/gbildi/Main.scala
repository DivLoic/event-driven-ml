package fr.xebia.gbildi

import org.slf4j.LoggerFactory

/**
 * Created by loicmdivad.
 */
object Main extends App {

  val logger = LoggerFactory.getLogger(getClass)

  logger info "Logging TaxiTripPickup schema: "
  logger info fr.xebia.gbildi.TaxiTripPickup.SCHEMA$.toString(true)

  logger info "Logging TaxiTripDropoff schema: "
  logger info fr.xebia.gbildi.TaxiTripDropoff.SCHEMA$.toString(true)

  logger info "Logging TripDurationPrediction schema: "
  logger info fr.xebia.gbildi.TripDurationPrediction.SCHEMA$.toString(true)

  logger info "Logging TripDurationCorrection schema: "
  logger info fr.xebia.gbildi.AggregatedTripDurationRmse.SCHEMA$.toString(true)
}
