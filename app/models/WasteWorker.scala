package models
import play.api._
import akka.actor._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import ModelHelper._
import scala.concurrent.ExecutionContext.Implicits.global

object WasteWorker {
  case object Start
  var count = 0
  def start()(implicit context: ActorSystem) = {
    val prop = Props(classOf[WasteWorker])
    val worker = context.actorOf(prop, name = "Waste" + count)
    count += 1
    worker ! Start
  }
}

class WasteWorker() extends Actor with ActorLogging {
  import WasteWorker._
  def receive = handler

  def handler: Receive = {
    case Start =>
      Logger.info(s"開始抓取廢棄物網頁...")
      grabber

  }

  def grabber = {
    val listF = Facility.getList
    for (list <- listF) {
      Logger.info(s"總共 ${list.length}")
      var success = 0
      var failed = 0
      for (facility <- list) {
        if (facility.addr.isEmpty || facility.location.isEmpty) {
          if (Facility.wasteGrabber(facility._id)) {
            success += 1
            Logger.info(s"成功處理 ${facility._id} ${facility.name}")
          } else
            failed += 1
        }
      }
      Logger.info(s"結束抓取 成功=${success} 失敗=${failed}")
      self ! PoisonPill
    }
  }
}