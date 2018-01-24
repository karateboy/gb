package models
import play.api._
import play.api.libs.json._
import com.github.nscala_time.time.Imports._
import models.ModelHelper._
import models.ExcelTool._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import play.api.Play.current
import org.apache.poi.openxml4j.opc._
import org.apache.poi.xssf.usermodel._
import org.apache.poi.ss.usermodel._
import org.mongodb.scala.model._

case class TankID(addr: String, wpType: Int = WorkPointType.Tank.id) extends IWorkPointID
case class Tank(_id: TankID, county: String, count: Int,
                var location: Option[Seq[Double]] = None, in: Seq[Input] = Seq.empty[Input], out: Seq[Output] = Seq.empty[Output],
                notes: Seq[Note] = Seq.empty[Note], tag: Seq[String] = Seq.empty[String],
                owner: Option[String] = None, state: Option[String] = None) extends IWorkPoint {
  def getSummary = {
    val content = s"${county}<br>" +
      s"${count}油槽"
    Summary(_id.addr, content)
  }
}
object Tank {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }
  import java.io.File
  import org.mongodb.scala.model._
  import org.mongodb.scala.bson._
  case class QueryParam(
    bedGT: Option[Int] = None, bedLT: Option[Int] = None,
    tag: Option[Seq[String]] = None,
    state: Option[String] = None,
    var owner: Option[String] = None,
    keyword: Option[String] = None,
    sortBy: String = "count+")

  val codecRegistry = fromRegistries(
    fromProviders(classOf[Tank], classOf[TankID], classOf[Note], classOf[Input], classOf[Output]), DEFAULT_CODEC_REGISTRY)

  val ColName = WorkPoint.ColName
  val collection = MongoDB.database.getCollection[Tank](WorkPoint.ColName).withCodecRegistry(codecRegistry)

  def init() {
    for (ret <- SysConfig.get(SysConfig.ImportTank)) {
      if (!ret.asBoolean().getValue) {
        val path = current.path.getAbsolutePath + "/import/tank.xlsx"
        importXLSX(path)(parser)
        convertAddrToLocation()
        SysConfig.set(SysConfig.ImportTank, new BsonBoolean(true))
      }
    }
  }

  import java.io.File
  def parser(sheet: XSSFSheet) = {
    var rowN = 2
    var finish = false
    var seq = IndexedSeq.empty[Tank]
    do {
      var row = sheet.getRow(rowN)
      if (row == null)
        finish = true
      else {
        try {
          val county = row.getCell(0).getStringCellValue
          val addr = row.getCell(1).getStringCellValue
          val x = row.getCell(2).getNumericCellValue
          val y = row.getCell(3).getNumericCellValue
          val lonlat = CoordinateTransform.tWD97_To_lonlat(x, y)
          val location = Seq(lonlat._1, lonlat._2)
          val count = row.getCell(4).getNumericCellValue.toInt

          val tankCase = Tank(_id = TankID(addr),
            county = county,
            count = count,
            location = Some(location))
          seq = seq :+ tankCase
        } catch {
          case ex: java.lang.NullPointerException =>
          // last row Ignore it...

          case ex: Throwable =>
            Logger.error(s"failed to convert row=$rowN...", ex)
        }
      }
      rowN += 1
    } while (!finish)

    val f = collection.insertMany(seq, InsertManyOptions().ordered(false)).toFuture()
    f.onFailure(errorHandler)
    waitReadyResult(f)
    f
  }

  def convertAddrToLocation() = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._
    import WorkPoint.wpFilter

    val tankListF = collection.find(wpFilter(WorkPointType.Tank.id)()).toFuture()
    for (tankList <- tankListF) {
      var failed = 0
      Logger.info(s"油槽數 #=${tankList.length}")
      tankList.map {
        tank =>
          val locationList = GoogleApi.queryAddr(tank._id.addr)
          if (!locationList.isEmpty) {
            val location = locationList(0)
            tank.location = Some(location)
            val f = collection.replaceOne(Filters.eq("_id", tank._id), tank).toFuture()
            f.onFailure(errorHandler)
            Logger.info(s"${tank._id.addr} 轉換成功!")
          } else {
            failed += 1
            Logger.warn(s"${tank._id.addr} 無法轉換!")
          }
      }
      Logger.info(s"共 ${failed} 筆無法轉換")
    }
  }

  def getList(ids: Seq[TankID]) = {
    val f = collection.find(Filters.in("_id", ids: _*)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def getSummaryMap(ids: Seq[TankID]) = {
    val f = getList(ids)
    for (list <- f) yield {
      val pair =
        for (d <- list) yield d._id -> d.getSummary

      pair.toMap
    }
  }

  implicit val idRead = Json.reads[TankID]
  def populateSummary(workPointList: Seq[WorkPoint]) = {
    import scala.language.postfixOps
    val idMap =
      workPointList.filter { wp =>
        wp._id("wpType").asInt32().getValue == WorkPointType.Tank.id
      } map {
        wp =>
          Json.parse(wp._id.toJson()).validate[TankID].asOpt.get -> wp
      } toMap

    val summaryMapF = getSummaryMap(idMap.keys.toSeq)
    for (summaryMap <- summaryMapF) yield {
      for ((id, summary) <- summaryMap) yield {
        val wp = idMap(id)
        wp.summary = Some(summary)
        wp
      }
    }
  }
}