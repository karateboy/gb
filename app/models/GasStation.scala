package models
import play.api._
import com.github.nscala_time.time.Imports._
import models.ModelHelper._
import models.ExcelTool._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import play.api.Play.current
import org.apache.poi.openxml4j.opc._
import org.apache.poi.xssf.usermodel._
import org.apache.poi.ss.usermodel._
import java.util.Date
import org.mongodb.scala.model._
import org.mongodb.scala.model.Indexes._

case class GasStationID(id: String, name: String, wpType: Int = WorkPointType.GasStation.id) extends IWorkPointID
case class GasStation(_id: GasStationID, county: String, addr: String, count: Int,
                      var location: Option[Seq[Double]] = None, in: Seq[Input] = Seq.empty[Input], out: Seq[Output] = Seq.empty[Output],
                      notes: Seq[Note] = Seq.empty[Note], tag: Seq[String] = Seq.empty[String],
                      owner: Option[String] = None, state: Option[String] = None) extends IWorkPoint
object GasStation {
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
    fromProviders(classOf[GasStation], classOf[GasStationID], classOf[Note], classOf[Input], classOf[Output]), DEFAULT_CODEC_REGISTRY)

  val ColName = WorkPoint.ColName
  val collection = MongoDB.database.getCollection[GasStation](WorkPoint.ColName).withCodecRegistry(codecRegistry)

  def init() {
    for (ret <- SysConfig.get(SysConfig.ImportGasStation)) {
      if (!ret.asBoolean().getValue) {
        val path = current.path.getAbsolutePath + "/import/gasStation.xlsx"
        importXLSX(path)(parser)
        convertAddrToLocation()
        SysConfig.set(SysConfig.ImportGasStation, new BsonBoolean(true))
      }
    }
  }

  def convertAddrToLocation() = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._
    import WorkPoint.wpFilter

    val tankListF = collection.find(wpFilter(WorkPointType.GasStation.id)()).toFuture()
    for (tankList <- tankListF) {
      var failed = 0
      Logger.info(s"加油站 #=${tankList.length}")
      tankList.map {
        tank =>
          val locationList = GoogleApi.queryAddr(tank.addr)
          if (!locationList.isEmpty) {
            val location = locationList(0)
            tank.location = Some(location)
            val f = collection.replaceOne(Filters.eq("_id", tank._id), tank).toFuture()
            f.onFailure(errorHandler)
            Logger.info(s"${tank.addr} 轉換成功!")
          } else {
            failed += 1
            Logger.warn(s"${tank.addr} 無法轉換!")
          }
      }
      Logger.info(s"共 ${failed} 筆無法轉換")
    }
  }

  import java.io.File
  def parser(sheet: XSSFSheet) = {
    var rowN = 2
    var finish = false
    var seq = Seq.empty[GasStation]
    do {
      var row = sheet.getRow(rowN)
      if (row == null)
        finish = true
      else {
        try {
          val id = getStrFromCell(row.getCell(0))
          val name = row.getCell(1).getStringCellValue
          val _id = GasStationID(id, name)

          val county = row.getCell(2).getStringCellValue
          val addr = row.getCell(3).getStringCellValue
          val x = getIntFromCell(row.getCell(4))
          val y = getIntFromCell(row.getCell(5))
          val count = getIntFromCell(row.getCell(6))
          val lonlat = CoordinateTransform.tWD97_To_lonlat(x, y)
          val location = Some(Seq(lonlat._1, lonlat._2))

          val gasStation = GasStation(_id = _id,
            county = county,
            addr = addr,
            count = count,
            location = location)
          seq = seq :+ gasStation
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
}