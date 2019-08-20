package fr.xebia.gbildi.config

import com.typesafe.config.Config

/**
 * Created by loicmdivad.
 */
case class ScoringConfig(dropoffTopic: String,
                         joinTopic: String,
                         scoringTopic: String,
                         predictionTopic: String,
                         kafkaClient: Config)
