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

  def updateCareHouse = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      implicit val careTypeReads = Json.reads[CareType]

      implicit val paramRead = Json.reads[CareHouse]
      val result = request.body.validate[CareHouse]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        careHouse => {
          val f = CareHouse.upsertCareHouse(careHouse._id, careHouse)
          //f.onSuccess(Ok(Json.obj("ok"->true)))
          for (ret <- f) yield Ok(Json.obj("Ok" -> true))
        })
  }

  import BuildCase2._
  def queryBuildCase(skip: Int, limit: Int, outputTypeStr: String) = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      val outputType = OutputType.withName(outputTypeStr)

      val result = request.body.validate[QueryBuildCaseParam2]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        param => {
          val f = BuildCase2.queryBuildCase(param)(skip, limit)
          for (buildCaseList <- f) yield {
            outputType match {
              case OutputType.html =>
                Ok(Json.toJson(buildCaseList))
              case OutputType.excel =>
                val excel = ExcelUtility.exportBuildCase(buildCaseList)
                Ok.sendFile(excel, fileName = _ =>
                  play.utils.UriEncoding.encodePathSegment("起造人.xlsx", "UTF-8"),
                  onClose = () => { Files.deleteIfExists(excel.toPath()) })
            }
          }
        })
  }

  import org.mongodb.scala.bson.ObjectId
  def getBuildCase(encodedJson: String) = Security.Authenticated.async({
    val json = java.net.URLDecoder.decode(encodedJson, "UTF-8")
    val ret = Json.parse(json).validate[QueryBuildCaseParam2]
    ret.fold(
      err =>
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        },
      param => {
        val f = BuildCase2.queryBuildCase(param)(0, 2048)
        for (buildCaseList <- f) yield {
          val excel = ExcelUtility.exportBuildCase(buildCaseList)
          Ok.sendFile(excel, fileName = _ =>
            play.utils.UriEncoding.encodePathSegment("起造人.xlsx", "UTF-8"),
            onClose = () => { Files.deleteIfExists(excel.toPath()) })
        }
      })

  })

  def queryBuildCaseList = queryBuildCase(0, 10000, "html")
  def queryBuildCaseExcel = queryBuildCase(0, 10000, "excel")

  def queryBuildCaseCount() = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      val result = request.body.validate[QueryBuildCaseParam2]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        param => {
          val f = BuildCase2.queryBuildCaseCount(param)
          f map {
            count =>
              Ok(Json.toJson(count))
          }
        })
  }
  def updateBuildCase = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      val result = request.body.validate[BuildCase2]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        buildCase => {
          val f = BuildCase2.upsertBuildCase(buildCase)
          //f.onSuccess(Ok(Json.obj("ok"->true)))
          for (ret <- f) yield Ok(Json.obj("Ok" -> true))
        })
  }

  def getBuilder(encodedID: String) = Security.Authenticated.async {
    val _id = java.net.URLDecoder.decode(encodedID, "UTF-8")
    val f = Builder.get(_id)
    for (builderOpt <- f) yield {
      if (builderOpt.isEmpty)
        NoContent
      else {
        val builder = builderOpt.get
        Ok(Json.toJson(builder))
      }
    }
  }

  //=====================================================================================
  def queryOilUser(skip: Int, limit: Int, outputTypeStr: String) = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      val outputType = OutputType.withName(outputTypeStr)
      implicit val paramRead = Json.reads[QueryOilUserParam]
      implicit val oilUserWrite = Json.writes[OilUser]
      val result = request.body.validate[QueryOilUserParam]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        param => {
          val f = OilUser.query(param)(skip, limit)
          for (oilUserList <- f) yield {
            outputType match {
              case OutputType.html =>
                Ok(Json.toJson(oilUserList))
              /*case OutputType.excel =>
                val excel = ExcelUtility.exportBuildCase(buildCaseList)
                Ok.sendFile(excel, fileName = _ =>
                  play.utils.UriEncoding.encodePathSegment("起造人.xlsx", "UTF-8"),
                  onClose = () => { Files.deleteIfExists(excel.toPath()) })
                  * /
                  */
            }
          }
        })
  }

  def getOilUser(encodedJson: String) = Security.Authenticated.async({
    val json = java.net.URLDecoder.decode(encodedJson, "UTF-8")
    implicit val paramRead = Json.reads[QueryOilUserParam]
    implicit val buildCaseWrite = Json.writes[OilUser]

    val ret = Json.parse(json).validate[QueryOilUserParam]
    ret.fold(
      err =>
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        },
      param => {
        val f = OilUser.query(param)(0, 2048)
        for (oilUserList <- f) yield {
          /*
          val excel = ExcelUtility.exportBuildCase(buildCaseList)
          Ok.sendFile(excel, fileName = _ =>
            play.utils.UriEncoding.encodePathSegment("起造人.xlsx", "UTF-8"),
            onClose = () => { Files.deleteIfExists(excel.toPath()) })*/
          Ok("")
        }
      })

  })

  def queryOilUserList = queryOilUser(0, 1000, "html")
  def queryOilUserExcel = queryOilUser(0, 1000, "excel")

  def queryOilUserCount() = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      implicit val paramRead = Json.reads[QueryOilUserParam]
      val result = request.body.validate[QueryOilUserParam]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        param => {
          val f = OilUser.queryCount(param)
          f map {
            count =>
              Ok(Json.toJson(count))
          }
        })
  }
  def updateOilUser = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>

      implicit val paramRead = Json.reads[OilUser]
      val result = request.body.validate[OilUser]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        oilUser => {
          val f = OilUser.upsert(oilUser._id, oilUser)
          //f.onSuccess(Ok(Json.obj("ok"->true)))
          for (ret <- f) yield Ok(Json.obj("Ok" -> true))
        })
  }

}