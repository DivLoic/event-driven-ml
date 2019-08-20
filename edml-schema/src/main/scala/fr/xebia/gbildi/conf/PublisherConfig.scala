package fr.xebia.gbildi.conf

import java.util

import com.typesafe.config.Config

import scala.collection.JavaConverters._

/**
 * Created by loicmdivad.
 */
case class PublisherConfig(modelTopic: String, modelName: String, kafkaClient: Config, tensorflowConfig: Config)

object PublisherConfig {

  def configToMap(c: Config): util.Map[String, AnyRef] = c.entrySet().asScala
    .map(pair => (pair.getKey, c.getAnyRef(pair.getKey)))
    .toMap.asJava
}
