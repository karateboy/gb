package controllers
import play.api._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import play.api.data._
import play.api.data.Forms._
import play.api.libs.ws._
import scala.concurrent._
import com.github.nscala_time.time.Imports._
import Highchart._
import models._
import models.ModelHelper._
import collection.JavaConversions._
import java.nio.file.Files

object SalesManager extends Controller {
  def getMyCase(queryParamJson: String, skip: Int, limit: Int) = Security.Authenticated.async {
    implicit request =>
      val userInfoOpt = Security.getUserinfo(request)
      val f = BuildCase2.getOwnerBuildCase(userInfoOpt.get.id)()(skip, limit)
      for (builder <- f)
        yield Ok(Json.toJson(builder))
  }

  def getMyCaseCount(queryParamJson: String) = Security.Authenticated.async {
    implicit request =>
      val userInfoOpt = Security.getUserinfo(request)
      val f = BuildCase2.getOwnerBuildCaseCount(userInfoOpt.get.id)
      for (count <- f)
        yield Ok(Json.toJson(count))
  }

  def getMyCaseExcel(queryParamJson: String) = Security.Authenticated.async {
    implicit request =>
      val userInfoOpt = Security.getUserinfo(request)
      val f = BuildCase2.getOwnerBuildCase(userInfoOpt.get.id)(0, 10000)
      val builderMapF = Builder.getMap
      for {
        buildCaseList <- f
        builderMap <- builderMapF
      } yield {
        val excel = ExcelUtility.exportBuildCase(buildCaseList, builderMap)
        Ok.sendFile(excel, fileName = _ =>
          play.utils.UriEncoding.encodePathSegment("起造人.xlsx", "UTF-8"),
          onClose = () => { Files.deleteIfExists(excel.toPath()) })
      }
  }

  def getOwnerless(dir: String, queryParamJson: String, skip: Int, limit: Int) = Security.Authenticated.async {
    implicit request =>
      val json = java.net.URLDecoder.decode(queryParamJson, "UTF-8")
      import BuildCase2._
      val queryParam = Json.parse(json).validate[QueryParam].asOpt.getOrElse(defaultQueryParam)

      val f = if (dir.equalsIgnoreCase("N"))
        BuildCase2.getNorthOwnerless(queryParam)(skip, limit)
      else
        BuildCase2.getSouthOwnerless(queryParam)(skip, limit)

      for (builder <- f) yield {
        Ok(Json.toJson(builder))
      }
  }

  def getOwnerlessCount(dir: String, queryParamJson: String) = Security.Authenticated.async {
    implicit request =>
      val json = java.net.URLDecoder.decode(queryParamJson, "UTF-8")
      import BuildCase2._
      val queryParam = Json.parse(json).validate[QueryParam].asOpt.getOrElse(defaultQueryParam)

      val f = if (dir.equalsIgnoreCase("N"))
        BuildCase2.getNorthOwnerlessCount(queryParam)
      else
        BuildCase2.getSouthOwnerlessCount(queryParam)

      for (count <- f)
        yield Ok(Json.toJson(count))
  }

  def getOwnerlessExcel(dir: String, queryParamJson: String) = Security.Authenticated.async {
    implicit request =>
      val json = java.net.URLDecoder.decode(queryParamJson, "UTF-8")
      import BuildCase2._
      val queryParam = Json.parse(json).validate[QueryParam].asOpt.getOrElse(defaultQueryParam)

      val f = if (dir.equalsIgnoreCase("N"))
        BuildCase2.getNorthOwnerless(queryParam)(0, 100000)
      else
        BuildCase2.getSouthOwnerless(queryParam)(0, 100000)

      val builderMapF = Builder.getMap
      for {
        buildCaseList <- f
        builderMap <- builderMapF
      } yield {
        val excel = ExcelUtility.exportBuildCase(buildCaseList, builderMap)
        Ok.sendFile(excel, fileName = _ =>
          play.utils.UriEncoding.encodePathSegment("起造人.xlsx", "UTF-8"),
          onClose = () => { Files.deleteIfExists(excel.toPath()) })
      }
  }

  def obtainCase() = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      import BuildCase2._
      val ret = request.body.validate[BuildCaseID]
      ret.fold(
        error => {
          Logger.error(JsError.toJson(error).toString())
          Future { BadRequest(Json.obj("ok" -> false, "msg" -> JsError.toJson(error).toString())) }
        },
        _id => {
          val me = Security.getUserinfo(request).get.id
          val retF = BuildCase2.obtain(_id, me)
          for (ret <- retF) yield {
            if (ret.getModifiedCount == 0)
              Ok(Json.obj("ok" -> false, "msg" -> "獲取失敗, 該案已有負責人"))
            else
              Ok(Json.obj("ok" -> true))
          }
        })
  }

  def releaseCase() = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      import BuildCase2._
      val ret = request.body.validate[BuildCaseID]
      ret.fold(
        error => {
          Logger.error(JsError.toJson(error).toString())
          Future { BadRequest(Json.obj("ok" -> false, "msg" -> JsError.toJson(error).toString())) }
        },
        _id => {
          val me = Security.getUserinfo(request).get.id
          val retF = BuildCase2.release(_id, me)
          for (ret <- retF) yield {
            if (ret.getModifiedCount == 0)
              Ok(Json.obj("ok" -> false, "msg" -> "釋放失敗, 不是自己的案"))
            else
              Ok(Json.obj("ok" -> true))
          }
        })
  }

  def getBuildCase() = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      import BuildCase2._
      val ret = request.body.validate[BuildCaseID]
      ret.fold(
        error => {
          Logger.error(JsError.toJson(error).toString())
          Future { BadRequest(Json.obj("ok" -> false, "msg" -> JsError.toJson(error).toString())) }
        },
        _id => {
          val bcF = BuildCase2.getBuildCase(_id)
          for (bc <- bcF) yield {
            Ok(Json.toJson(bc))
          }
        })
  }

  def getWorkPoint() = Security.Authenticated.async {
    val f = WorkPoint.getList()

    for (workPointList <- f) yield {
      Ok(Json.toJson(workPointList))
    }
  }

  def getTop3DumpSite(lon: Double, lat: Double) = Security.Authenticated.async {
    import DumpSite._
    val f = DumpSite.top3Near(Seq(lon, lat))
    for (dumpSites <- f) yield {
      Ok(Json.toJson(dumpSites))
    }

  }
}