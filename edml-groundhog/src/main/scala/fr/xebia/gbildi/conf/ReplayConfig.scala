package fr.xebia.gbildi.conf

import java.time.ZoneId

import com.typesafe.config.Config

/**
 * Created by loicmdivad.
 */
case class ReplayConfig(sourceTopic: String,
                        replayTopic: String,
                        dtFiled: String,
                        timeZone: ZoneId,
                        kafkaConsumer: Config,
                        kafkaProducer: Config)
