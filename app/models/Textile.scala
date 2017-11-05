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

case class Textile(_id: String, name: String, addr: String, industry: String,
                    location: Option[Seq[Double]] = None, odor:Option[String]=None,
                    quantity: Option[Double] = None, dried:Option[Boolean]=None, 
                    moisture: Option[Double] = None, recycled:Option[Boolean]=None,
                    sales: Option[String] = None, contracted:Boolean=false,
                    lastUpdate: Option[Date] = None)

case class QueryTextileParam(name: Option[String], addr: Option[String])

object Textile {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val codecRegistry = fromRegistries(fromProviders(classOf[Textile]), DEFAULT_CODEC_REGISTRY)

  val ColName = "textile"
  val collection = MongoDB.database.getCollection[Textile](ColName).withCodecRegistry(codecRegistry)

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      f.onSuccess({
        case x =>
          val cf1 = collection.createIndex(ascending("name")).toFuture()
          val cf2 = collection.createIndex(ascending("addr")).toFuture()

          cf1.onFailure(errorHandler)
          cf2.onFailure(errorHandler)

          import scala.concurrent._
          val endF = Future.sequence(Seq(cf1, cf2))
          endF.onComplete({
            case x =>
              val path = current.path.getAbsolutePath + "/import/textile.xlsx"
              importXLSX(path)(parser)
          })
      })
    }
  }
  //
  import java.io.File
  def parser(sheet: XSSFSheet) {
    var rowN = 1
    var finish = false
    var seq = IndexedSeq.empty[Textile]
    do {
      var row = sheet.getRow(rowN)
      if (row == null)
        finish = true
      else {
        try {
          import com.github.nscala_time.time.Imports._
          val id = row.getCell(0).getStringCellValue
          val name = row.getCell(1).getStringCellValue
          val addr = row.getCell(2).getStringCellValue
          val industry = row.getCell(5).getStringCellValue
          val locationList = GoogleApi.queryAddr(addr)
          val location = if (locationList.isEmpty)
            None
          else
            Some(locationList(0))
            
          val purifier = Textile(_id = id,
            name = name,
            addr = addr,
            industry = industry,
            location = location)
          seq = seq :+ purifier
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

  def getFilter(param: QueryTextileParam) = {
    import org.mongodb.scala.model.Filters._
    val addrFilter = param.addr map { addr => regex("addr", "(?i)" + addr) }

    val filterList = List(addrFilter).flatMap { f => f }

    val filter = if (!filterList.isEmpty)
      and(filterList: _*)
    else
      Filters.exists("_id")

    filter
  }

  import org.mongodb.scala.model._

  def query(param: QueryTextileParam)(skip: Int, limit: Int) = {
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

  def queryCount(param: QueryTextileParam) = {
    val filter = getFilter(param)

    val f = collection.count(filter).toFuture()
    f.onFailure {
      errorHandler
    }
    for (count <- f) yield count
  }

  def upsert(_id: String, textile: Textile) = {
    val f = collection.replaceOne(Filters.eq("_id", _id), textile, UpdateOptions().upsert(true)).toFuture()
    f.onFailure {
      errorHandler
    }
    f
  }
}