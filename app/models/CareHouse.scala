package models
import play.api._
import com.github.nscala_time.time.Imports._
import models.ModelHelper._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import play.api.Play.current

case class CareHouseID(county: String, name: String, wpType: Int = WorkPointType.CareHouse.id) extends IWorkPointID

case class CareHouse(_id: CareHouseID, addr: String, serviceType: Seq[String],
                     phone: String, fax: String, email: String, bed: Int,
                     var location: Option[Seq[Double]] = None, in: Seq[Input] = Seq.empty[Input],
                     out: Seq[Output] = Seq.empty[Output], notes: Seq[Note] = Seq.empty[Note],
                     tag: Seq[String] = Seq.empty[String], owner: Option[String] = None) extends IWorkPoint

object CareHouse {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }
  import java.io.File
  import org.mongodb.scala.model._
  import org.mongodb.scala.bson._

  val codecRegistry = fromRegistries(
    fromProviders(classOf[CareHouse], classOf[CareHouseID], classOf[Note], classOf[Input], classOf[Output]), DEFAULT_CODEC_REGISTRY)

  val ColName = WorkPoint.ColName
  val collection = MongoDB.database.getCollection[CareHouse](WorkPoint.ColName).withCodecRegistry(codecRegistry)

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
    }

    val f = SysConfig.get(SysConfig.ImportCareHouse)
    for (imported <- f) {
      if (!imported.asBoolean().getValue) {
        if (importRecord){
          convertAddrToLocation()
          SysConfig.set(SysConfig.ImportCareHouse, new BsonBoolean(true))
        }
      }
    }
  }

  def importRecord() = {
    val path = current.path.getAbsolutePath + "/import/"

    val ret1 = ExcelTool.importXLSX(path + "一般護理之家.xlsx")(parser)
    val ret2 = ExcelTool.importXLSX(path + "養護型機構.xlsx")(parser)
    val ret3 = ExcelTool.importXLSX(path + "精神護理之家.xlsx")(parser)
    val ret4 = ExcelTool.importXLSX(path + "長期照護型機構.xlsx")(parser)
    val ret5 = ExcelTool.importXLSX(path + "安養服務.xlsx")(parser)
    val ret6 = ExcelTool.importXLSX(path + "日間照顧服務.xlsx")(parser)

    ret1 && ret2 && ret3 && ret4 && ret6
  }

  import org.apache.poi.openxml4j.opc._
  import org.apache.poi.xssf.usermodel._
  def parser(sheet: XSSFSheet) = {
    var rowN = 1
    var finish = false
    var seq = IndexedSeq.empty[CareHouse]
    do {
      var row = sheet.getRow(rowN)
      if (row == null)
        finish = true
      else {
        try {
          val name = row.getCell(0).getStringCellValue
          val county = row.getCell(1).getStringCellValue
          val _id = CareHouseID(county, name)
          val addr = row.getCell(2).getStringCellValue
          val serviceType = row.getCell(3).getStringCellValue.split(",")
          val phone = row.getCell(4).getStringCellValue
          val fax = row.getCell(5).getStringCellValue
          val email = row.getCell(6).getStringCellValue
          val bed = row.getCell(7).getNumericCellValue.toInt
          val ch = CareHouse(_id = _id, addr = addr, serviceType = serviceType, phone = phone, fax = fax, email = email, bed = bed)

          seq = seq :+ ch
        } catch {
          case ex: java.lang.NullPointerException =>
          // last row Ignore it...

          case ex: Throwable =>
            Logger.error(s"failed to convert row=$rowN...", ex)
        }
      }
      rowN += 1
    } while (!finish)

    seq.map { bc =>

    }

    val writeModelSeq = seq.map { ch =>
      ReplaceOneModel(Filters.eq("_id", ch._id), ch, UpdateOptions().upsert(true))
    }

    val f = collection.bulkWrite(writeModelSeq, new BulkWriteOptions().ordered(false)).toFuture()

    f.onFailure(errorHandler)
    waitReadyResult(f)
  }

    def convertAddrToLocation() = {
      import org.mongodb.scala.model.Filters._
      import org.mongodb.scala.model._
      import WorkPoint.wpFilter
      
      val noLocationListF = collection.find(wpFilter(WorkPointType.CareHouse.id)(Filters.exists("location", false))).toFuture()
      for (noLocationList <- noLocationListF) {
        var failed = 0
        Logger.info(s"no location list #=${noLocationList.length}")
        noLocationList.map {
          careHouse =>
            assert(careHouse.location.isEmpty)
            val locationList = GoogleApi.queryAddr(careHouse.addr)
            if (!locationList.isEmpty) {
              val location = locationList(0)
              careHouse.location = Some(location)
              val f = collection.updateOne(Filters.eq("_id", careHouse._id), Updates.set("location", location)).toFuture()
              f.onFailure(errorHandler)
              Logger.info(s"${careHouse.addr} 轉換成功!")
            } else {
              failed += 1
              Logger.warn(s"${careHouse.addr} 無法轉換!")
            }
        }
        Logger.info(s"共 ${failed} 筆無法轉換")
      }
    }
}