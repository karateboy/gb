package models
import play.api._
import com.github.nscala_time.time.Imports._
import models.ModelHelper._
import models._
import org.mongodb.scala.bson.Document
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import play.api.Play.current
import org.apache.poi.openxml4j.opc._
import org.apache.poi.xssf.usermodel._
import com.github.nscala_time.time.Imports._
import java.io._
import java.nio.file.Files
import java.nio.file._
import org.apache.poi.ss.usermodel._
import java.util.Date

case class BuildCase(_id: String, county: String, name: String,
                     architect: String, area: Double, addr: String, date: Date,
                     var location: Option[Seq[Double]])
case class QueryBuildCaseParam(county: Option[String], name: Option[String],
                               architect: Option[String], addr: Option[String])

object BuildCase {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val codecRegistry = fromRegistries(fromProviders(classOf[BuildCase]), DEFAULT_CODEC_REGISTRY)

  val ColName = "buildCase"
  val collection = MongoDB.database.getCollection[BuildCase](ColName).withCodecRegistry(codecRegistry)

  import org.mongodb.scala.model.Indexes._
  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      f.onSuccess({
        case x =>
          val cf1 = collection.createIndex(ascending("county", "name")).toFuture()
          val cf2 = collection.createIndex(ascending("architect")).toFuture()

          cf1.onFailure(errorHandler)
          cf2.onFailure(errorHandler)

          import scala.concurrent._
          val endF = Future.sequence(Seq(cf1, cf2))
          endF.onComplete({
            case x =>
              importXLSX(path)(parser)
          })
      })
    }
  }

  val path = current.path.getAbsolutePath + "/import/"
  import java.io.File
  def parser(sheet: XSSFSheet) {
    var rowN = 2
    var finish = false
    var seq = IndexedSeq.empty[BuildCase]
    do {
      var row = sheet.getRow(rowN)
      if (row == null)
        finish = true
      else {
        try {
          import com.github.nscala_time.time.Imports._
          val idStr = row.getCell(0).getStringCellValue
          val id = idStr.takeWhile { _ != '(' }.trim()
          val dateRegex = """\((.*?)\)""".r
          val dateStr = dateRegex.findFirstIn(idStr).get.drop(1).reverse.drop(1).reverse
          val date = new DateTime(dateStr).toDate()
          val county = row.getCell(1).getStringCellValue
          val builderName = row.getCell(2).getStringCellValue
          val architect = row.getCell(3).getStringCellValue
          val area = row.getCell(4).getNumericCellValue
          val addr = row.getCell(5).getStringCellValue
          val location = Seq(row.getCell(6).getNumericCellValue,
            row.getCell(7).getNumericCellValue)

          val buildCase = BuildCase(_id = s"$county#$id",
            county = county,
            name = builderName,
            architect = architect,
            area = area,
            addr = addr,
            date = date,
            location = Some(location))
          seq = seq :+ buildCase
        } catch {
          case ex: Throwable =>
            Logger.error("failed to convert...", ex)
        }
      }
      rowN += 1
    } while (!finish)

    val f = collection.insertMany(seq).toFuture()
    f.onFailure(errorHandler)
    f.onSuccess({
      case ret =>
        Logger.info(s"Success import buildCase.xlsx")
    })
  }

  def importXLSX(dir: String)(parser: (XSSFSheet) => Unit) = {
    //Open Excel
    val pkg = OPCPackage.open(new FileInputStream(dir + "buildCase.xlsx"))
    val wb = new XSSFWorkbook(pkg);

    val sheet = wb.getSheetAt(0)
    parser(sheet)
  }

  /*
   * case class QueryBuildCaseParam(county: Option[String], name: Option[String],
                               architect: Option[String], addr: Option[String], alarm2: Option[Long], alarm3: Option[Long])
   * 
   */

  def getFilter(param: QueryBuildCaseParam) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._

    val countyFilter = param.county map { county => regex("county", county) }
    val nameFilter = param.name map { name => regex("name", name) }
    val addrFilter = param.addr map { district => regex("addr", district) }

    val filterList = List(countyFilter, nameFilter, addrFilter).flatMap { f => f }

    val filter = if (!filterList.isEmpty)
      and(filterList: _*)
    else
      Filters.exists("_id")

    filter
  }

  def queryBuildCase(param: QueryBuildCaseParam)(skip: Int, limit: Int) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._

    val filter = getFilter(param)

    val f = collection.find(filter).sort(Sorts.ascending("_id")).skip(skip).limit(limit).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f) yield {
      records
    }
  }

  def queryBuildCaseCount(param: QueryBuildCaseParam) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._

    val filter = getFilter(param)

    val f = collection.count(filter).toFuture()
    f.onFailure {
      errorHandler
    }
    for (count <- f) yield count
  }

  def convertAddrToLocation() = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._

    val noLocationListF = collection.find(Filters.exists("location", false)).toFuture()
    for (noLocationList <- noLocationListF) {
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
            Logger.info(".")
          } else {
            Logger.warn(s"${careHouse.addr} 無法轉換!")
          }
      }
    }
  }
}