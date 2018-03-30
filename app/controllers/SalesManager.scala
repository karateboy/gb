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
import play.utils.UriEncoding
object SalesManager extends Controller {
  import org.mongodb.scala.model._

  def getMyCase(typeID: String, queryParamJson: String, skip: Int, limit: Int) = Security.Authenticated.async {
    implicit request =>
      val wpType = WorkPointType.withName(typeID)

      wpType match {
        case WorkPointType.BuildCase =>
          val paramOpt = Json.parse(queryParamJson).validate[BuildCase2.QueryParam].asOpt
          val queryParam = paramOpt.getOrElse(BuildCase2.defaultQueryParam)
          queryParam.owner = Some(Security.getUserID(request))
          val f = BuildCase2.query(BuildCase2.getFilter(queryParam))(BuildCase2.getSortBy(queryParam))(skip, limit)
          for (wpList <- f)
            yield Ok(Json.toJson(wpList))

        case WorkPointType.CareHouse =>
          {
            import CareHouse._
            val paramOpt = Json.parse(queryParamJson).validate[QueryParam].asOpt
            val queryParam = paramOpt.getOrElse(QueryParam())
            queryParam.owner = Some(Security.getUserID(request))

            val f = CareHouse.query(getFilter(queryParam))(getSortBy(queryParam))(skip, limit)
            for (wpList <- f)
              yield Ok(Json.toJson(wpList))
          }
      }

  }

  def getMyCaseCount(typeID: String, queryParamJson: String) = Security.Authenticated.async {
    implicit request =>
      val wpType = WorkPointType.withName(typeID)

      wpType match {
        case WorkPointType.BuildCase => {
          import BuildCase2._
          val paramOpt = Json.parse(queryParamJson).validate[QueryParam].asOpt
          val queryParam = paramOpt.getOrElse(QueryParam())
          queryParam.owner = Some(Security.getUserID(request))
          val f = count(getFilter(queryParam))
          for (count <- f)
            yield Ok(Json.toJson(count))
        }

        case WorkPointType.CareHouse => {
          import CareHouse._
          val paramOpt = Json.parse(queryParamJson).validate[QueryParam].asOpt
          val queryParam = paramOpt.getOrElse(QueryParam())
          queryParam.owner = Some(Security.getUserID(request))
          val f = count(getFilter(queryParam))
          for (count <- f)
            yield Ok(Json.toJson(count))
        }
      }

  }

  def getMyCaseExcel(typeID: String, queryParamJson: String) = Security.Authenticated.async {
    implicit request =>
      val wpType = WorkPointType.withName(typeID)

      def buildCase = {
        import BuildCase2._
        val paramOpt = Json.parse(queryParamJson).validate[QueryParam].asOpt
        val queryParam = paramOpt.getOrElse(defaultQueryParam)
        queryParam.owner = Some(Security.getUserID(request))

        val f = query(getFilter(queryParam))(getSortBy(queryParam))(0, 10000)
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

      wpType match {
        case WorkPointType.BuildCase =>
          buildCase
      }

  }

  def getOwnerless(dir: String, typeID: String, queryParamJson: String, skip: Int, limit: Int) = Security.Authenticated.async {
    implicit request =>
      val wpType = WorkPointType.withName(typeID)

      def buildCase = {
        import BuildCase2._
        val queryParam = Json.parse(queryParamJson).validate[QueryParam].asOpt.getOrElse(defaultQueryParam)
        val f = if (dir.equalsIgnoreCase("N"))
          getNorthOwnerless(queryParam)(skip, limit)
        else
          getSouthOwnerless(queryParam)(skip, limit)

        for (objList <- f) yield {
          Ok(Json.toJson(objList))
        }
      }

      def careHouse = {
        import CareHouse._
        val queryParam = Json.parse(queryParamJson).validate[QueryParam].asOpt.getOrElse(QueryParam())
        val f = if (dir.equalsIgnoreCase("N"))
          getNorthOwnerless(queryParam)(skip, limit)
        else
          getSouthOwnerless(queryParam)(skip, limit)

        for (objList <- f) yield {
          Ok(Json.toJson(objList))
        }
      }

      wpType match {
        case WorkPointType.BuildCase =>
          buildCase
        case WorkPointType.CareHouse =>
          careHouse
      }
  }

  def getOwnerlessCount(dir: String, typeID: String, queryParamJson: String) = Security.Authenticated.async {
    implicit request =>

      val wpType = WorkPointType.withName(typeID)

      def buildCase = {
        import BuildCase2._
        val queryParam = Json.parse(queryParamJson).validate[QueryParam].asOpt.getOrElse(QueryParam())

        val f = if (dir.equalsIgnoreCase("N"))
          getNorthOwnerlessCount(queryParam)
        else
          getSouthOwnerlessCount(queryParam)

        for (count <- f)
          yield Ok(Json.toJson(count))
      }

      def careHouse = {
        import CareHouse._
        val queryParam = Json.parse(queryParamJson).validate[QueryParam].asOpt.getOrElse(QueryParam())

        val f = if (dir.equalsIgnoreCase("N"))
          getNorthOwnerlessCount(queryParam)
        else
          getSouthOwnerlessCount(queryParam)

        for (count <- f)
          yield Ok(Json.toJson(count))
      }

      wpType match {
        case WorkPointType.BuildCase =>
          buildCase
        case WorkPointType.CareHouse =>
          careHouse
      }
  }

  def getOwnerlessExcel(dir: String, typeID: String, queryParamJson: String) = Security.Authenticated.async {
    implicit request =>

      val wpType = WorkPointType.withName(typeID)

      def buildCase = {
        import BuildCase2._
        val queryParam = Json.parse(queryParamJson).validate[QueryParam].asOpt.getOrElse(QueryParam())
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
            play.utils.UriEncoding.encodePathSegment("builder.xlsx", "UTF-8"),
            onClose = () => { Files.deleteIfExists(excel.toPath()) })
        }
      }

      def careHouse = {
        import CareHouse._
        val queryParam = Json.parse(queryParamJson).validate[QueryParam].asOpt.getOrElse(QueryParam())
        val f = if (dir.equalsIgnoreCase("N"))
          CareHouse.getNorthOwnerless(queryParam)(0, 100000)
        else
          CareHouse.getSouthOwnerless(queryParam)(0, 100000)

        for {
          careHouseList <- f
        } yield {
          val excel = ExcelUtility.exportCareHouse(careHouseList)
          Ok.sendFile(excel, fileName = _ =>
            play.utils.UriEncoding.encodePathSegment("careHouse.xlsx", "UTF-8"),
            onClose = () => { Files.deleteIfExists(excel.toPath()) })
        }
      }
      wpType match {
        case WorkPointType.BuildCase =>
          buildCase
        case WorkPointType.CareHouse =>
          careHouse
      }
  }

  def getOwnerlessDM(dir: String, typeID: String) = Security.Authenticated.async {
    implicit request =>

      val wpType = WorkPointType.withName(typeID)

      def buildCase = {
        import BuildCase2._
        val f = if (dir.equalsIgnoreCase("N"))
          BuildCase2.getNorthDM
        else
          BuildCase2.getSouthDM

        val builderMapF = Builder.getMap
        for {
          buildCaseList <- f
          builderMap <- builderMapF
        } yield {
          val dmList = buildCaseList flatMap { _.getDM(builderMap) }
          val excel = ExcelUtility.exportDM(dmList)
          Ok.sendFile(excel, fileName = _ =>
            play.utils.UriEncoding.encodePathSegment("dm.xlsx", "UTF-8"),
            onClose = () => { Files.deleteIfExists(excel.toPath()) })
        }
      }

      wpType match {
        case WorkPointType.BuildCase =>
          buildCase

      }
  }

  def getOwnerlessSplit(dir: String, typeID: String) = Security.Authenticated.async {
    implicit request =>
      val wpType = WorkPointType.withName(typeID)

      wpType match {
        case WorkPointType.BuildCase =>
          val f = if (dir.equalsIgnoreCase("N"))
            BuildCase2.splitNorthOwnerless
          else
            BuildCase2.splitSouthOwnerless

          for (ret <- f) yield Ok(Json.obj("updated" -> ret.getModifiedCount))
        case WorkPointType.CareHouse =>
          val f = if (dir.equalsIgnoreCase("N"))
            CareHouse.splitNorthOwnerless
          else
            CareHouse.splitSouthOwnerless

          for (ret <- f) yield Ok(Json.obj("updated" -> ret.getModifiedCount))

      }
  }

  def getAllCase(dir: String, typeID: String, queryParamJson: String, skip: Int, limit: Int) = Security.Authenticated.async {
    implicit request =>
      val wpType = WorkPointType.withName(typeID)

      def buildCase = {
        import BuildCase2._
        val queryParam = Json.parse(queryParamJson).validate[QueryParam].asOpt.getOrElse(defaultQueryParam)
        val f = if (dir.equalsIgnoreCase("N"))
          getNorthAll(queryParam)(skip, limit)
        else
          getSouthAll(queryParam)(skip, limit)

        for (objList <- f) yield {
          Ok(Json.toJson(objList))
        }
      }

      def careHouse = {
        import CareHouse._
        val queryParam = Json.parse(queryParamJson).validate[QueryParam].asOpt.getOrElse(QueryParam())
        val f = if (dir.equalsIgnoreCase("N"))
          getNorthAll(queryParam)(skip, limit)
        else
          getSouthAll(queryParam)(skip, limit)

        for (objList <- f) yield {
          Ok(Json.toJson(objList))
        }
      }

      wpType match {
        case WorkPointType.BuildCase =>
          buildCase
        case WorkPointType.CareHouse =>
          careHouse
      }
  }

  def getAllCaseCount(dir: String, typeID: String, queryParamJson: String) = Security.Authenticated.async {
    implicit request =>

      val wpType = WorkPointType.withName(typeID)

      def buildCase = {
        import BuildCase2._
        val queryParam = Json.parse(queryParamJson).validate[QueryParam].asOpt.getOrElse(QueryParam())

        val f = if (dir.equalsIgnoreCase("N"))
          getNorthAllCount(queryParam)
        else
          getSouthAllCount(queryParam)

        for (count <- f)
          yield Ok(Json.toJson(count))
      }

      def careHouse = {
        import CareHouse._
        val queryParam = Json.parse(queryParamJson).validate[QueryParam].asOpt.getOrElse(QueryParam())

        val f = if (dir.equalsIgnoreCase("N"))
          getNorthAllCount(queryParam)
        else
          getSouthAllCount(queryParam)

        for (count <- f)
          yield Ok(Json.toJson(count))
      }

      wpType match {
        case WorkPointType.BuildCase =>
          buildCase
        case WorkPointType.CareHouse =>
          careHouse
      }
  }

  def getAllCaseExcel(dir: String, typeID: String, queryParamJson: String) = Security.Authenticated.async {
    implicit request =>

      val wpType = WorkPointType.withName(typeID)

      def buildCase = {
        import BuildCase2._
        val queryParam = Json.parse(queryParamJson).validate[QueryParam].asOpt.getOrElse(QueryParam())
        val f = if (dir.equalsIgnoreCase("N"))
          BuildCase2.getNorthAll(queryParam)(0, 100000)
        else
          BuildCase2.getSouthAll(queryParam)(0, 100000)

        val builderMapF = Builder.getMap
        for {
          buildCaseList <- f
          builderMap <- builderMapF
        } yield {
          val excel = ExcelUtility.exportBuildCase(buildCaseList, builderMap)
          Ok.sendFile(excel, fileName = _ =>
            play.utils.UriEncoding.encodePathSegment("builder.xlsx", "UTF-8"),
            onClose = () => { Files.deleteIfExists(excel.toPath()) })
        }
      }

      def careHouse = {
        import CareHouse._
        val queryParam = Json.parse(queryParamJson).validate[QueryParam].asOpt.getOrElse(QueryParam())
        val f = if (dir.equalsIgnoreCase("N"))
          CareHouse.getNorthAll(queryParam)(0, 100000)
        else
          CareHouse.getSouthAll(queryParam)(0, 100000)

        for {
          careHouseList <- f
        } yield {
          val excel = ExcelUtility.exportCareHouse(careHouseList)
          Ok.sendFile(excel, fileName = _ =>
            play.utils.UriEncoding.encodePathSegment("careHouse.xlsx", "UTF-8"),
            onClose = () => { Files.deleteIfExists(excel.toPath()) })
        }
      }
      wpType match {
        case WorkPointType.BuildCase =>
          buildCase
        case WorkPointType.CareHouse =>
          careHouse
      }
  }

  def obtainCase() = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      import BuildCase2.idRead
      import CareHouse.chIdRead
      val bcIdOpt = request.body.validate[BuildCaseID].asOpt
      val chIdOpt = request.body.validate[CareHouseID].asOpt

      (bcIdOpt, chIdOpt) match {
        case (Some(_id), _) =>
          val retF = BuildCase2.obtain(_id, Security.getUserID(request))
          for (ret <- retF) yield {
            if (ret.getModifiedCount == 0)
              Ok(Json.obj("ok" -> false, "msg" -> "獲取失敗, 該案已有負責人"))
            else
              Ok(Json.obj("ok" -> true))
          }

        case (None, Some(_id)) =>
          val retF = CareHouse.obtain(_id, Security.getUserID(request))
          for (ret <- retF) yield {
            if (ret.getModifiedCount == 0)
              Ok(Json.obj("ok" -> false, "msg" -> "獲取失敗, 該案已有負責人"))
            else
              Ok(Json.obj("ok" -> true))
          }
        case _ =>
          Future { BadRequest(Json.obj("ok" -> false, "msg" -> "無法辨識ID類別")) }
      }
  }

  def releaseCase() = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      import BuildCase2.idRead
      import CareHouse.chIdRead
      val bcIdOpt = request.body.validate[BuildCaseID].asOpt
      val chIdOpt = request.body.validate[CareHouseID].asOpt

      (bcIdOpt, chIdOpt) match {
        case (Some(_id), _) =>
          val retF = BuildCase2.release(_id, Security.getUserID(request))
          for (ret <- retF) yield {
            if (ret.getModifiedCount == 0)
              Ok(Json.obj("ok" -> false, "msg" -> "釋放失敗, 不是自己的案"))
            else
              Ok(Json.obj("ok" -> true))
          }

        case (None, Some(_id)) =>
          val retF = CareHouse.release(_id, Security.getUserID(request))
          for (ret <- retF) yield {
            if (ret.getModifiedCount == 0)
              Ok(Json.obj("ok" -> false, "msg" -> "釋放失敗, 不是自己的案"))
            else
              Ok(Json.obj("ok" -> true))
          }

        case _ =>
          Future { BadRequest(Json.obj("ok" -> false, "msg" -> "無法辨識ID類別")) }
      }
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

  def getWorkPointTypeList = Security.Authenticated {
    val list = WorkPointType.getList
    Ok(Json.toJson(list))
  }

  def getTargetWorkPointTypes = Security.Authenticated {
    val list = WorkPointType.getWorkList
    Ok(Json.toJson(list))
  }

  def getAreaWorkPoint(typeListStr: String, bottomLeftJson: String, upperRightJson: String) = Security.Authenticated.async {
    import WorkPoint._
    val typeList = typeListStr.split(",").toList
    val typeIdList = typeList map { WorkPointType.withName }
    val idList = typeIdList map { _.id }
    val bottomLeft = Json.parse(bottomLeftJson).validate[LatLng].asOpt.get
    val upperRight = Json.parse(upperRightJson).validate[LatLng].asOpt.get

    val f = WorkPoint.getAreaList(idList, bottomLeft, upperRight)
    for (wpList <- f) yield {
      Ok(Json.toJson(wpList))
    }

  }
}