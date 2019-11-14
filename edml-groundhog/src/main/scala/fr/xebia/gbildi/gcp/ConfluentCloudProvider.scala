package fr.xebia.gbildi.gcp

import com.typesafe.config.Config

import scala.collection.JavaConverters._

/**
 * Created by loicmdivad.
 */
trait ConfluentCloudProvider {

  implicit class configMapperOps(config: Config) {

    def toMap: Map[String, String] = config
        .entrySet()
        .asScala
        .map(pair => (pair.getKey, config.getAnyRef(pair.getKey).toString))
        .toMap
  }
}