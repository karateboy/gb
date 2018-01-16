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

object BuildCaseState extends Enumeration {
  val Initial = Value
  val GetPhone = Value
  val Closed = Value
}

case class BuildCaseID(county: String, permitID: String, wpType: Int = WorkPoint.BuildCase) extends IWorkPointID
case class SiteInfo(usage: String, floorDesc: String, addr: String, area: Option[Double])

case class BuildCase2(_id: BuildCaseID, builder: String, personal: Boolean,
                      siteInfo: SiteInfo,
                      permitDate: Date, architect: String,
                      var location: Option[Seq[Double]], in: Seq[Input], out: Seq[Output],
                      contractor: Option[String] = None,
                      state: String = BuildCaseState.Initial.toString(), owner: Option[String] = None,
                      tag: Seq[String] = Seq.empty[String],
                      notes: Seq[Note] = Seq.empty[Note], var editor: Option[String] = None) extends IWorkPoint

case class QueryBuildCaseParam2(
  county: Option[String],
  builder: Option[String],
  contact: Option[String],
  phone: Option[String],
  architect: Option[String],
  areaGT: Option[Double], areaLT: Option[Double],
  addr: Option[String],
  yellowAlert: Option[Boolean], redAlert: Option[Boolean],
  tag: Option[Seq[String]],
  state: Option[String],
  sales: Option[String], assistant: Option[String])

object BuildCase2 {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }
  import WorkPoint._

  val codecRegistry = fromRegistries(fromProviders(classOf[BuildCase2], classOf[Input],
    classOf[Output], classOf[Note], classOf[SiteInfo], classOf[BuildCaseID]), DEFAULT_CODEC_REGISTRY)

  val ColName = WorkPoint.ColName
  val collection = MongoDB.database.getCollection[BuildCase2](WorkPoint.ColName).withCodecRegistry(codecRegistry)

  implicit val siWrite = Json.writes[SiteInfo]
  implicit val idWrite = Json.writes[BuildCaseID]
  implicit val bcWrite = Json.writes[BuildCase2]

  implicit val siRead = Json.reads[SiteInfo]
  implicit val idRead = Json.reads[BuildCaseID]
  implicit val bcRead = Json.reads[BuildCase2]

  implicit val qbcRead = Json.reads[QueryBuildCaseParam2]
  implicit val qbcWrite = Json.writes[QueryBuildCaseParam2]

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
            val siteInfo = SiteInfo(usage, floorDesc, addr, None)
            val location = None

            val builderF = for (builderOpt <- Builder.get(builderID)) yield {
              builderOpt.getOrElse({
                Logger.debug(s"$builderID is new")
                val rawBuilder = Builder.initBuilder(builderID, builderAddr, representative)
                Builder.upsert(rawBuilder)
                rawBuilder
              })
            }

            for (builder <- builderF) yield {
              val state = if (!builder.phone.isEmpty())
                BuildCaseState.GetPhone.toString()
              else
                BuildCaseState.Initial.toString()

              BuildCase2(
                _id = BuildCaseID(county, permitID),
                builder = builderID,
                personal = false,
                siteInfo = siteInfo,
                permitDate = permitDate, architect = architect,
                location = None,
                in = Seq.empty[Input], out = Seq.empty[Output],
                state = state)
            }
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
            val siteInfo = SiteInfo(usage, floorDesc, addr, None)
            val location = None
            val rawBuilder = Builder.initBuilder(builderID, "", "")

            BuildCase2(
              _id = BuildCaseID(county, permitID),
              builder = builderID,
              personal = true,
              siteInfo = siteInfo,
              permitDate = permitDate, architect = architect,
              location = None,
              in = Seq.empty[Input], out = Seq.empty[Output])
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
                waitReadyResult(companyBuildCase)

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
    val editingF = collection.find(wpFilter(WorkPoint.BuildCase)(Filters.eq("editor", editor))).toFuture()
    editingF.onFailure(errorHandler)
    val ff =
      for (editing <- editingF) yield {
        if (!editing.isEmpty)
          Future {
            editing.head
          }
        else {
          import com.mongodb.client.model.ReturnDocument.AFTER
          val f = collection.findOneAndUpdate(wpFilter(WorkPoint.BuildCase)(Filters.or(Filters.eq("location", null), Filters.eq("siteInfo.area", null))),
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

  def getFilter(param: QueryBuildCaseParam2) = {
    import org.mongodb.scala.model.Filters._

    /*
     case class QueryBuildCaseParam2(
  		county: Option[String],
  		builder: Option[String],
  		contact: Option[String],
  		phone: Option[String],
  		architect: Option[String],
  		areaGT: Option[Double], areaLT: Option[Double],
  		addr: Option[String],
  		yellowAlert: Option[Boolean], redAlert: Option[Boolean],
  		tag: Option[Seq[String]],
  		state: Option[BuildCaseState.Value],
  		sales: Option[String], assistant: Option[String])
     * 
     *
     * */

    val countyFilter = param.county map { county => regex("county", "(?i)" + county) }
    val builderFilter = param.county map { county => regex("county", "(?i)" + county) }
    val architectFilter = param.architect map { architect => regex("architect", "(?i)" + architect) }
    val addrFilter = param.addr map { addr => regex("addr", "(?i)" + addr) }

    val areaGtFilter = param.areaGT map { v => Filters.gt("area", v) }
    val areaLtFilter = param.areaLT map { v => Filters.lt("area", v) }
    val stateFilter = param.state map { v => equal("state", v) }

    val salesFilter = param.sales map {
      sales =>
        regex("sales", "(?i)" + sales)
    }

    val yellowAlertFilter = param.yellowAlert map { v =>
      if (v == true) {
        val today = DateTime.now
        // today < date + 4*month
        // today - 4*month < date
        val yellowDue = today - 4.month
        and(lt("date", today.toLocalDate().toDate()), gt("date", yellowDue.toLocalDate().toDate()))
      } else
        exists("_id")
    }

    val redAlertFilter = param.redAlert map { v =>
      if (v == true) {
        val today = DateTime.now
        // today > date + 4*month
        // today - 6*month < date
        val yellowDue = today - 4.month
        val redDue = today - 6.month
        and(gt("date", yellowDue.toLocalDate().toDate()), lt("date", redDue.toLocalDate().toDate()))
      } else
        exists("_id")
    }

    val filterList = List(architectFilter, addrFilter,
      countyFilter, areaGtFilter, areaLtFilter, redAlertFilter, yellowAlertFilter,
      stateFilter, salesFilter).flatMap { f => f }

    val filter = if (!filterList.isEmpty)
      and(filterList: _*)
    else
      Filters.exists("_id")

    filter
  }

  import org.mongodb.scala.model._

  def queryBuildCase(param: QueryBuildCaseParam2)(skip: Int, limit: Int) = {
    import org.mongodb.scala.model.Filters._

    val filter = getFilter(param)

    val f = collection.find(filter).sort(Sorts.ascending("permitDate")).skip(skip).limit(limit).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f) yield {
      records
    }
  }

  def queryBuildCaseCount(param: QueryBuildCaseParam2) = {
    val filter = getFilter(param)

    val f = collection.count(filter).toFuture()
    f.onFailure {
      errorHandler
    }
    for (count <- f) yield count
  }

  def upsertBuildCase(buildCase: BuildCase2) = {
    val f = collection.replaceOne(Filters.eq("_id", buildCase._id), buildCase, UpdateOptions().upsert(true)).toFuture()
    f.onFailure {
      errorHandler
    }
    f
  }

  def updateStateByBuilder(builderID: String, state: String) = {
    val f = collection.updateMany(Filters.eq("builder", builderID), Updates.set("state", state)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  import org.mongodb.scala.bson.conversions.Bson
  def query(filter: Bson)(skip: Int, limit: Int) = {
    val sort = Sorts.descending("siteInfo.area")
    val f = collection.find(filter).sort(sort).skip(skip).limit(limit).toFuture()
    f.onFailure(errorHandler)
    f
  }
  def count(filter: Bson) = {
    val f = collection.count(filter).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def myCaseFilter(owner: String) = Filters.eq("owner", owner)
  def getOwnerBuildCase(owner: String) = query(myCaseFilter(owner)) _
  def getOwnerBuildCaseCount(owner: String) = count(myCaseFilter(owner))

  val northCounty = List(
    "基隆", "宜蘭", "台北", "新北", "桃園",
    "新竹縣", "新竹市")

  val southCounty = List(
    "苗栗", "台中", "南投",
    "彰化", "台南", "高雄", "屏東", "金門")

  def northOwnerless() = wpFilter(WorkPoint.BuildCase)(Filters.in("_id.county", northCounty: _*), Filters.eq("owner", null))
  def southOwnerless() = wpFilter(WorkPoint.BuildCase)(Filters.in("_id.county", southCounty: _*), Filters.eq("owner", null))

  def getNorthOwnerless() = query(northOwnerless()) _
  def getNorthOwnerlessCount() = count(northOwnerless())

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
}