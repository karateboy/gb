package models
import play.api._
import play.api.Play.current
import play.api.libs.json._
import models.ModelHelper._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import org.apache.poi.openxml4j.opc._
import org.apache.poi.xssf.usermodel._
import com.github.nscala_time.time.Imports._

import java.io._
import java.nio.file.Files
import java.nio.file._
import java.util.Date

import org.mongodb.scala.model._
import org.mongodb.scala.model.Indexes._
import org.mongodb.scala.bson._
import MongoDB._

/*
object BuildCaseState extends Enumeration {
  val Initial = Value
  val GetPhone = Value
  val Visited = Value
  val Monitoring = Value
  val Contracted = Value
  val Closed = Value
}
*
*/

import java.util.Date
case class ContactInfo(name: Option[String] = None, addr: Option[String] = None, phone: Option[String] = None)
case class BuildCaseForm(
  constructor: ContactInfo    = ContactInfo(),
  permit:      ContactInfo    = ContactInfo(),
  earthWork:   ContactInfo    = ContactInfo(),
  dump:        ContactInfo    = ContactInfo(),
  burner:      ContactInfo    = ContactInfo(),
  wall:        ContactInfo    = ContactInfo(),
  photos:      Seq[ObjectId]  = Seq.fill(4)(new ObjectId(Photo.noPhotoID)),
  comment:     Option[String] = Some(""),
  submitDate:  Date           = new Date())

case class BuildCaseID(county: String, permitID: String, wpType: Int = WorkPointType.BuildCase.id) extends IWorkPointID
case class SiteInfo(usage: String, floorDesc: String, addr: String, area: Option[Double], landlordAddr: Option[String])
case class BuildCase2(_id: BuildCaseID, builder: String, personal: Boolean,
                      siteInfo:   SiteInfo,
                      permitDate: Date, architect: String,
                      var location: Option[Seq[Double]] = None,
                      contractor:   Option[String]      = None, contractorCheckDate: Option[Date] = None,
                      state: Option[String] = Some(CaseState.Unknown.toString()), owner: Option[String] = None,
                      tag:   Seq[String] = Seq.empty[String],
                      notes: Seq[Note]   = Seq.empty[Note], var editor: Option[String] = None,
                      dm: Boolean = false, form: Option[BuildCaseForm] = None) extends IWorkPoint {
  def getSummary = {
    val content = s"${siteInfo.addr}<br>" +
      s"${siteInfo.usage}<br>" +
      s"${siteInfo.floorDesc}<br>" +
      s"${siteInfo.area.getOrElse(0)}平方公尺"
    Summary(builder, content)
  }

  def getDM(builderMap: Map[String, Builder]) = {
    if (personal)
      None
    else {
      val builderInfo = builderMap(builder)
      val dm = DM(Some(builder), Some(builderInfo.contact), builderInfo.addr)
      Some(dm)
    }
  }
}

object BuildCase2 {
  case class QueryParam(
    areaGT: Option[Double] = None, areaLT: Option[Double] = None,
    tag:         Option[Seq[String]] = None,
    var state:   Option[String]      = None,
    var owner:   Option[String]      = None,
    keyword:     Option[String]      = None,
    var hasForm: Option[Boolean]     = None,
    sortBy:      String              = "siteInfo.area+")

  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }
  import WorkPoint._

  val codecRegistry = fromRegistries(fromProviders(classOf[BuildCase2], classOf[Note], classOf[SiteInfo],
    classOf[BuildCaseID], classOf[BuildCaseForm], classOf[ContactInfo]), DEFAULT_CODEC_REGISTRY)

  val ColName = WorkPoint.ColName
  val collection = MongoDB.database.getCollection[BuildCase2](WorkPoint.ColName).withCodecRegistry(codecRegistry)

  import ObjectIdUtil._
  implicit val ciWrite = Json.writes[ContactInfo]
  implicit val formWrite = Json.writes[BuildCaseForm]
  implicit val siWrite = Json.writes[SiteInfo]
  implicit val idWrite = Json.writes[BuildCaseID]
  implicit val bcWrite = Json.writes[BuildCase2]

  implicit val ciRead = Json.reads[ContactInfo]
  implicit val formRead = Json.reads[BuildCaseForm]
  implicit val siRead = Json.reads[SiteInfo]
  implicit val idRead = Json.reads[BuildCaseID]
  implicit val bcRead = Json.reads[BuildCase2]

  val defaultQueryParam = QueryParam()
  implicit val qbcRead = Json.reads[QueryParam]
  implicit val qbcWrite = Json.writes[QueryParam]

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val cf1 = collection.createIndex(ascending("_id.county")).toFuture()
      val cf2 = collection.createIndex(ascending("builder")).toFuture()
      val cf3 = collection.createIndex(ascending("architect")).toFuture()
      val cf4 = collection.createIndex(ascending("_id.county", "wpType")).toFuture()
      val cf5 = collection.createIndex(ascending("state")).toFuture()

      cf1.onFailure(errorHandler)
      cf2.onFailure(errorHandler)
      cf3.onFailure(errorHandler)
      cf4.onFailure(errorHandler)
      cf5.onFailure(errorHandler)

      import scala.concurrent._
      val endF = Future.sequence(Seq(cf1, cf2, cf3, cf4, cf5))
      endF.onComplete({
        case x =>
          val path = current.path.getAbsolutePath + "/import/buildCase.xlsx"
          importMonthlyReport(path)(monthlyReportParser)
      })
    }
  }

  def importMonthlyReport(path: String)(parser: (XSSFWorkbook) => Unit): Boolean = {
    val file = new File(path)
    importMonthlyReport(file)(parser)
  }

  def importMonthlyReport(file: File, delete: Boolean = false)(parser: (XSSFWorkbook) => Unit): Boolean = {
    Logger.info(s"Start import ${file.getAbsolutePath}...")
    //Open Excel
    try {
      val fs = new FileInputStream(file)
      val pkg = OPCPackage.open(fs)
      val wb = new XSSFWorkbook(pkg);
      parser(wb)

      fs.close()
      if (delete)
        file.delete()
      Logger.info(s"Success import ${file.getAbsolutePath}")
    } catch {
      case ex: FileNotFoundException =>
        Logger.warn(s"Cannot open ${file.getAbsolutePath}")
        false
      case ex: Throwable =>
        Logger.error(s"Fail to import ${file.getAbsolutePath}", ex)
        false
    }
    true
  }

  val countyList = List(
    "基隆", "宜蘭", "台北", "新北", "桃園",
    "新竹縣", "新竹市", "苗栗", "台中", "南投",
    "彰化", "台南", "高雄", "屏東", "金門")

  import java.io.File

  def importCheckedBuildCase(county: String, file: File) = {
    try {
      val fs = new FileInputStream(file)
      val pkg = OPCPackage.open(fs)
      val wb = new XSSFWorkbook(pkg);
      buildCaseParser(county, wb)
      fs.close()
    } catch {
      case ex: Throwable =>
        Logger.error(s"${file.getAbsolutePath}", ex)
    }
  }

  def buildCaseParser(county: String, wb: XSSFWorkbook) = {
    val sheet = wb.getSheetAt(0)
    val row = sheet.getRow(1)

    val permitID = row.getCell(1).getStringCellValue
    assert(!permitID.isEmpty())
    val builderID = row.getCell(2).getStringCellValue
    val representative = try {
      row.getCell(3).getStringCellValue
    } catch {
      case ex: NullPointerException =>
        ""
    }

    try {
      val phone = row.getCell(4).getStringCellValue
      if (!representative.isEmpty() && isVaildPhone(phone)) {
        for (builderOpt <- Builder.get(builderID)) {
          builderOpt map {
            builder => Builder.upsert(builder.updateContact(representative, phone))
          }
        }
      }
    } catch {
      case ex: Throwable =>
    }

    try {
      val long = row.getCell(10).getNumericCellValue
      val lat = row.getCell(11).getNumericCellValue

      updateLocation(BuildCaseID(county, permitID), Seq(long, lat))
    } catch {
      case x: Throwable =>
    }

    try {
      val area = row.getCell(15).getNumericCellValue
      updateArea(BuildCaseID(county, permitID), area)
    } catch {
      case x: Throwable =>
        None
    }
  }

  import scala.concurrent._
  def monthlyReportParser(wb: XSSFWorkbook) {
    def getCountyInfo(sheetName: String) = {
      val tag = sheetName.take(3)
      if (countyList.contains(tag))
        tag
      else {
        val tag = sheetName.take(2)
        if (countyList.contains(tag))
          tag
        else
          throw new Exception(s"未知的縣市:{tag}")
      }
    }

    var buildCaseSeq = IndexedSeq.empty[BuildCase2]
    for {
      sheetIdx <- 0 to countyList.length * 2 - 1
      sheet = wb.getSheetAt(sheetIdx) if sheet != null
      sheetName = wb.getSheetName(sheetIdx)
    } {

      val personal = sheetName.contains("個人")
      val county = getCountyInfo(sheetName)

      var rowN = 3
      var finishSheet = false

      def getDateOfCell(cell: XSSFCell) = {
        import org.apache.poi.ss.usermodel.CellType
        val cellType = cell.getCellTypeEnum
        cellType match {
          case CellType.NUMERIC =>
            cell.getDateCellValue
          case CellType.STRING =>
            val groupArray = cell.getStringCellValue.split("年|月|日")
            val yearStr = groupArray(0)
            val monthStr = groupArray(1)
            val dayStr = groupArray(2)
            new DateTime().withYear(yearStr.toInt + 1911).withMonthOfYear(monthStr.toInt).
              withDayOfMonth(dayStr.toInt).toLocalDate().toDate()
          case _ =>
            throw new Exception("Unexpected Date format!")
        }
      }

      do {
        var row = sheet.getRow(rowN)
        if (row == null)
          finishSheet = true
        else {
          def companyBuildCase() = {
            val permitDate = getDateOfCell(row.getCell(0))
            val permitID = row.getCell(1).getStringCellValue
            assert(!permitID.isEmpty())
            val builderID = row.getCell(2).getStringCellValue.trim()
            val builderAddr = row.getCell(3).getStringCellValue
            val representative = row.getCell(4).getStringCellValue.trim()
            val usage = row.getCell(5).getStringCellValue
            val architect = row.getCell(6).getStringCellValue
            val floorDesc = row.getCell(7).getStringCellValue
            val addr = row.getCell(8).getStringCellValue.trim()
            val siteInfo = SiteInfo(usage, floorDesc, addr, None, None)
            val location = None

            val builderF = for (builderOpt <- Builder.get(builderID)) yield {
              builderOpt.getOrElse({
                Logger.debug(s"$builderID is new")
                val rawBuilder = Builder.initBuilder(builderID, builderAddr, representative)
                Builder.upsert(rawBuilder)
                rawBuilder
              })
            }

            BuildCase2(
              _id = BuildCaseID(county, permitID),
              builder = builderID,
              personal = false,
              siteInfo = siteInfo,
              permitDate = permitDate, architect = architect,
              state = Some(CaseState.Unknown.toString()))
          }

          def personalBuildCase() = {
            val permitDate = getDateOfCell(row.getCell(0))
            val permitID = row.getCell(1).getStringCellValue
            assert(!permitID.isEmpty())
            val builderID = row.getCell(2).getStringCellValue.trim()
            val usage = row.getCell(3).getStringCellValue
            val architect = row.getCell(4).getStringCellValue.trim()
            val floorDesc = row.getCell(5).getStringCellValue
            val addr = row.getCell(6).getStringCellValue.trim()
            val siteInfo = SiteInfo(usage, floorDesc, addr, None, None)
            val location = None
            val rawBuilder = Builder.initBuilder(builderID, "", "")

            BuildCase2(
              _id = BuildCaseID(county, permitID),
              builder = builderID,
              personal = true,
              siteInfo = siteInfo,
              permitDate = permitDate, architect = architect)

          }

          try {
            def containNull() = {
              val range =
                if (personal)
                  (0 to 6).toList
                else
                  (0 to 8).toList

              range.exists { idx => row.getCell(idx) == null }
            }

            def isPermitIdEmpty = row.getCell(1).getStringCellValue.isEmpty()

            if (containNull || isPermitIdEmpty)
              finishSheet = true
            else {
              val buildCase = if (personal)
                personalBuildCase
              else
                companyBuildCase

              buildCaseSeq = buildCaseSeq :+ buildCase
            }
          } catch {
            case ex: IllegalStateException =>
              Logger.info(s"$sheetIdx:$rowN => Finished", ex)
              finishSheet = true

            case ex: Throwable =>
              Logger.debug(s"$sheetIdx:$rowN =>", ex)
              finishSheet = true
          }
        }
        rowN += 1
      } while (!finishSheet) //end of sheet
      val group = if (personal) "個人" else "公司"
      Logger.info(s"$county:$group=>${rowN - 4} cases")
    } // end of workbook

    import scala.concurrent._
    import org.mongodb.scala.model.Filters._
    val writeModelSeq = buildCaseSeq.map { bc =>
      InsertOneModel(bc)
    }

    val f = collection.bulkWrite(writeModelSeq, new BulkWriteOptions().ordered(false)).toFuture()

    import scala.util._

    f.onComplete {
      case Success(x) =>
        Logger.info(s"Success ${x.getInsertedCount} inserted.")
      case Failure(ex) =>
        ex match {
          case x: org.mongodb.scala.MongoBulkWriteException =>
            Logger.warn(x.getWriteResult.toString)
          case _: Throwable =>
            Logger.error(s"bulk Insert failed ${ex.getMessage}")

        }
    }
  }

  import scala.concurrent._
  def checkOut(editor: String) = {
    val editingF = collection.find(wpFilter(WorkPointType.BuildCase.id)(Filters.eq("editor", editor))).toFuture()
    editingF.onFailure(errorHandler)
    val ff =
      for (editing <- editingF) yield {
        if (!editing.isEmpty)
          Future {
            editing.head
          }
        else {
          import com.mongodb.client.model.ReturnDocument.AFTER
          val f = collection.findOneAndUpdate(
            wpFilter(WorkPointType.BuildCase.id)(Filters.or(Filters.eq("location", null), Filters.eq("siteInfo.area", null))),
            Updates.set("editor", editor),
            FindOneAndUpdateOptions().returnDocument(AFTER)).toFuture()
          f.onFailure(errorHandler)
          f
        }
      }
    ff flatMap { x => x }
  }

  def checkIn(editor: String, bc: BuildCase2) = {
    bc.editor = None

    if (bc.location.isDefined && bc.siteInfo.area.isDefined)
      UsageRecord.addBuildCaseUsage(editor, bc._id)

    upsert(bc)
  }

  def checkOutContractor(editor: String) = {
    val editingF = collection.find(wpFilter(WorkPointType.BuildCase.id)(Filters.eq("editor", editor))).toFuture()
    editingF.onFailure(errorHandler)
    val ff =
      for (editing <- editingF) yield {
        if (!editing.isEmpty)
          Future {
            editing.head
          }
        else {
          import com.mongodb.client.model.ReturnDocument.AFTER
          val tenDayAgo = DateTime.now() - 10.day
          val f = collection.findOneAndUpdate(
            wpFilter(WorkPointType.BuildCase.id)(
              Filters.and(
                Filters.eq("contractor", null),
                Filters.not(Filters.gt("contractorCheckDate", tenDayAgo.toDate())))),
            Updates.set("editor", editor),
            FindOneAndUpdateOptions().returnDocument(AFTER)).toFuture()
          f.onFailure(errorHandler)
          f
        }
      }
    ff flatMap { x => x }
  }

  def upsert(bc: BuildCase2) = {
    val f = collection.replaceOne(Filters.eq("_id", bc._id), bc, UpdateOptions().upsert(true)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def updateArea(_id: BuildCaseID, area: Double) = {
    val f = collection.findOneAndUpdate(Filters.eq("_id", _id), Updates.set("siteInfo.area", area)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def updateLocation(_id: BuildCaseID, location: Seq[Double]) = {
    val f = collection.findOneAndUpdate(Filters.eq("_id", _id), Updates.set("location", location)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def getSortBy(param: QueryParam) = {
    import org.mongodb.scala.model.Sorts.ascending
    val sortByField = param.sortBy.takeWhile { x => !(x == '+' || x == '-') }
    val dir = param.sortBy.contains("+")

    val firstSort =
      if (dir)
        Sorts.ascending(sortByField)
      else
        Sorts.descending(sortByField)

    val secondSort = Sorts.descending("siteInfo.area")
    if (sortByField != "siteInfo.area")
      Sorts.orderBy(firstSort, secondSort)
    else
      firstSort
  }

  def getFilter(param: QueryParam) = {
    import org.mongodb.scala.model.Filters._

    /*
     * case class QueryBuildCase2Param(
  			county: Option[String],
  			builder: Option[String],
  			architect: Option[String],
  			areaGT: Option[Double], areaLT: Option[Double],
  			addr: Option[String],
  			tag: Option[Seq[String]],
  			state: Option[String],
  			sales: Option[String],
  			sortBy: Option[String])
    */

    import org.mongodb.scala.bson.conversions._
    val keywordFilter: Option[Bson] = param.keyword map {
      keyword =>
        val countyFilter = regex("_id.county", "(?i)" + keyword)
        val permitIdFilter = regex("_id.permitID", "(?i)" + keyword)
        val builderFilter = regex("builder", "(?i)" + keyword)
        val architectFilter = regex("architect", "(?i)" + keyword)
        val addrFilter = regex("siteInfo.addr", "(?i)" + keyword)
        or(countyFilter, permitIdFilter, builderFilter, architectFilter, addrFilter)
    }

    val areaGtFilter = param.areaGT map { v => Filters.gt("siteInfo.area", v) }
    val areaLtFilter = param.areaLT map { v => Filters.lt("siteInfo.area", v) }
    val stateFilter = param.state map { v => Filters.eq("state", v) }
    val ownerFilter = param.owner map { sales => regex("owner", "(?i)" + sales) }
    val hasFormFilter = param.hasForm map { has =>
      if (has)
        Filters.ne("form", null)
      else
        Filters.eq("form", null)
    }

    val filterList = List(areaGtFilter, areaLtFilter, stateFilter, ownerFilter, keywordFilter, hasFormFilter).flatMap { f => f }

    val filter = if (!filterList.isEmpty)
      and(filterList: _*)
    else
      Filters.exists("_id")

    filter
  }

  def matchCaseFilter(caseFilter: CaseFilter.Value, dir: String, userID: String)(queryParam: QueryParam) = {
    import CaseFilter._
    caseFilter match {
      case Ownerless =>
        if (dir.equalsIgnoreCase("N"))
          northOwnerless(queryParam)
        else
          southOwnerless(queryParam)
      case MyCase =>
        queryParam.owner = Some(userID)
        queryParam.hasForm = Some(false)
        queryParam.state = Some(CaseState.Unknown.toString)
        getFilter(queryParam)
      case AllCase =>
        if (dir.equalsIgnoreCase("N"))
          northAll(queryParam)
        else
          southAll(queryParam)
      case SubmittedByMe =>
        queryParam.owner = Some(userID)
        queryParam.hasForm = Some(true)
        getFilter(queryParam)
      case SubmittedCases =>
        queryParam.hasForm = Some(true)
        getFilter(queryParam)
      case ClosedByMe =>
        queryParam.owner = Some(userID)
        queryParam.state = Some(CaseState.Closed.toString)
        getFilter(queryParam)
      case ClosedList=>
        queryParam.state = Some(CaseState.Closed.toString)
        getFilter(queryParam)
      case EscalatedByMe=>
        queryParam.owner = Some(userID)
        queryParam.state = Some(CaseState.Escalated.toString)
        getFilter(queryParam)
      case EscalatedList=>
        queryParam.state = Some(CaseState.Escalated.toString)
        getFilter(queryParam)
    }
  }

  import org.mongodb.scala.model._

  def query(param: QueryParam)(skip: Int, limit: Int): Future[Seq[BuildCase2]] = {
    val filter = getFilter(param)
    val sortBy = getSortBy(param)
    query(filter)(sortBy)(skip, limit)
  }

  def count(param: QueryParam): Future[Long] = count(getFilter(param))

  def updateStateByBuilder(builderID: String, state: String) = {
    val f = collection.updateMany(Filters.eq("builder", builderID), Updates.set("state", state)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  import org.mongodb.scala.bson.conversions.Bson
  def query(filter: Bson)(sortBy: Bson = Sorts.descending("siteInfo.area"))(skip: Int, limit: Int) = {
    val f = collection.find(wpFilter(WorkPointType.BuildCase.id)(filter)).sort(sortBy).skip(skip).limit(limit).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def count(filter: Bson) = {
    val f = collection.count(wpFilter(WorkPointType.BuildCase.id)(filter)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  //def myCaseFilter(owner: String) = Filters.eq("owner", owner)

  val northCounty = List(
    "基隆", "宜蘭", "台北", "新北", "桃園",
    "新竹縣", "新竹市")

  val southCounty = List(
    "苗栗", "台中", "南投",
    "彰化", "台南", "高雄", "屏東", "金門")

  val northCaseFilter = Filters.in("_id.county", northCounty: _*)
  val southCaseFilter = Filters.in("_id.county", southCounty: _*)

  def northOwnerless(param: QueryParam) =
    Filters.and(northCaseFilter, Filters.eq("owner", null), Filters.gt("siteInfo.area", 500), getFilter(param))
  def southOwnerless(param: QueryParam) =
    Filters.and(southCaseFilter, Filters.eq("owner", null), Filters.gt("siteInfo.area", 500), getFilter(param))

  def northAll(param: QueryParam) =
    Filters.and(northCaseFilter, Filters.gt("siteInfo.area", 500), getFilter(param))
  def southAll(param: QueryParam) =
    Filters.and(southCaseFilter, Filters.gt("siteInfo.area", 500), getFilter(param))

  def getNorthAll(param: QueryParam) = query(northAll(param))(getSortBy(param)) _
  def getNorthAllCount(param: QueryParam) = count(northAll(param))
  def getNorthOwnerless(param: QueryParam) = query(northOwnerless(param))(getSortBy(param)) _
  def getNorthOwnerlessCount(param: QueryParam) = count(northOwnerless(param))
  def getSouthOwnerless(param: QueryParam) = query(southOwnerless(param))(getSortBy(param)) _
  def getSouthOwnerlessCount(param: QueryParam) = count(southOwnerless(param))
  def getSouthAll(param: QueryParam) = query(southAll(param))(getSortBy(param)) _
  def getSouthAllCount(param: QueryParam) = count(southAll(param))

  def obtain(_id: BuildCaseID, owner: String) = {
    val filter = Filters.and(Filters.eq("_id", _id), Filters.eq("owner", null))
    val f = collection.updateOne(filter, Updates.set("owner", owner)).toFuture()
    UsageRecord.addBuildCaseUsage(owner, _id)
    f.onFailure(errorHandler)
    f
  }

  def release(_id: BuildCaseID, owner: String) = {
    val filter = Filters.and(Filters.eq("_id", _id), Filters.eq("owner", owner))
    val f = collection.updateOne(filter, Updates.set("owner", null)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def getBuildCase(_id: BuildCaseID) = {
    val f = collection.find(Filters.eq("_id", _id)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def getBuildCaseList(ids: Seq[BuildCaseID]) = {
    val f = collection.find(Filters.in("_id", ids: _*)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def getSummaryMap(ids: Seq[BuildCaseID]) = {
    val f = getBuildCaseList(ids)
    for (bcList <- f) yield {
      val pair =
        for (bc <- bcList) yield bc._id -> bc.getSummary

      pair.toMap
    }
  }

  def populateSummary(workPointList: Seq[WorkPoint]) = {
    import scala.language.postfixOps
    val buildCaseIDMap =
      workPointList.filter { wp =>
        wp._id("wpType").asInt32().getValue == WorkPointType.BuildCase.id
      } map {
        wp =>
          Json.parse(wp._id.toJson()).validate[BuildCaseID].asOpt.get -> wp
      } toMap

    val bcSummaryMapF = getSummaryMap(buildCaseIDMap.keys.toSeq)
    for (bcSummaryMap <- bcSummaryMapF) yield {
      for ((bcID, summary) <- bcSummaryMap) yield {
        val wp = buildCaseIDMap(bcID)
        wp.summary = Some(summary)
        wp
      }
    }
  }

  def getNorthDM = query(Filters.and(northCaseFilter, Filters.eq("dm", false)))()(0, 10000)
  def getSouthDM = query(Filters.and(southCaseFilter, Filters.eq("dm", false)))()(0, 10000)

  def splitOwnerless(caseFilter: Bson, userList: Seq[String]) = {
    val bcListF = collection.find(caseFilter).sort(Sorts.descending("siteInfo.area")).toFuture()
    val ret =
      for (bcList <- bcListF) yield {
        val updateModelList =
          for ((bc, idx) <- bcList.zipWithIndex) yield {
            val owner = userList(idx % userList.length)
            UpdateOneModel(Filters.eq("_id", bc._id), Updates.set("owner", owner))
          }
        val f = collection.bulkWrite(updateModelList, BulkWriteOptions().ordered(false)).toFuture()
        f.onFailure(errorHandler)
        f
      }
    ret.flatMap(x => x)
  }
  def splitNorthOwnerless = {
    val userList = User.northSalesList
    val caseFilter = Filters.and(northCaseFilter, Filters.eq("owner", null), Filters.gt("siteInfo.area", 500))
    splitOwnerless(caseFilter, userList)
  }

  def splitSouthOwnerless = {
    val userList = User.southSalesList
    val caseFilter = Filters.and(southCaseFilter, Filters.eq("owner", null), Filters.gt("siteInfo.area", 500))
    splitOwnerless(caseFilter, userList)
  }

  def splitSouthCase(caseFilter: Bson) = {
    val userList = User.southSalesList
    val bcListF = collection.find(caseFilter).sort(Sorts.descending("siteInfo.area")).toFuture()
    val ret =
      for (bcList <- bcListF) yield {
        val updateModelList =
          for ((bc, idx) <- bcList.zipWithIndex) yield {
            val owner = userList(idx % userList.length)
            UpdateOneModel(Filters.eq("_id", bc._id), Updates.set("owner", owner))
          }
        val f = collection.bulkWrite(updateModelList, BulkWriteOptions().ordered(false)).toFuture()
        f.onFailure(errorHandler)
        f
      }
    ret.flatMap(x => x)
  }

  def updateForm(_id: BuildCaseID, form: BuildCaseForm) = {
    val f = collection.updateOne(Filters.eq("_id", _id), Updates.set("form", form)).toFuture()
    f.onFailure(errorHandler)
    f
  }
}