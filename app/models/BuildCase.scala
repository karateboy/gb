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

case class BuildCase(_id: String, county: String, name: String,
                     architect: String, area: Double, addr: Option[String], alarm2: Option[Long], alarm3: Option[Long],
                     var location: Option[Seq[Double]])
case class QueryBuildCaseParam(county: Option[String], name: Option[String],
                               architect: Option[String], addr: Option[String], alarm2: Option[Long], alarm3: Option[Long])

object BuildCase {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val codecRegistry = fromRegistries(fromProviders(classOf[BuildCase]), DEFAULT_CODEC_REGISTRY)

  val ColName = "buildCase"
  val collection = MongoDB.database.getCollection[CareHouse](ColName).withCodecRegistry(codecRegistry)

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

  //FIXME
  val path = current.path.getAbsolutePath + "/import/"
  import java.io.File
  def parser(sheet: XSSFSheet, county: String) {
    var rowN = 2
    var finish = false
    var seq = IndexedSeq.empty[CareHouse]
    do {
      var row = sheet.getRow(rowN)
      if (row == null)
        finish = true
      else {
        val isPublic = row.getCell(1).getStringCellValue == "公立"
        val name = row.getCell(2).getStringCellValue
        val principal = row.getCell(3).getStringCellValue
        val district = row.getCell(4).getStringCellValue
        val addr = row.getCell(5).getStringCellValue
        val phone = try {
          row.getCell(6).getStringCellValue
        } catch {
          case ex: java.lang.IllegalStateException =>
            row.getCell(6).getNumericCellValue.toString()
        }

        def getCareTypes() = {
          val careTypes = row.getCell(7).getStringCellValue.split("\n")
          val careNums = try {
            val num = row.getCell(8).getNumericCellValue.toInt
            Array(num)
          } catch {
            case ex: Throwable =>
              row.getCell(8).getStringCellValue.split("\n").map {
                str =>
                  try {
                    str.toInt
                  } catch {
                    case ex: Throwable =>
                      0
                  }
              }
          }

          if (careTypes.length <= careNums.length) {
            for (idx <- 0 to careTypes.length - 1)
              yield CareType(careTypes(idx), careNums(idx))
          } else {
            for (idx <- 0 to careTypes.length - 1)
              yield CareType(careTypes(idx), careNums(0))
          }
        }
        def getBeds() = {
          //管(\d+)床
          val bedRegEx = """\u7ba1(\d+)\u5e8a""".r.unanchored
          try {
            val cell8 = row.getCell(8).getStringCellValue
            cell8 match {
              case bedRegEx(bed) =>
                Some(bed.toInt)
              case _ =>
                None
            }
          } catch {
            case _: Throwable =>
              None
          }

        }

        val careHouse = CareHouse(_id = s"$county#$name",
          isPublic = isPublic,
          county = county,
          name = name,
          principal = principal,
          district = district,
          addr = addr,
          phone = phone,
          careTypes = getCareTypes,
          beds = getBeds,
          waste = None,
          location = None)

        //Logger.debug(careHouse.toString)
        seq = seq :+ careHouse
      }
      rowN += 1
    } while (!finish)

    val f = collection.insertMany(seq).toFuture()
    f.onFailure(errorHandler)
    f.onSuccess({
      case ret =>
        Logger.info(s"Success import $county")
    })
  }

  def importXLSX(dir: String)(parser: (XSSFSheet, String) => Unit) = {
    //Open Excel
    val pkg = OPCPackage.open(new FileInputStream(dir + "buildCase.xlsx"))
    val wb = new XSSFWorkbook(pkg);

    for {
      idx <- 0 to 21
      sheet = wb.getSheetAt(idx)
      county = wb.getSheetName(idx)
    } {
      Logger.debug(s"$county")
      parser(sheet, county)
    }
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
    val alarm2Filter = param.alarm2 map { alarm2 => gt("alarm2", alarm2) }
    val alarm3Filter = param.alarm3 map { alarm3 => gt("alarm3", alarm3) }

    val filterList = List(countyFilter, nameFilter, addrFilter, alarm2Filter, alarm3Filter).flatMap { f => f }

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