package rminhas

import akka.actor.ActorSystem
import rminhas.actors.SectionActor.{AnalyzeLogEntry, PrintStatistics}
import akka.actor.typed.{ActorSystem => TypedActorSystem}
import rminhas.actors.{AlertActor, SectionActor}
import rminhas.actors.AlertActor.{PrintAlertMessage, IncrementRequestCount}
import rminhas.models.LogEntry
import rminhas.utils.Utils.tailFile
import java.nio.file.{Files, Paths}
import scala.concurrent.duration.DurationInt

object MainApp {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      println(
        """Pass the filename as a command line argument:
          |java -jar target/scala-2.13/common-log-analyzer-assembly-1.0.0.jar /path/to/file.log 
          |Stopping the program""".stripMargin
      )
      System.exit(1)
    }

    val filename = args(0)
    if (!Files.exists(Paths.get(filename))) {
      println(s"""File [$filename] was not found.
           |Stopping the program.""".stripMargin)
      System.exit(1)
    }

    implicit val rootActorSystem = ActorSystem()

    val sectionActor = TypedActorSystem(SectionActor(), "SectionActor")
    val alertActor   = TypedActorSystem(AlertActor(), "AlertActor")

    rootActorSystem.scheduler.scheduleWithFixedDelay(0.seconds, 10.seconds) { () =>
      sectionActor ! PrintStatistics
    }(rootActorSystem.dispatcher)

    rootActorSystem.scheduler.scheduleWithFixedDelay(0.seconds, 2.minutes) { () =>
      alertActor ! PrintAlertMessage
    }(rootActorSystem.dispatcher)

    tailFile(filename)
      .runForeach(logLine =>
        LogEntry
          .stringToLogEntry(logLine)
          .map(logEntry => {
            sectionActor ! AnalyzeLogEntry(logEntry)
            alertActor ! IncrementRequestCount(logEntry.date)
          })
      )
  }
}
