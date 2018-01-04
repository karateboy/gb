package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import play.api.data._
import play.api.data.Forms._
import play.api.libs.ws._
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder
import scala.concurrent.Future
import play.api.libs.json._
import com.github.nscala_time.time.Imports._
import Highchart._
import models._
import models.ModelHelper._

object Application extends Controller {

  val title = "廢棄物清運系統"

  import models.User._

  def newUser = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      adminOnly({
        val newUserParam = request.body.validate[User]

        newUserParam.fold(
          error => {
            Logger.error(JsError.toJson(error).toString())
            Future { BadRequest(Json.obj("ok" -> false, "msg" -> JsError.toJson(error).toString())) }
          },
          param => {
            val f = User.newUser(param)
            val requestF =
              for (result <- f) yield {
                Ok(Json.obj("ok" -> true))
              }

            requestF.recover({
              case _: Throwable =>
                Logger.info("recover from newUser error...")
                Ok(Json.obj("ok" -> false))
            })
          })
      })
  }

  def deleteUser(email: String) = Security.Authenticated.async {
    implicit request =>
      adminOnly({
        val f = User.deleteUser(email)
        val requestF =
          for (result <- f) yield {
            Ok(Json.obj("ok" -> (result.getDeletedCount == 1)))
          }

        requestF.recover({
          case _: Throwable =>
            Logger.info("recover from deleteUser error...")
            Ok(Json.obj("ok" -> false))
        })
      })
  }

  def updateUser(id: String) = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      val userParam = request.body.validate[User]

      userParam.fold(
        error => {
          Future {
            Logger.error(JsError.toJson(error).toString())
            BadRequest(Json.obj("ok" -> false, "msg" -> JsError.toJson(error).toString()))
          }
        },
        param => {
          val f = User.updateUser(param)
          for (ret <- f) yield {
            Ok(Json.obj("ok" -> (ret.getMatchedCount == 1)))
          }
        })
  }

  def getAllUsers = Security.Authenticated.async {
    val userF = User.getAllUsersFuture()
    for (users <- userF) yield Ok(Json.toJson(users))
  }

  def adminOnly[A, B <: controllers.Security.UserInfo](permited: Future[Result])(implicit request: play.api.mvc.Security.AuthenticatedRequest[A, B]) = {
    val userInfoOpt = Security.getUserinfo(request)
    if (userInfoOpt.isEmpty)
      Future {
        Forbidden("No such user!")
      }
    else {
      val userInfo = userInfoOpt.get
      val userF = User.getUserByIdFuture(userInfo.id)
      val userOpt = waitReadyResult(userF)
      if (userOpt.isEmpty || userOpt.get.groupId != Group.Admin.toString())
        Future {
          Forbidden("無權限!")
        }
      else {
        permited
      }
    }
  }

  import scala.concurrent.ExecutionContext.Implicits.global
  def getGroupInfoList = Security.Authenticated {
    val infoList = Group.getInfoList
    implicit val write = Json.writes[GroupInfo]
    Ok(Json.toJson(infoList))
  }

  def testGeoCoding = Security.Authenticated {
    //val retList = GoogleApi.queryAddr("台北市萬華區西園路二段372巷23弄13號3樓")
    //for(ret <- retList){
    //  Logger.info(ret.toString())  
    //}
    CareHouse.convertAddrToLocation()
    Ok("ok")
  }

  def getBuildCaseTemplate = Security.Authenticated {
    import java.io.File
    val path = current.path.getAbsolutePath + "/report_template/buildCaseImport.xlsx"
    val excel = new File(path)
    Ok.sendFile(excel, fileName = _ =>
      play.utils.UriEncoding.encodePathSegment("起造人樣本.xlsx", "UTF-8"))

  }
  
  def uploadBuildCase() = Security.Authenticated(parse.multipartFormData) {
    implicit request =>
      request.body.files.map { upload =>
        val filename = upload.filename
        val contentType = upload.contentType
        Logger.info(s"upload $filename")
        BuildCase2.importXLSX(upload.ref.file, true)
      }

      Ok(Json.obj("Ok" -> true))
  }

}
