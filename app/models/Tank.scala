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

case class Tank(_id: String, county: String, addr: String,
                tank: Int, location: Option[Seq[Double]] = None,
                quantity: Option[Double] = None, brand: Option[String] = None, usage: Option[Double] = None,
                frequency: Option[Int] = None, discount: Option[Double] = None, sales: Option[String] = None,
                lastUpdate: Option[Date] = None)

case class QueryTankParam(name: Option[String],
                          addr: Option[String], county: Option[String], tankGT: Option[Int])

object Tank {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val codecRegistry = fromRegistries(fromProviders(classOf[Tank]), DEFAULT_CODEC_REGISTRY)

  val ColName = "tank"
  val collection = MongoDB.database.getCollection[Tank](ColName).withCodecRegistry(codecRegistry)

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      f.onSuccess({
        case x =>
          val cf1 = collection.createIndex(ascending("county", "addr")).toFuture()
          val cf2 = collection.createIndex(ascending("addr")).toFuture()

          cf1.onFailure(errorHandler)
          cf2.onFailure(errorHandler)

          import scala.concurrent._
          val endF = Future.sequence(Seq(cf1, cf2))
          endF.onComplete({
            case x =>
              val path = current.path.getAbsolutePath + "/import/tank.xlsx"
              importXLSX(path)(parser)
          })
      })
    }
  }
  //
  import java.io.File
  def parser(sheet: XSSFSheet) {
    var rowN = 2
    var finish = false
    var seq = IndexedSeq.empty[Tank]
    do {
      var row = sheet.getRow(rowN)
      if (row == null)
        finish = true
      else {
        try {
          import com.github.nscala_time.time.Imports._
          val county = row.getCell(0).getStringCellValue
          val addr = row.getCell(1).getStringCellValue
          val id = addr
          val x = row.getCell(2).getNumericCellValue
          val y = row.getCell(3).getNumericCellValue
          val lonlat = CoordinateTransform.tWD97_To_lonlat(x, y)
          val location = Seq(lonlat._1, lonlat._2)
          val tank = row.getCell(4).getNumericCellValue.toInt

          val tankCase = Tank(_id = id,
            county = county,
            addr = addr,
            location = Some(location),
            tank = tank)
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

    import scala.concurrent._
    val seqF = seq.map { bc =>
      collection.replaceOne(Filters.eq("_id", bc._id), bc, UpdateOptions().upsert(true)).toFuture()
    }
    val f = Future.sequence(seqF)
    f.onFailure(errorHandler)
  }

  def getFilter(param: QueryTankParam) = {
    import org.mongodb.scala.model.Filters._
    val addrFilter = param.addr map { addr => regex("addr", "(?i)" + addr) }
    val countyFilter = param.county map { county => regex("county", "(?i)" + county) }
    val tankGtFilter = param.tankGT map { v => Filters.gt("tank", v) }

    val filterList = List(addrFilter,
      countyFilter, tankGtFilter).flatMap { f => f }

    val filter = if (!filterList.isEmpty)
      and(filterList: _*)
    else
      Filters.exists("_id")

    filter
  }

  import org.mongodb.scala.model._

  def query(param: QueryTankParam)(skip: Int, limit: Int) = {
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

  def queryCount(param: QueryTankParam) = {
    val filter = getFilter(param)

    val f = collection.count(filter).toFuture()
    f.onFailure {
      errorHandler
    }
    for (count <- f) yield count
  }

  def upsert(_id: String, tank: Tank) = {
    val f = collection.replaceOne(Filters.eq("_id", _id), tank, UpdateOptions().upsert(true)).toFuture()
    f.onFailure {
      errorHandler
    }
    f
  }
}