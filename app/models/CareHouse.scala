package models
import play.api._
import com.github.nscala_time.time.Imports._
import models.ModelHelper._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import play.api.Play.current

case class CareHouseID(county: String, name: String, wpType: Int = WorkPointType.CareHouse.id) extends IWorkPointID

case class CareHouse(_id: CareHouseID, addr: String, serviceType: Seq[String],
                     phone: String, fax: String, email: String, bed: Int,
                     var location: Option[Seq[Double]] = None, notes: Seq[Note] = Seq.empty[Note],
                     tag: Seq[String] = Seq.empty[String], owner: Option[String] = None, state: Option[String] = None, dm: Boolean = false) extends IWorkPoint {
  def getSummary = {
    val content = s"電話：${phone}<br>" +
      s"地址：${addr}<br>" +
      s"${bed}床<br>"

    Summary(_id.name, content)
  }
}

object CareHouse {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }
  import java.io.File
  import org.mongodb.scala.model._
  import org.mongodb.scala.bson._
  case class QueryParam(
    bedGT: Option[Int] = None, bedLT: Option[Int] = None,
    tag:       Option[Seq[String]] = None,
    state:     Option[String]      = None,
    var owner: Option[String]      = None,
    keyword:   Option[String]      = None,
    sortBy:    String              = "bed+")

  val defaultQueryParam = QueryParam()
  import WorkPoint._
  implicit val chIdRead = Json.reads[CareHouseID]
  implicit val chRead = Json.reads[CareHouse]
  implicit val qRead = Json.reads[QueryParam]
  implicit val chIdWrite = Json.writes[CareHouseID]
  implicit val chWrite = Json.writes[CareHouse]
  implicit val qWrite = Json.writes[QueryParam]

  val codecRegistry = fromRegistries(
    fromProviders(classOf[CareHouse], classOf[CareHouseID], classOf[Note]), DEFAULT_CODEC_REGISTRY)

  val ColName = WorkPoint.ColName
  val collection = MongoDB.database.getCollection[CareHouse](WorkPoint.ColName).withCodecRegistry(codecRegistry)

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
    }

    val f = SysConfig.get(SysConfig.ImportCareHouse)
    for (imported <- f) {
      if (!imported.asBoolean().getValue) {
        if (importRecord) {
          convertAddrToLocation()
          SysConfig.set(SysConfig.ImportCareHouse, new BsonBoolean(true))
        }
      }
    }
  }

  def importRecord() = {
    val path = current.path.getAbsolutePath + "/import/"

    val ret1 = ExcelTool.importXLSX(path + "一般護理之家.xlsx")(parser)
    val ret2 = ExcelTool.importXLSX(path + "養護型機構.xlsx")(parser)
    val ret3 = ExcelTool.importXLSX(path + "精神護理之家.xlsx")(parser)
    val ret4 = ExcelTool.importXLSX(path + "長期照護型機構.xlsx")(parser)
    val ret5 = ExcelTool.importXLSX(path + "安養服務.xlsx")(parser)
    val ret6 = ExcelTool.importXLSX(path + "日間照顧服務.xlsx")(parser)

    ret1 && ret2 && ret3 && ret4 && ret6
  }

  import org.apache.poi.openxml4j.opc._
  import org.apache.poi.xssf.usermodel._
  def parser(sheet: XSSFSheet) = {
    var rowN = 1
    var finish = false
    var seq = IndexedSeq.empty[CareHouse]
    do {
      var row = sheet.getRow(rowN)
      if (row == null)
        finish = true
      else {
        try {
          val name = row.getCell(0).getStringCellValue
          val county = row.getCell(1).getStringCellValue
          val _id = CareHouseID(county, name)
          val addr = row.getCell(2).getStringCellValue
          val serviceType = row.getCell(3).getStringCellValue.split(",")
          val phone = row.getCell(4).getStringCellValue
          val fax = row.getCell(5).getStringCellValue
          val email = row.getCell(6).getStringCellValue
          val bed = row.getCell(7).getNumericCellValue.toInt
          val ch = CareHouse(_id = _id, addr = addr, serviceType = serviceType, phone = phone,
            fax = fax, email = email, bed = bed)

          seq = seq :+ ch
        } catch {
          case ex: java.lang.NullPointerException =>
          // last row Ignore it...

          case ex: Throwable =>
            Logger.error(s"failed to convert row=$rowN...", ex)
        }
      }
      rowN += 1
    } while (!finish)

    seq.map { bc =>

    }

    val writeModelSeq = seq.map { ch =>
      ReplaceOneModel(Filters.eq("_id", ch._id), ch, UpdateOptions().upsert(true))
    }

    val f = collection.bulkWrite(writeModelSeq, new BulkWriteOptions().ordered(false)).toFuture()

    f.onFailure(errorHandler)
    waitReadyResult(f)
  }

  def convertAddrToLocation() = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._
    import WorkPoint.wpFilter

    val noLocationListF = collection.find(wpFilter(WorkPointType.CareHouse.id)(Filters.eq("location", null))).toFuture()
    for (noLocationList <- noLocationListF) {
      var failed = 0
      Logger.info(s"no location list #=${noLocationList.length}")
      noLocationList.map {
        careHouse =>
          assert(careHouse.location.isEmpty)
          val locationList = GoogleApi.queryAddr(careHouse.addr)
          if (!locationList.isEmpty) {
            val location = locationList(0)
            careHouse.location = Some(location)
            val f = collection.updateOne(Filters.eq("_id", careHouse._id), Updates.set("location", location)).toFuture()
            f.onFailure(errorHandler)
            Logger.info(s"${careHouse.addr} 轉換成功!")
          } else {
            failed += 1
            Logger.warn(s"${careHouse.addr} 無法轉換!")
          }
      }
      Logger.info(s"共 ${failed} 筆無法轉換")
    }
  }

  def upsert(ch: CareHouse) = {
    val f = collection.replaceOne(Filters.eq("_id", ch._id), ch, UpdateOptions().upsert(true)).toFuture()
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

    val secondSort = Sorts.descending("bed")
    if (sortByField != "bed")
      Sorts.orderBy(firstSort, secondSort)
    else
      firstSort
  }

  def getFilter(param: QueryParam) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.bson.conversions._
    val keywordFilter: Option[Bson] = param.keyword map {
      keyword =>
        val countyFilter = regex("_id.county", "(?i)" + keyword)
        val nameFilter = regex("_id.name", "(?i)" + keyword)
        val addrFilter = regex("addr", "(?i)" + keyword)
        or(countyFilter, nameFilter, addrFilter)
    }

    val bedGtFilter = param.bedGT map { v => Filters.gt("bed", v) }
    val bedLtFilter = param.bedLT map { v => Filters.lt("bed", v) }
    val stateFilter = param.state map { v => Filters.eq("state", v) }
    val ownerFilter = param.owner map { sales => regex("owner", "(?i)" + sales) }

    val filterList = List(bedGtFilter, bedLtFilter, stateFilter, ownerFilter, keywordFilter).flatMap { f => f }

    val filter = if (!filterList.isEmpty)
      and(filterList: _*)
    else
      Filters.exists("_id")

    filter
  }

  import scala.concurrent._
  import org.mongodb.scala.bson.conversions._
  def query(param: QueryParam)(skip: Int, limit: Int): Future[Seq[CareHouse]] = {
    val filter = getFilter(param)
    val sortBy = getSortBy(param)
    query(filter)(sortBy)(skip, limit)
  }
  def count(param: QueryParam): Future[Long] = count(getFilter(param))

  import org.mongodb.scala.bson.conversions.Bson
  import WorkPoint.wpFilter
  def careHouseFilter(bsons: Bson*) = wpFilter(WorkPointType.CareHouse.id)(bsons: _*)

  def query(filter: Bson)(sortBy: Bson = Sorts.descending("bed"))(skip: Int, limit: Int) = {
    val f = collection.find(careHouseFilter(filter)).sort(sortBy).skip(skip).limit(limit).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def count(filter: Bson) = {
    val f = collection.count(careHouseFilter(filter)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  val northCounty = List(
    "基隆市", "宜蘭縣", "台北市", "新北市", "桃園市",
    "新竹縣", "新竹市")

  def northOwnerless(param: QueryParam) =
    Filters.and(Filters.in("_id.county", northCounty: _*), Filters.eq("owner", null), getFilter(param))
  def southOwnerless(param: QueryParam) =
    Filters.and(Filters.nin("_id.county", northCounty: _*), Filters.eq("owner", null), getFilter(param))
  def northAll(param: QueryParam) =
    Filters.and(Filters.in("_id.county", northCounty: _*), getFilter(param))
  def southAll(param: QueryParam) =
    Filters.and(Filters.nin("_id.county", northCounty: _*), getFilter(param))

  val northCaseFilter = Filters.in("_id.county", northCounty: _*)
  val southCaseFilter = Filters.nin("_id.county", northCounty: _*)

  def getNorthOwnerless(param: QueryParam) = query(northOwnerless(param))(getSortBy(param)) _
  def getNorthOwnerlessCount(param: QueryParam) = count(northOwnerless(param))
  def getSouthOwnerless(param: QueryParam) = query(southOwnerless(param))(getSortBy(param)) _
  def getSouthOwnerlessCount(param: QueryParam) = count(southOwnerless(param))

  def getNorthAll(param: QueryParam) = query(northAll(param))(getSortBy(param)) _
  def getNorthAllCount(param: QueryParam) = count(northAll(param))
  def getSouthAll(param: QueryParam) = query(southAll(param))(getSortBy(param)) _
  def getSouthAllCount(param: QueryParam) = count(southAll(param))

  def obtain(_id: CareHouseID, owner: String) = {
    val filter = Filters.and(Filters.eq("_id", _id), Filters.eq("owner", null))
    val f = collection.updateOne(filter, Updates.set("owner", owner)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def release(_id: CareHouseID, owner: String) = {
    val filter = Filters.and(Filters.eq("_id", _id), Filters.eq("owner", owner))
    val f = collection.updateOne(filter, Updates.set("owner", null)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def getCareHouse(_id: CareHouseID) = {
    val f = collection.find(Filters.eq("_id", _id)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def getCareHouseList(ids: Seq[CareHouseID]) = {
    val f = collection.find(Filters.in("_id", ids: _*)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def getSummaryMap(ids: Seq[CareHouseID]) = {
    val f = getCareHouseList(ids)
    for (bcList <- f) yield {
      val pair =
        for (bc <- bcList) yield bc._id -> bc.getSummary

      pair.toMap
    }
  }

  def populateSummary(workPointList: Seq[WorkPoint]) = {
    import scala.language.postfixOps
    val careHouseIDMap =
      workPointList.filter { wp =>
        wp._id("wpType").asInt32().getValue == WorkPointType.CareHouse.id
      } map {
        wp =>
          Json.parse(wp._id.toJson()).validate[CareHouseID].asOpt.get -> wp
      } toMap

    val bcSummaryMapF = getSummaryMap(careHouseIDMap.keys.toSeq)
    for (bcSummaryMap <- bcSummaryMapF) yield {
      for ((bcID, summary) <- bcSummaryMap) yield {
        val wp = careHouseIDMap(bcID)
        wp.summary = Some(summary)
        wp
      }
    }
  }

  def splitOwnerless(caseFilter: Bson, userList: Seq[String]) = {
    val bcListF = collection.find(careHouseFilter(caseFilter)).sort(Sorts.descending("bed")).toFuture()
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
    val caseFilter = Filters.and(northCaseFilter, Filters.eq("owner", null))
    splitOwnerless(caseFilter, userList)
  }

  def splitSouthOwnerless = {
    val userList = User.southSalesList
    val caseFilter = Filters.and(southCaseFilter, Filters.eq("owner", null))
    splitOwnerless(caseFilter, userList)
  }
}