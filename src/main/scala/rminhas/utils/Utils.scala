package rminhas.utils

import akka.NotUsed
import akka.stream.alpakka.file.scaladsl.FileTailSource
import akka.stream.scaladsl.Source
import java.nio.file.Paths
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Utils {
  def getSection(url: String): Option[String] =
    url.split("/") match {
      case arr if arr.length > 1 && arr(1).trim.nonEmpty => Some("/" + arr(1))
      case _                                             => None
    }

  def tailFile(filename: String,
               maxLineSize: Int = 8192,
               pollingInterval: FiniteDuration = 250.millis): Source[String, NotUsed] =
    FileTailSource.lines(
      path = Paths.get(filename),
      maxLineSize = maxLineSize,
      pollingInterval = pollingInterval
    )
}
