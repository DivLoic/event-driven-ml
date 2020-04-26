package fr.xebia.gbildi.config

import com.typesafe.config.Config

/**
 * Created by loicmdivad.
 */
case class ServingConfig(pickupTopic: String,
                         modelTopic: String,
                         modelStore: String,
                         predictionTopic: String,
                         kafkaClient: Config)