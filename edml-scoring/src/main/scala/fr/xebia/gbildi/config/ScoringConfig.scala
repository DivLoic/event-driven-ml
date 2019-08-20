package fr.xebia.gbildi.config

import com.typesafe.config.Config

/**
 * Created by loicmdivad.
 */
case class ScoringConfig(dropoffTopic: String,
                         scoringTopic: String,
                         correctionTopic: String,
                         predictionTopic: String,
                         kafkaClient: Config)
