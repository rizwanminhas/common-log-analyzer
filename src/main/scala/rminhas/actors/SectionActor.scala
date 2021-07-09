package rminhas.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import rminhas.models.LogEntry
import rminhas.utils.Utils.getSection
import scala.collection.immutable.Map

object SectionActor {

  sealed trait SectionActorMessage
  final case class AnalyzeLogEntry(logEntry: LogEntry) extends SectionActorMessage
  final case object PrintStatistics                    extends SectionActorMessage

  def apply(sectionsCountMap: Map[String, Long] = Map.empty,
            statusCountMap: Map[Int, Long] = Map.empty): Behavior[SectionActorMessage] =
    Behaviors.receiveMessage {

      case AnalyzeLogEntry(logEntry) =>
        val statusCount           = statusCountMap.getOrElse(logEntry.status, 0L) + 1
        val updatedStatusCountMap = statusCountMap + (logEntry.status -> statusCount)
        getSection(logEntry.request) match {
          case Some(section) =>
            val sectionCount = sectionsCountMap.getOrElse(section, 0L) + 1
            apply(sectionsCountMap + (section -> sectionCount), updatedStatusCountMap)
          case None => apply(sectionsCountMap, updatedStatusCountMap)
        }

      case PrintStatistics =>
        val topNSectionsWithCount = sectionsCountMap.toSeq.sortWith(_._2 > _._2).take(10)
        printResults(topNSectionsWithCount, statusCountMap)
        apply()
    }

  def printResults(sectionsCount: Seq[(String, Long)], statusCodesMap: Map[Int, Long]): Unit = {
    val output = new StringBuilder("\n\n*** Section results start ***\n")
    output.append(s"Top 10 sections count:\n")

    if (sectionsCount.isEmpty)
      output.append("No section data to report.\n")
    else
      sectionsCount.foreach(entry => output.append(s"section=${entry._1}, count=${entry._2}\n"))

    output.append(s"\nHTTP status code counts:\n")
    if (statusCodesMap.isEmpty)
      output.append("No HTTP status code data to report.\n")
    else
      statusCodesMap.foreach(entry =>
        output.append(s"http status code=${entry._1}, count=${entry._2}\n")
      )
    output.append("*** Section results end ***\n")

    println(output.toString)
  }
}
