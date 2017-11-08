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

case class OilUser(_id: String, useType: String, name: String, county: String, addr: String,
                   tank: Int, location: Option[Seq[Double]] = None,
                   quantity: Option[Double] = None, brand: Option[String] = None, usage: Option[Double] = None,
                   frequency: Option[Int] = None, discount: Option[Double] = None, sales: Option[String] = None,
                   contracted: Option[Boolean] = None,
                   lastUpdate: Option[Date] = None, remark: Option[String] = None)

case class QueryOilUserParam(name: Option[String], useType: Option[Seq[String]],
                             addr: Option[String], county: Option[String], sales: Option[String], contracted: Option[Boolean])

object OilUser {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val codecRegistry = fromRegistries(fromProviders(classOf[OilUser]), DEFAULT_CODEC_REGISTRY)

  val ColName = "oilUser"
  val collection = MongoDB.database.getCollection[OilUser](ColName).withCodecRegistry(codecRegistry)

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      f.onSuccess({
        case x =>
          val cf1 = collection.createIndex(ascending("useType", "county", "addr")).toFuture()
          val cf2 = collection.createIndex(ascending("addr")).toFuture()

          cf1.onFailure(errorHandler)
          cf2.onFailure(errorHandler)

          import scala.concurrent._
          val endF = Future.sequence(Seq(cf1, cf2))
          endF.onComplete({
            case x =>
              GasStation.init()
              Tank.init()
              Boiler.init()
          })
      })
    }
  }

  def getFilter(param: QueryOilUserParam) = {
    import org.mongodb.scala.model.Filters._
    val nameFilter = param.name map { name => regex("name", "(?i)" + name) }
    val useTypeFilter = param.useType flatMap { useTypeList =>
      val eqList = useTypeList.map { equal("useType", _) }
      if (eqList.isEmpty)
        None
      else
        Some(or(eqList: _*))
    }

    val addrFilter = param.addr map { addr => regex("addr", "(?i)" + addr) }
    val countyFilter = param.county map { county => regex("county", "(?i)" + county) }
    val salesFilter = param.sales map { sales => regex("sales", "(?i)" + sales) }
    val contracedFilter = param.contracted map { contracted => equal("contracted", contracted) }

    val filterList = List(nameFilter, useTypeFilter, addrFilter,
      countyFilter, salesFilter, contracedFilter).flatMap { f => f }

    val filter = if (!filterList.isEmpty)
      and(filterList: _*)
    else
      Filters.exists("_id")

    filter
  }

  import org.mongodb.scala.model._

  def query(param: QueryOilUserParam)(skip: Int, limit: Int) = {
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

  def queryCount(param: QueryOilUserParam) = {
    val filter = getFilter(param)

    val f = collection.count(filter).toFuture()
    f.onFailure {
      errorHandler
    }
    for (count <- f) yield count
  }

  def upsert(_id: String, oilUser: OilUser) = {
    val f = collection.replaceOne(Filters.eq("_id", _id), oilUser, UpdateOptions().upsert(true)).toFuture()
    f.onFailure {
      errorHandler
    }
    f
  }
}