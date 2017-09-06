package controllers
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Json
import play.api.Play.current
import play.api.data._
import play.api.data.Forms._
import play.api.libs.ws._
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder
import scala.concurrent.Future
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.nscala_time.time.Imports._
import Highchart._
import models._

case class Stat(
    avg: Option[Double],
    min: Option[Double],
    max: Option[Double],
    count: Int,
    total: Int,
    overCount: Int) {
  val effectPercent = {
    if (total > 0)
      Some(count.toDouble * 100 / total)
    else
      None
  }

  val isEffective = {
    effectPercent.isDefined && effectPercent.get > 75
  }
  val overPercent = {
    if (count > 0)
      Some(overCount.toDouble * 100 / total)
    else
      None
  }
}

object Query extends Controller {
  def getPeriods(start: DateTime, endTime: DateTime, d: Period): List[DateTime] = {
    import scala.collection.mutable.ListBuffer

    val buf = ListBuffer[DateTime]()
    var current = start
    while (current < endTime) {
      buf.append(current)
      current += d
    }

    buf.toList
  }

  def getPeriodCount(start: DateTime, endTime: DateTime, p: Period) = {
    var count = 0
    var current = start
    while (current < endTime) {
      count += 1
      current += p
    }

    count
  }

  import java.nio.file.Files

  def queryCareHouse(skip: Int, limit: Int, outputTypeStr: String) = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      val outputType = OutputType.withName(outputTypeStr)
      implicit val paramRead = Json.reads[QueryCareHouseParam]
      implicit val careTypeWrite = Json.writes[CareType]
      implicit val careHouseWrite = Json.writes[CareHouse]
      val result = request.body.validate[QueryCareHouseParam]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        param => {
          val f = CareHouse.queryCareHouse(param)(skip, limit)
          for (careHouseList <- f) yield {
            outputType match {
              case OutputType.html =>
                Ok(Json.toJson(careHouseList))
              case OutputType.excel =>
                val excel = ExcelUtility.exportCareHouse(careHouseList)
                Ok.sendFile(excel, fileName = _ =>
                  play.utils.UriEncoding.encodePathSegment("安養機構.xlsx", "UTF-8"),
                  onClose = () => { Files.deleteIfExists(excel.toPath()) })
            }
          }
        })
  }

  def queryCareHouseList = queryCareHouse(0, 10000, "html")
  def queryCareHouseExcel = queryCareHouse(0, 10000, "excel")
  
  def queryCareHouseCount() = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      implicit val paramRead = Json.reads[QueryCareHouseParam]
      implicit val careTypeWrite = Json.writes[CareType]
      implicit val careHouseWrite = Json.writes[CareHouse]
      val result = request.body.validate[QueryCareHouseParam]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        param => {
          val f = CareHouse.queryCareHouseCount(param)
          f map {
            count =>
              Ok(Json.toJson(count))
          }
        })
  }
}