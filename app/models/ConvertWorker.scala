package models
import play.api._
import akka.actor._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import ModelHelper._
import scala.concurrent.ExecutionContext.Implicits.global

object ConvertWorker {
  case object StartConvert
  var count = 0
  def start()(implicit context: ActorSystem) = {
    val prop = Props(classOf[ConvertWorker])
    val worker = context.actorOf(prop, name = "Converter" + count)
    count += 1
    worker ! StartConvert
  }
}

class ConvertWorker() extends Actor with ActorLogging {
  import ConvertWorker._
  def receive = handler

  def handler: Receive = {
    case StartConvert =>
      Logger.info(s"Start convert careHouse location...")
      CareHouse.convertAddrToLocation
      self ! PoisonPill
  }
}