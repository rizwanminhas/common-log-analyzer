package rminhas.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.mutable

object AlertActor {
  sealed trait AlertActorMessage
  final case class IncrementRequestCount(logTime: Long)               extends AlertActorMessage
  final case object PrintAlertMessage                                 extends AlertActorMessage
  final case class GetAlertStatus(replyTo: ActorRef[(Boolean, Long)]) extends AlertActorMessage

  def getAverage(queue: mutable.Queue[Long]): Long =
    if (queue.nonEmpty) queue.sum / queue.size else 0

  def apply(rpsThreshold: Int = 10,
            secondsToMonitor: Int = 120,
            alerted: Boolean = false,
            requestCount: Long = 0L,
            lastLogTime: Long = 0L,
            statusChangedAt: Long = 0L): Behavior[AlertActorMessage] =
    Behaviors.receiveMessage {

      case IncrementRequestCount(logTime) =>
        apply(rpsThreshold, secondsToMonitor, alerted, requestCount + 1, logTime, statusChangedAt)

      case PrintAlertMessage =>
        val averageRps = requestCount / secondsToMonitor
        println("********************8average rps:" + averageRps)
        if (!alerted && averageRps >= rpsThreshold) {
          println(
            s"High traffic generated an alert - hits = {$averageRps}, triggered at {$lastLogTime}"
          )
          apply(rpsThreshold = rpsThreshold,
                secondsToMonitor = secondsToMonitor,
                alerted = true,
                statusChangedAt = lastLogTime)
        } else if (alerted && averageRps < rpsThreshold) {
          println(s"Alert recovered at {$lastLogTime}")
          apply(rpsThreshold = rpsThreshold,
                secondsToMonitor = secondsToMonitor,
                statusChangedAt = lastLogTime)
        } else {
          apply(rpsThreshold = rpsThreshold,
                secondsToMonitor = secondsToMonitor,
                alerted,
                statusChangedAt = statusChangedAt)
        }

      case GetAlertStatus(replyTo) =>
        replyTo ! (alerted, statusChangedAt)
        Behaviors.same
    }
}
