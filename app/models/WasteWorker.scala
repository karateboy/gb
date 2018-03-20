package models
import play.api._
import akka.actor._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import ModelHelper._
import scala.concurrent.ExecutionContext.Implicits.global

object WasteWorker {
  case object Start
  case object GrabFactoryInfo
  case object GrabProcessPlantInfo
  
  var count = 0
  def createWorker(implicit context: ActorSystem) = {
    val prop = Props(classOf[WasteWorker])
    val worker = context.actorOf(prop, name = "Waste" + count)
    count += 1
    worker
  }
  def start()(implicit context: ActorSystem) = {
    val worker = createWorker
    worker ! Start
  }
  
  def grabFactoryInfo()(implicit context: ActorSystem) = {
    val worker = createWorker
    worker ! GrabFactoryInfo
  }
  
  def grabProcessPlantInfo()(implicit context: ActorSystem) = {
    val worker = createWorker
    worker ! GrabProcessPlantInfo
  }
}

class WasteWorker() extends Actor with ActorLogging {
  import WasteWorker._
  def receive = handler

  def handler: Receive = {
    case Start =>
      Logger.info(s"開始抓取廢棄物網頁...")
      grabber
    case GrabFactoryInfo=>
      Logger.info(s"開始抓取工廠資訊...")
      grabFactoryInfo
    case GrabProcessPlantInfo=>
      Logger.info(s"開始抓取處理廠資訊...")
      grabProcessPlantInfo
  }

  def grabber = {
    val listF = Facility.getNoAddrList
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
  
  def grabFactoryInfo = {
    val listF = Facility.getFactoryList
    for (list <- listF) {
      Logger.info(s"總共 ${list.length}")
      var success = 0
      var failed = 0
      for (facility <- list) {
        if (facility.phone.isEmpty || facility.contact.isEmpty) {
          if (Facility.grabFactoryInfo(facility._id)) {
            success += 1
            Logger.info(s"成功處理 ${facility._id} ${facility.name} 工廠資訊")
          } else
            failed += 1
        }
      }
      Logger.info(s"結束抓取工廠資訊 成功=${success} 失敗=${failed}")
      self ! PoisonPill
    }
  }
  
  def grabProcessPlantInfo = {
    val listF = Facility.getProcessPlantList
    for (list <- listF) {
      Logger.info(s"總共 ${list.length}")
      var success = 0
      var failed = 0
      for (facility <- list) {
        if (facility.phone.isEmpty || facility.contact.isEmpty) {
          if (Facility.grabProcessPlantInfo(facility._id)) {
            success += 1
            Logger.info(s"成功處理 ${facility._id} ${facility.name} 處理廠資訊")
          } else
            failed += 1
        }
      }
      Logger.info(s"結束抓取處理廠資訊 成功=${success} 失敗=${failed}")
      self ! PoisonPill
    }
  }
}