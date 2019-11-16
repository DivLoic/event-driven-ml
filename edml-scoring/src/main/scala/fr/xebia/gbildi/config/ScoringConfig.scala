package fr.xebia.gbildi.config

import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

/**
 * Created by loicmdivad.
 */
case class ScoringConfig(dropoffTopic: String,
                         scoringTopic: String,
                         correctionTopic: String,
                         predictionTopic: String,
                         windowSize: FiniteDuration,
                         kafkaClient: Config)
