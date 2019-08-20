package fr.xebia.gbildi

/**
 * Created by loicmdivad.
 */
package object config {

  import com.typesafe.config.Config
  import scala.collection.JavaConverters._

  trait Configs {

    implicit class configMapperOps(config: Config) {

      def toMap: Map[String, String] = config
        .entrySet()
        .asScala
        .map(pair => (pair.getKey, config.getAnyRef(pair.getKey).toString))
        .toMap
    }
  }
}
