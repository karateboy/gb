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
import scala.concurrent.Future
import play.api.libs.json._
import com.github.nscala_time.time.Imports._
import Highchart._
import models._
import models.ModelHelper._
import collection.JavaConversions._
import java.nio.file.Files

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
  def getGroupInfoList = Action {
    val infoList = Group.getInfoList
    implicit val write = Json.writes[GroupInfo]
    Ok(Json.toJson(infoList))
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
        ???
        //BuildCase2.importXLSX(upload.ref.file, true)
      }

      Ok(Json.obj("Ok" -> true))
  }

  def testParseMonthlyBuildCase() = Security.Authenticated {
    import java.io.File
    val path = current.path.getAbsolutePath + "/import/buildCase1.xlsx"
    BuildCase2.importMonthlyReport(path)(BuildCase2.monthlyReportParser)
    Ok("")
  }

  def testImportCheckBuildCase() = Action {
    import java.io.File
    val path = "D:\\checked\\"
    import org.apache.commons.io.FileUtils
    val files = FileUtils.iterateFiles(new File(path), Array("xlsx"), true)

    for (file <- files) {
      val filePath = file.getAbsolutePath
      val countyOpt = BuildCase2.countyList.find { x => filePath.contains(x) }

      if (countyOpt.isDefined) {
        BuildCase2.importCheckedBuildCase(countyOpt.get, file)
      } else {
        Logger.error(s"No county info=>${filePath}")
      }
    }

    Ok("")
  }

  def checkOutBuilder = Security.Authenticated.async {
    implicit request =>
      val userInfoOpt = Security.getUserinfo(request)
      val f = Builder.checkOut(userInfoOpt.get.id)
      val retF =
        for (builder <- f)
          yield Ok(Json.toJson(builder))

      retF.recover({
        case _: Throwable =>
          Logger.info("recover from no content...")
          NoContent
      })
  }

  def upsertBuilder = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      val ret = request.body.validate[Builder]
      ret.fold(
        error => {
          Logger.error(JsError.toJson(error).toString())
          Future { BadRequest(Json.obj("ok" -> false, "msg" -> JsError.toJson(error).toString())) }
        },
        builder => {
          val userInfoOpt = Security.getUserinfo(request)
          val userID = userInfoOpt.get.id
          val validCheckIn = if (builder.state == Builder.InvalidPhoneState)
            true
          else
            isVaildPhone(builder.phone)

          val f =
            if (Some(userID) == builder.editor)
              Builder.checkIn(userID, builder)
            else
              Builder.upsert(builder)

          for (result <- f) yield {
            val msg = if (!validCheckIn)
              "無效的電話號碼"
            else
              ""
            Ok(Json.obj("ok" -> validCheckIn, "msg" -> msg))
          }
        })
  }

  def checkOutBuildCase = Security.Authenticated.async {
    implicit request =>
      val userInfoOpt = Security.getUserinfo(request)
      val f = BuildCase2.checkOut(userInfoOpt.get.id)
      val retF =
        for (buildCase <- f) yield {
          Ok(Json.toJson(buildCase))
        }

      retF.recover({
        case _: Throwable =>
          Logger.info("recover from no content...")
          NoContent
      })
  }

  def checkOutContractor = Security.Authenticated.async {
    implicit request =>
      val userInfoOpt = Security.getUserinfo(request)
      val f = BuildCase2.checkOutContractor(userInfoOpt.get.id)
      val retF =
        for (buildCase <- f) yield {
          Ok(Json.toJson(buildCase))
        }

      retF.recover({
        case _: Throwable =>
          Logger.info("recover from no content...")
          NoContent
      })
  }

  def upsertBuildCase = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      val ret = request.body.validate[BuildCase2]
      ret.fold(
        error => {
          Logger.error(JsError.toJson(error).toString())
          Future { BadRequest(Json.obj("ok" -> false, "msg" -> JsError.toJson(error).toString())) }
        },
        buildCase => {
          val userInfoOpt = Security.getUserinfo(request)
          val userID = userInfoOpt.get.id

          val f =
            if (Some(userID) == buildCase.editor) {
              BuildCase2.checkIn(userID, buildCase)
            } else
              BuildCase2.upsert(buildCase)

          for (result <- f) yield {
            Ok(Json.obj("ok" -> true))
          }
        })
  }

  def upsertContractor = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      val ret = request.body.validate[Contractor]
      ret.fold(
        error => {
          Logger.error(JsError.toJson(error).toString())
          Future { BadRequest(Json.obj("ok" -> false, "msg" -> JsError.toJson(error).toString())) }
        },
        contractor => {
          val f = Contractor.upsert(contractor)

          for (result <- f) yield {
            Ok(Json.obj("ok" -> true))
          }
        })
  }

  def getContractor(_id: String) = Security.Authenticated.async {
    implicit request =>

      val f = Contractor.get(_id)

      for (contractorOpt <- f) yield {
        if (contractorOpt.isDefined) {
          val contractor = contractorOpt.get
          Ok(Json.toJson(contractor))
        } else
          NoContent
      }
  }

  def getUsageRecord(offset: Int) = Security.Authenticated.async {
    implicit request =>
      val userInfoOpt = Security.getUserinfo(request)
      val userID = userInfoOpt.get.id

      val f = UsageRecord.getRecord(userID, offset)
      for (records <- f) yield {
        if (records.isEmpty) {
          Ok(Json.toJson(UsageRecord.emptyRecord(userID, offset)))
        } else {
          Ok(Json.toJson(records.head))
        }
      }
  }

  def getDownloadType = Security.Authenticated {
    Ok(Json.toJson(DownloadType.getList))
  }

  def handleDownload(downloadTypeStr: String) = Security.Authenticated.async {
    val downloadType = DownloadType.withName(downloadTypeStr)

    def contractorDM = {
      import BuildCase2._
      val f = Contractor.getList

      for {
        constractorList <- f
      } yield {
        val dmList = constractorList flatMap { _.getDmOpt }
        val excel = ExcelUtility.exportDM(dmList)
        Ok.sendFile(excel, fileName = _ =>
          play.utils.UriEncoding.encodePathSegment("dm.xlsx", "UTF-8"),
          onClose = () => { Files.deleteIfExists(excel.toPath()) })
      }
    }
    downloadType match {
      case DownloadType.Contractor =>
        contractorDM
    }
  }

}
