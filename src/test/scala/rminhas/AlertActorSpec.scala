package rminhas

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import rminhas.actors.AlertActor
import rminhas.actors.AlertActor.{
  AlertActorMessage,
  GetAlertStatus,
  IncrementRequestCount,
  PrintAlertMessage
}
import java.util.UUID

class AlertActorSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {
  private val testKit     = ActorTestKit()
  private val statusProbe = testKit.createTestProbe[(Boolean, Long)]("statusProbe")

  override def afterAll(): Unit = testKit.shutdownTestKit()

  private def getActor(rpsThreshold: Int, secondsToMonitor: Int): ActorRef[AlertActorMessage] =
    testKit.spawn(AlertActor(rpsThreshold = rpsThreshold, secondsToMonitor = secondsToMonitor),
                  "alertActor-" + UUID.randomUUID())

  private def generateAndProcessLogs(actor: ActorRef[AlertActorMessage],
                                     totalReqsCount: Int,
                                     startTime: Long) =
    (1 to totalReqsCount).foreach(i => actor ! IncrementRequestCount(startTime + i * 10))

  "AlertActor" should "have the alert flag set to false and timestamp set to 0 initially." in {
    val alertActor = getActor(10, 120)
    alertActor ! GetAlertStatus(statusProbe.ref)
    statusProbe.expectMessage((false, 0L))
  }

  it should "not set the alert flag to true or print an alert if the rps is < threshold." in {
    val rpsThreshold = 3
    val alertActor   = getActor(rpsThreshold, 120)
    generateAndProcessLogs(alertActor, rpsThreshold - 1, 0)
    alertActor ! PrintAlertMessage
    alertActor ! GetAlertStatus(statusProbe.ref)
    statusProbe.expectMessage((false, 0L))
  }

  it should "set the alert flag to true and print an alert if rps >= threshold." in {
    val rpsThreshold  = 3
    val alertActor    = getActor(rpsThreshold, 3)
    val totalRequests = 100
    generateAndProcessLogs(alertActor, totalRequests, 0)
    alertActor ! PrintAlertMessage
    alertActor ! GetAlertStatus(statusProbe.ref)
    statusProbe.expectMessage((true, totalRequests * 10L))
  }

  it should "recover from an alert i.e. set alert flag to false once the rps falls below the threshold." in {
    val rpsThreshold  = 10
    val alertActor    = getActor(rpsThreshold, 3)
    val totalRequests = 30
    generateAndProcessLogs(alertActor, totalRequests, 0)
    alertActor ! PrintAlertMessage
    alertActor ! GetAlertStatus(statusProbe.ref)
    statusProbe.expectMessage((true, totalRequests * 10L))

    generateAndProcessLogs(alertActor, totalRequests - 1, totalRequests * 10)
    alertActor ! PrintAlertMessage
    alertActor ! GetAlertStatus(statusProbe.ref)
    statusProbe.expectMessage((false, (2 * totalRequests * 10 - 10).toLong))
  }

  it should "keep the alert flag set to true with the timestamp of log entry that triggered the alert as long as the rps stays >= threshold." in {
    val rpsThreshold  = 5
    val alertActor    = getActor(rpsThreshold, 10)
    val totalRequests = 60
    generateAndProcessLogs(alertActor, totalRequests, 0)
    alertActor ! PrintAlertMessage
    alertActor ! GetAlertStatus(statusProbe.ref)
    statusProbe.expectMessage((true, totalRequests * 10L))

    generateAndProcessLogs(alertActor, totalRequests, totalRequests * 10)
    alertActor ! PrintAlertMessage
    alertActor ! GetAlertStatus(statusProbe.ref)
    statusProbe.expectMessage((true, totalRequests * 10L))
  }
}
