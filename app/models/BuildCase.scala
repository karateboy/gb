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
import org.mongodb.scala.model._
import org.mongodb.scala.model.Indexes._

case class BuildCase(_id: String, county: String, name: String,
                     architect: String, area: Double, addr: String, date: Date,
                     var location: Option[Seq[Double]],
                     builder: Option[String] = None, phone: Option[String] = None, contracted: Boolean = false,
                     lastVisit: Option[Date] = None, sales: Option[String] = None)

case class QueryBuildCaseParam(name: Option[String],
                               architect: Option[String], addr: Option[String], county: Option[String],
                               areaGT: Option[Double], areaLT: Option[Double], yellowAlert: Option[Boolean],
                               redAlert: Option[Boolean], contracted: Option[Boolean], sales: Option[String])

object BuildCase {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val codecRegistry = fromRegistries(fromProviders(classOf[BuildCase]), DEFAULT_CODEC_REGISTRY)

  val ColName = "buildCase"
  val collection = MongoDB.database.getCollection[BuildCase](ColName).withCodecRegistry(codecRegistry)

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

          val buildCase = BuildCase(_id = addr,
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

    import scala.concurrent._
    val seqF = seq.map { bc =>
      collection.replaceOne(Filters.eq("_id", bc._id), bc, UpdateOptions().upsert(true)).toFuture()
    }
    val f = Future.sequence(seqF)
    f.onFailure(errorHandler)
    f.onSuccess({
      case ret =>
        Logger.info(s"Success import buildCase.xlsx")
    })
  }

  def importXLSX(dir: String)(parser: (XSSFSheet) => Unit) = {
    //Open Excel
    try {
      val file = new File(dir + "buildCase.xlsx")
      val fs = new FileInputStream(file)
      val pkg = OPCPackage.open(fs)
      val wb = new XSSFWorkbook(pkg);

      val sheet = wb.getSheetAt(0)
      parser(sheet)
      fs.close()
      file.delete()
      Logger.info("Success import buildCase.xlsx")
    } catch {
      case ex: FileNotFoundException =>
      //Ignore it
      case ex: Throwable =>
        Logger.error("Fail to import buildCase.xlsx", ex)
    }
  }

  def getFilter(param: QueryBuildCaseParam) = {
    import org.mongodb.scala.model.Filters._

    /*
     * case class QueryBuildCaseParam(name: Option[String],
                               architect: Option[String], addr: Option[String], county: Option[String],
                               areaGT: Option[Double], areaLT: Option[Double], yellowAlert: Option[Boolean],
                               redAlert: Option[Double], contracted: Option[Boolean], sales: Option[String])
     * 
     * */

    val nameFilter = param.name map { name => regex("name", "(?i)" + name) }
    val architectFilter = param.architect map { architect => regex("architect", "(?i)" + architect) }
    val addrFilter = param.addr map { addr => regex("addr", "(?i)" + addr) }
    val countyFilter = param.county map { county => regex("county", "(?i)" + county) }
    val areaGtFilter = param.areaGT map { v => Filters.gt("area", v) }
    val areaLtFilter = param.areaLT map { v => Filters.lt("area", v) }
    val contractedFilter = param.contracted map {
      v =>
        if (v)
          equal("contracted", v)
        else
          equal("contracted", v)
    }

    val salesFilter = param.sales map {
      sales =>
        regex("sales", "(?i)" + sales)
    }

    val yellowAlertFilter = param.yellowAlert map { v =>
      if (v == true) {
        val today = DateTime.now
        // today < date + 4*month
        // today - 4*month < date
        val yellowDue = today - 4.month
        and(lt("date", today.toLocalDate().toDate()), gt("date", yellowDue.toLocalDate().toDate()))
      } else
        exists("_id")
    }

    val redAlertFilter = param.redAlert map { v =>
      if (v == true) {
        val today = DateTime.now
        // today > date + 4*month
        // today - 6*month < date
        val yellowDue = today - 4.month
        val redDue = today - 6.month
        and(gt("date", yellowDue.toLocalDate().toDate()), lt("date", redDue.toLocalDate().toDate()))
      } else
        exists("_id")
    }

    val filterList = List(nameFilter, architectFilter, addrFilter,
      countyFilter, areaGtFilter, areaLtFilter, redAlertFilter, yellowAlertFilter,
      contractedFilter, salesFilter).flatMap { f => f }

    val filter = if (!filterList.isEmpty)
      and(filterList: _*)
    else
      Filters.exists("_id")

    filter
  }

  import org.mongodb.scala.model._

  def queryBuildCase(param: QueryBuildCaseParam)(skip: Int, limit: Int) = {
    import org.mongodb.scala.model.Filters._

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
    val filter = getFilter(param)

    val f = collection.count(filter).toFuture()
    f.onFailure {
      errorHandler
    }
    for (count <- f) yield count
  }

  def upsertBuildCase(_id: String, buildCase: BuildCase) = {
    val f = collection.replaceOne(Filters.eq("_id", _id), buildCase, UpdateOptions().upsert(true)).toFuture()
    f.onFailure {
      errorHandler
    }
    f
  }

}