package fr.xebia.gbildi

import java.time.Duration
import scala.concurrent.duration.FiniteDuration


/**
 * Created by loicmdivad.
 */
package object schema {

  implicit class ScalaDurationConverter(scalaDuration: FiniteDuration){
    def toJava: Duration = Duration.ofNanos(scalaDuration.toNanos)
  }

  implicit class JavaDurationConverter(javaDuration: Duration){
    def toScala: FiniteDuration = scala.concurrent.duration.Duration.fromNanos(javaDuration.toNanos)
  }
}
