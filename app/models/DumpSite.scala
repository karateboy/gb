package models
import play.api._
import play.api.Play.current
import play.api.libs.json._
import models.ModelHelper._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import org.apache.poi.openxml4j.opc._
import org.apache.poi.xssf.usermodel._
import com.github.nscala_time.time.Imports._

import java.io._
import java.nio.file.Files
import java.nio.file._
import java.util.Date

import org.mongodb.scala.model._
import org.mongodb.scala.model.Indexes._
import org.mongodb.scala.bson._
import MongoDB._

case class DumpSiteID(county: String, dirNo: String, wpType: Int = WorkPointType.DumpSite.id) extends IWorkPointID
case class DumpSite(_id: DumpSiteID, name: String, contact: String, phone: String, addr: String,
                    feature: String, siteType: String, area: Double, notes: Seq[Note] = Seq.empty[Note],
                    var location: Option[Seq[Double]] = None, owner: Option[String] = None, state: Option[String] = None, dm: Boolean = false) extends IWorkPoint {
  def getSummary = {
    val content = s"${feature}<br>" +
      s"${siteType}<br>" +
      s"${addr}<br>" +
      s"${area}平方公尺"
    Summary(name, content)
  }
}
object DumpSite {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val codecRegistry = fromRegistries(fromProviders(classOf[DumpSite], classOf[DumpSiteID], classOf[Note]), DEFAULT_CODEC_REGISTRY)

  val ColName = WorkPoint.ColName
  val collection = MongoDB.database.getCollection[DumpSite](WorkPoint.ColName).withCodecRegistry(codecRegistry)

  import WorkPoint._

  implicit val dpIdRead = Json.reads[DumpSiteID]
  implicit val dpIdWrite = Json.writes[DumpSiteID]
  implicit val dpWrite = Json.writes[DumpSite]

  def init(colNames: Seq[String]) {
    val docF = SysConfig.get(SysConfig.ImportDumpSite)
    for (v <- docF) {
      if (!v.asBoolean().getValue) {
        val path = current.path.getAbsolutePath + "/import/dumpSite.xlsx"
        if (ExcelTool.importXLSX(path)(parser)) {
          SysConfig.set(SysConfig.ImportDumpSite, new BsonBoolean(true))
          convertAddrToLocation
        }
      }
    }
  }

  def parser(sheet: XSSFSheet) {
    var dupmSiteSeq = IndexedSeq.empty[DumpSite]
    var rowN = 1
    var finishSheet = false

    do {
      var row = sheet.getRow(rowN)
      if (row == null)
        finishSheet = true
      else {
        try {
          val county = row.getCell(0).getStringCellValue
          val dirNo = row.getCell(1).getStringCellValue
          val name = row.getCell(2).getStringCellValue
          val contactNphone = row.getCell(3).getStringCellValue
          val contact = contactNphone.takeWhile { !_.isDigit }
          val phone = contactNphone.drop(contact.length())
          val addr = row.getCell(4).getStringCellValue
          val feature = row.getCell(5).getStringCellValue
          val siteType = row.getCell(6).getStringCellValue
          val area = row.getCell(7).getNumericCellValue
          val _id = DumpSiteID(county, dirNo)
          val ds = DumpSite(_id = _id, name = name, contact = contact, phone = phone, addr = addr,
            feature = feature, siteType = siteType, area = area)
          dupmSiteSeq = dupmSiteSeq.+:(ds)
        } catch {
          case ex: Throwable =>
            Logger.warn("end of import", ex)
            finishSheet = true
        }
      }
      rowN += 1
    } while (!finishSheet) //end of sheet

    import scala.concurrent._
    import org.mongodb.scala.model.Filters._
    val writeModelSeq = dupmSiteSeq.map { ds =>
      InsertOneModel(ds)
    }

    val f = collection.bulkWrite(writeModelSeq, new BulkWriteOptions().ordered(false)).toFuture()

    import scala.util._

    f.onComplete {
      case Success(x) =>
        Logger.info(s"Success ${x.getInsertedCount} inserted.")
      case Failure(ex) =>
        ex match {
          case x: org.mongodb.scala.MongoBulkWriteException =>
            Logger.warn(x.getWriteResult.toString)
          case _: Throwable =>
            Logger.error(s"bulk Insert failed ${ex.getMessage}")

        }
    }
  }

  def convertAddrToLocation() = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._

    val noLocationListF = collection.find(Filters.and(
      Filters.eq("_id.wpType", WorkPointType.DumpSite.id),
      Filters.eq("location", null))).toFuture()
    for (noLocationList <- noLocationListF) {
      var failed = 0
      Logger.info(s"no location list #=${noLocationList.length}")
      noLocationList.map {
        dumpSite =>
          val locationList = GoogleApi.queryAddr(dumpSite.addr)
          if (!locationList.isEmpty) {
            val location = locationList(0)
            dumpSite.location = Some(location)
            val f = collection.updateOne(Filters.eq("_id", dumpSite._id), Updates.set("location", location)).toFuture()
            f.onFailure(errorHandler)
            Logger.info(s"${dumpSite.addr} 轉換成功!")
          } else {
            failed += 1
            Logger.warn(s"${dumpSite.addr} 無法轉換!")
          }
      }
      Logger.info(s"共 ${failed} 筆無法轉換")
    }
  }

  def top3Near(location: Seq[Double]) = {
    val geometry = geojson.Point(geojson.Position(location: _*))
    val filter = Filters.nearSphere("location", geometry)
    val f = collection.find(WorkPoint.wpFilter(WorkPointType.DumpSite.id)(filter)).limit(3).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def getDumpSite(_id: DumpSiteID) = {
    val f = collection.find(Filters.eq("_id", _id)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def getDumpSiteList(ids: Seq[DumpSiteID]) = {
    val f = collection.find(Filters.in("_id", ids: _*)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def getSummaryMap(ids: Seq[DumpSiteID]) = {
    val f = getDumpSiteList(ids)
    for (list <- f) yield {
      val pair =
        for (d <- list) yield d._id -> d.getSummary

      pair.toMap
    }
  }

  def populateSummary(workPointList: Seq[WorkPoint]) = {
    import scala.language.postfixOps
    val idMap =
      workPointList.filter { wp =>
        wp._id("wpType").asInt32().getValue == WorkPointType.DumpSite.id
      } map {
        wp =>
          Json.parse(wp._id.toJson()).validate[DumpSiteID].asOpt.get -> wp
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