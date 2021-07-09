package rminhas.models

case class LogEntry(remoteHost: String,
                    rfc931: String,
                    authUser: String,
                    date: Long,
                    request: String,
                    status: Int,
                    bytes: Int)

object LogEntry {
  private val logLinePattern =
    """^"(\S+)","(\S+)","(\S+)",(\d+),"\S+ (\S+) \S+",(\d+),(\d+)$""".r

  def stringToLogEntry(string: String): Option[LogEntry] =
    string match {
      case logLinePattern(host, rfc931, authUser, date, request, status, bytes) =>
        Some(LogEntry(host, rfc931, authUser, date.toLong, request, status.toInt, bytes.toInt))
      case _ => None
    }
}
