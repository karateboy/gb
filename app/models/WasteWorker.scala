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
  case object ExportFactorySheet

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

  def exportFactorSheet()(implicit context: ActorSystem) = {
    val worker = createWorker
    worker ! ExportFactorySheet
  }
}

class WasteWorker() extends Actor with ActorLogging {
  import WasteWorker._
  def receive = handler

  def handler: Receive = {
    case Start =>
      grabber
    case GrabFactoryInfo =>
      grabFactoryInfo
    case GrabProcessPlantInfo =>
      grabProcessPlantInfo
    case ExportFactorySheet =>
      exportFactorySheet
  }

  def grabber = {
    val listF = Facility.grabWasteInfoList
    for (list <- listF) {
      Logger.info(s"開始抓取廢棄物網頁  ${list.length}")
      var success = 0
      var failed = 0
      for (facility <- list) {
        if (Facility.wasteGrabber(facility._id)) {
          success += 1
          Logger.info(s"成功處理 ${facility._id} ${facility.name}")
        } else
          failed += 1
      }
      Logger.info(s"結束抓取 成功=${success} 失敗=${failed}")
      self ! PoisonPill
    }
  }

  def grabFactoryInfo = {
    val listF = Facility.getFactoryList
    for (list <- listF) {
      Logger.info(s"開始抓取工廠資訊 總共 ${list.length}")
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
      Logger.info(s"開始抓取處理廠資訊 總共 ${list.length}")
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

  def exportFactorySheet() = {
    val listF = Facility.getFactoryListByPollutant
    var missingWasteCodeSet = Set.empty[String]
    for (list <- listF) {
      Logger.info(s"開始匯出工廠資訊 總共 ${list.length}")
      var no = 0
      for (facility <- list.take(1500)) {
        import controllers.ExcelUtility
        Logger.info(s"匯出${no}_${facility.name}")
        try {
          if (!facility.wasteOut.getOrElse(Seq.empty[WasteOutput]).isEmpty) {
            missingWasteCodeSet = missingWasteCodeSet ++
              ExcelUtility.exportFactorySheet(s"${no}_${facility.name}", facility)
            no += 1
          }
        } catch {
          case ex: Throwable =>
            Logger.error(s"無法匯出 ${facility.name}", ex)
        }
      }
      Logger.info(s"結束匯出有槽工廠表")
      for (wasteCode <- missingWasteCodeSet) {
        Logger.info(s"無法媒合${wasteCode}")
      }
      self ! PoisonPill
    }
  }
}