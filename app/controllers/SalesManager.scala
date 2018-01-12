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

object SalesManager extends Controller {
  def getMyCase(skip: Int, limit: Int) = Security.Authenticated.async {
    implicit request =>
      val userInfoOpt = Security.getUserinfo(request)
      val f = BuildCase2.getOwnerBuildCase(userInfoOpt.get.id)(skip, limit)
      for (builder <- f)
        yield Ok(Json.toJson(builder))
  }

  def getMyCaseCount() = Security.Authenticated.async {
    implicit request =>
      val userInfoOpt = Security.getUserinfo(request)
      val f = BuildCase2.getOwnerBuildCaseCount(userInfoOpt.get.id)
      for (count <- f)
        yield Ok(Json.toJson(count))
  }

  def getNorthOwnerless(skip: Int, limit: Int) = Security.Authenticated.async {
    implicit request =>
      val f = BuildCase2.getNorthOwnerless()(skip, limit)
      for (builder <- f) yield {
        Ok(Json.toJson(builder))
      }
  }

  def getNorthOwnerlessCount() = Security.Authenticated.async {
    implicit request =>
      val f = BuildCase2.getNorthOwnerlessCount()
      for (count <- f)
        yield Ok(Json.toJson(count))
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
}