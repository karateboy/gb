package models
import play.api._
import com.github.nscala_time.time.Imports._
import models.ModelHelper._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import play.api.Play.current
import scala.collection.JavaConversions._

case class FacilityType(_id: Int, typeID: String, name: String)
object FacilityType extends Enumeration {
  val Factory = Value(1)
  val ProcessPlant = Value(2)
  val RecyclePlant = Value(3)

  val map = Map(
    Factory -> "事業機構",
    ProcessPlant -> "處理機構",
    RecyclePlant -> "再利用機構")

  //implicit val write = Json.writes[FacilityType]

  implicit val tReads: Reads[FacilityType.Value] = EnumUtils.enumReads(FacilityType)
  implicit val tWrites: Writes[FacilityType.Value] = EnumUtils.enumFormat(FacilityType)

  val workList = List(Factory, ProcessPlant, RecyclePlant)

  def getList =
    for (key <- values.toSeq) yield FacilityType(key.id, key.toString(), map(key))

  def getWorkList =
    for (key <- workList) yield FacilityType(key.id, key.toString(), map(key))
}

import java.util.Date
case class AirPollutant(VOC: Double = 0, TSP: Double = 0, SOx: Double = 0, NOx: Double = 0,
                        noVOCtotal: Double = 0, total: Double = 0)

case class WasteInput(wasteCode: String, wasteName: String, method: String, totalQuantity: Double, deadLine: Date, price: Option[Double])
case class WasteOutput(date: Date, wasteCode: String, wasteName: String, method: String, quantity: Double, unit: String)

case class Facility(_id: String, name: String, fcType: Int, addr: Option[String], phone: Option[String],
                    var location: Option[Seq[Double]] = None, wasteIn: Option[Seq[WasteInput]] = None,
                    wasteOut: Option[Seq[WasteOutput]] = None, notes: Option[Seq[Note]] = None,
                    tag: Option[Seq[String]] = None, owner: Option[String] = None, state: Option[String] = None,
                    pollutant: Option[AirPollutant] = None, grade: Option[String] = None, checkDate: Option[Date] = None,
                    contact: Option[String] = None) {
  def getSummary = {
    val content = s"電話：${phone}<br>" +
      s"地址：${addr}<br>"

    Summary(name, content)
  }
}

object Facility {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }
  import java.io.File
  import org.mongodb.scala.model._
  import org.mongodb.scala.bson._

  case class QueryParam(
    tag:       Option[Seq[String]] = None,
    state:     Option[String]      = None,
    var owner: Option[String]      = None,
    keyword:   Option[String]      = None,
    sortBy:    String              = "pollutant.noVOCtotal+")

  import WorkPoint.outputWrite
  import WorkPoint.outRead
  import WorkPoint.noteRead
  import WorkPoint.noteWrite
  implicit val pRead = Json.reads[AirPollutant]
  implicit val pWrite = Json.writes[AirPollutant]
  implicit val wiRead = Json.reads[WasteInput]
  implicit val wiWrite = Json.writes[WasteInput]
  implicit val woRead = Json.reads[WasteOutput]
  implicit val woWrite = Json.writes[WasteOutput]
  implicit val qpRead = Json.reads[QueryParam]
  implicit val qpWrite = Json.writes[QueryParam]
  implicit val fcRead = Json.reads[Facility]
  implicit val fcWrite = Json.writes[Facility]

  val codecRegistry = fromRegistries(
    fromProviders(classOf[Facility], classOf[Note], classOf[WasteInput], classOf[WasteOutput], classOf[AirPollutant]), DEFAULT_CODEC_REGISTRY)

  val ColName = "facility"
  val collection = MongoDB.database.getCollection[Facility](ColName).withCodecRegistry(codecRegistry)

  def resetSysConfig = {
    Logger.info("Reset facility related SysConfig")
    val f2 = SysConfig.set(SysConfig.ImportFacilityPollutant, BsonBoolean(false))
    val f3 = SysConfig.set(SysConfig.ImportProcessPlant1, BsonBoolean(false))
    val f4 = SysConfig.set(SysConfig.ImportProcessPlant2, BsonBoolean(false))
    val f5 = SysConfig.set(SysConfig.GrabWasteInfo, BsonBoolean(false))
    import scala.concurrent._
    val f = Future.sequence(Seq(f2, f3, f4))
    waitReadyResult(f)
  }
  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      resetSysConfig
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      waitReadyResult(f)

      val cf1 = collection.createIndex(Indexes.ascending("fcType")).toFuture()
      val cf2 = collection.createIndex(Indexes.ascending("name")).toFuture()
      val cf3 = collection.createIndex(Indexes.geo2dsphere("location")).toFuture()
      val cf4 = collection.createIndex(
        Indexes.compoundIndex(Indexes.ascending("fcType"), Indexes.geo2dsphere("location"))).toFuture()

      val cf5 = collection.createIndex(Indexes.ascending("pollutant.noVOCtotal")).toFuture()
      val cf6 = collection.createIndex(Indexes.ascending("wasteIn.wasteCode")).toFuture()
      val cf7 = collection.createIndex(Indexes.ascending("wasteOut.wasteCode")).toFuture()
      val cf8 = collection.createIndex(Indexes.ascending("grade")).toFuture()

      cf1.onFailure(errorHandler)
      cf2.onFailure(errorHandler)
      cf3.onFailure(errorHandler)
      cf4.onFailure(errorHandler)
      cf5.onFailure(errorHandler)
      cf6.onFailure(errorHandler)
      cf7.onFailure(errorHandler)
      cf8.onFailure(errorHandler)
    }

    {
      val imported = waitReadyResult(SysConfig.get(SysConfig.ImportFacilityPollutant))
      if (!imported.asBoolean().getValue) {
        if (importAirPollutant) {
          SysConfig.set(SysConfig.ImportFacilityPollutant, BsonBoolean(true))
        }
      }
    }

    {
      val imported = waitReadyResult(SysConfig.get(SysConfig.ImportProcessPlant1))
      if (!imported.asBoolean().getValue) {
        if (importProcessPlant1) {
          SysConfig.set(SysConfig.ImportProcessPlant1, BsonBoolean(true))
        }
      }
    }

    {
      val imported = waitReadyResult(SysConfig.get(SysConfig.ImportProcessPlant2))
      if (!imported.asBoolean().getValue) {
        if (importProcessPlant2) {
          SysConfig.set(SysConfig.ImportProcessPlant2, BsonBoolean(true))
        }
      }

      val importRecycle = waitReadyResult(SysConfig.get(SysConfig.ImportRecyclePlant))
      if (!importRecycle.asBoolean().getValue) {
        if (importRecyclePlant) {
          SysConfig.set(SysConfig.ImportRecyclePlant, BsonBoolean(true))
        }
      }
    }
  }

  case class FacilityGeoObj(features: Seq[FacilityObj])
  case class FacilityObj(properties: FacilityProperty, geometry: Geometry)
  case class Geometry(coordinates: Seq[Double])
  case class FacilityProperty(fac_no: String, fac_name: String, fac_addr: Option[String], fc_type: String)
  def importFacility() = {
    import play.api.libs.json.Reads._ // Custom validation helpers
    import play.api.libs.functional.syntax._ // Combinator syntax
    import java.io.FileInputStream
    val path = current.path.getAbsolutePath + "/import/"
    val geoJsonFile = path + "facility.geojson"
    val jsValue = Json.parse(new FileInputStream(geoJsonFile))

    implicit val geoRead = Json.reads[Geometry]
    implicit val fcPropRead: Reads[FacilityProperty] =
      ((__ \ "fac_no").read[String] and
        (__ \ "fac_name").read[String] and
        (__ \ "fac_addr").readNullable[String] and
        (__ \ "type").read[String])(FacilityProperty.apply _)
    implicit val fcObjRead = Json.reads[FacilityObj]
    implicit val fcGeoRead = Json.reads[FacilityGeoObj]

    val ret = jsValue.validate[FacilityGeoObj]
    ret.fold(
      err => {
        Logger.error(JsError.toJson(err).toString())
      },
      obj => {
        Logger.info(s"total ${obj.features.length} facilities")
        val facilities = obj.features map {
          fcObj =>
            val props = fcObj.properties
            val fcType = props.fc_type match {
              case "事業機構"  => FacilityType.Factory.id
              case "再利用機構" => FacilityType.RecyclePlant.id
              case "處理機構"  => FacilityType.ProcessPlant.id
            }
            val location = if (fcObj.geometry.coordinates.length == 2)
              Some(fcObj.geometry.coordinates)
            else
              None
            Facility(
              _id = props.fac_no,
              name = props.fac_name,
              fcType = fcType,
              addr = props.fac_addr,
              phone = None,
              location = location)
        }

        val writeModelSeq = facilities map { facility =>
          val updates = Updates.combine(
            Updates.set("addr", facility.addr),
            Updates.set("location", facility.location))
          UpdateOneModel(Filters.eq("_id", facility._id), updates)
        }

        val f = collection.bulkWrite(writeModelSeq, BulkWriteOptions().ordered(false)).toFuture()

        f.onFailure(errorHandler)
        val ret = waitReadyResult(f)
        Logger.info(s"${ret.getModifiedCount} facilities has been updated.")
      })

    true
  }

  def importAirPollutant() = {
    import com.github.tototoshi.csv._
    val reader = CSVReader.open(new File(current.path.getAbsolutePath + "/import/air_pollutant.csv"))
    val recordList = reader.allWithHeaders()
    val updateModelList = recordList map { pollutant =>
      val airPollutant = AirPollutant(
        VOC = pollutant("VOCs").toDouble,
        TSP = pollutant("TSP").toDouble,
        SOx = pollutant("SOx").toDouble,
        NOx = pollutant("NOx").toDouble,
        noVOCtotal = pollutant("TSP").toDouble + pollutant("SOx").toDouble + pollutant("NOx").toDouble,
        total = pollutant("VOCs").toDouble + pollutant("TSP").toDouble + pollutant("SOx").toDouble + pollutant("NOx").toDouble)

      val updates = Updates.combine(
        Updates.set("name", pollutant("FacilityName")),
        Updates.set("fcType", FacilityType.Factory.id),
        Updates.set("pollutant", airPollutant))
      UpdateOneModel(Filters.eq("_id", pollutant("FacilityID")), updates, UpdateOptions().upsert(true))
    }
    Logger.info(s"更新 ${updateModelList.length} 空汙紀錄")

    val f = collection.bulkWrite(updateModelList, BulkWriteOptions().ordered(false)).toFuture()
    f.onFailure(errorHandler)
    val ret = waitReadyResult(f)
    Logger.info(s"${ret.getMatchedCount} 空汙染比對成功. 新增=${ret.getUpserts().length}")

    true
  }

  def importProcessPlant(fileName: String, grade: String) = {
    val elem = xml.XML.loadFile(new File(current.path.getAbsolutePath + "/import/" + fileName))
    var plantWasteInputMap = Map.empty[String, Seq[WasteInput]]
    var plantNoNameMap = Map.empty[String, String]
    elem match {
      case <EmsData>{ emsData @ _* }</EmsData> =>
        Logger.info(s"${emsData.length}筆資料")
        val dataSeq = emsData.filter(_.label == "Data")
        for (dataNode <- dataSeq) {
          val plantNo = dataNode \ "Tre_No"
          val company_name = dataNode \ "Tre_Name"
          val deadLine = dataNode \ "GrantDeadline"
          val totalQuantity = dataNode \ "GrantTotalQty"
          val wasteCode = dataNode \ "waste_no"
          val wasteName = dataNode \ "Waste_name"
          val method = dataNode \ "TreMethodName"
          val date = deadLine.text.split(" ")(0).toDateTime("YYYY/MM/dd").toDate()
          val wi = WasteInput(wasteCode.text, wasteName.text, method.text, totalQuantity.text.toDouble, date, None)
          val seq = plantWasteInputMap.getOrElse(plantNo.text, Seq.empty[WasteInput])
          plantWasteInputMap = plantWasteInputMap + (plantNo.text -> (seq :+ (wi)))
          plantNoNameMap = plantNoNameMap + (plantNo.text -> company_name.text)
        }
    }
    Logger.info(s"共 ${plantWasteInputMap.size} 廠")
    val updateModels = for ((plantNo, wiSeq) <- plantWasteInputMap) yield {
      val updates = Updates.combine(
        Updates.set("name", plantNoNameMap(plantNo)),
        Updates.set("fcType", FacilityType.ProcessPlant.id),
        Updates.addEachToSet("wasteIn", wiSeq: _*),
        Updates.set("grade", grade))
      UpdateOneModel(Filters.eq("_id", plantNo), updates, UpdateOptions().upsert(true))
    }

    val f = collection.bulkWrite(updateModels.toList, BulkWriteOptions().ordered(false)).toFuture()
    val ret = waitReadyResult(f)
    val upserts = ret.getUpserts.length
    Logger.info(s"${ret.getModifiedCount} ${grade}級處理機構資訊已更新. 新增=(${upserts})")

    true
  }

  def importCleanPlant(fileName: String, grade: String) = {
    val elem = xml.XML.loadFile(new File(current.path.getAbsolutePath + "/import/" + fileName))
    var plantWasteInputMap = Map.empty[String, Seq[WasteInput]]
    elem match {
      case <EmsData>{ emsData @ _* }</EmsData> =>
        Logger.info(s"${emsData.length}")
        val dataSeq = emsData.filter(_.label == "Data")
        for (dataNode <- dataSeq) {
          val plantNo = dataNode \ "Cle_No"
          val company_name = dataNode \ "Cle_Name"
          val deadLine = dataNode \ "GrantDeadline"
          val totalQuantity = dataNode \ "GrantTotalQty"
          val wasteCode = dataNode \ "waste_no"
          val wasteName = dataNode \ "Waste_name"
          val method = dataNode \ "TreMethodName"
          val date = deadLine.text.split(" ")(0).toDateTime("YYYY/MM/dd").toDate()
          val wi = WasteInput(wasteCode.text, wasteName.text, method.text, totalQuantity.text.toDouble, date, None)
          val seq = plantWasteInputMap.getOrElse(plantNo.text, Seq.empty[WasteInput])
          plantWasteInputMap = plantWasteInputMap + (plantNo.text -> (seq :+ (wi)))
        }
    }
    Logger.info(s"total ${plantWasteInputMap.size} plants")
    val updateModels = for ((plantNo, wiSeq) <- plantWasteInputMap) yield {
      val updates = Updates.combine(Updates.set("wasteIn", wiSeq), Updates.set("grade", grade))
      UpdateOneModel(Filters.eq("_id.no", plantNo), updates)
    }

    val f = collection.bulkWrite(updateModels.toList, BulkWriteOptions().ordered(false)).toFuture()
    val ret = waitReadyResult(f)
    Logger.info(s"${ret.getModifiedCount} ${grade}級處理機構資訊已更新.")

    true
  }

  def importProcessPlant1 = importProcessPlant("甲級處理機構.xml", "甲")
  def importProcessPlant2 = importProcessPlant("乙級處理機構.xml", "乙")
  def importProcessPlant3 = importCleanPlant("丙級處理機構.xml", "丙")

  def importRecyclePlant() = {
    def listFiles = {
      val dir = current.path.getAbsolutePath + "/import/recycle/"
      val allFiles = new java.io.File(dir).listFiles().toList
      allFiles.filter(p => p != null)
    }

    for (f <- listFiles) {
      try {
        ExcelTool.importXLSX(f, false)(Facility.recyclePlantParser)
      } catch {
        case ex: Throwable =>
          Logger.error(s"Failed to process ${f.getAbsolutePath}", ex)
      }
    }

    true
  }

  def upsert(ch: Facility) = {
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
        val noFilter = regex("_id.no", keyword)
        val nameFilter = regex("_id.name", "(?i)" + keyword)
        val addrFilter = regex("addr", "(?i)" + keyword)
        or(noFilter, nameFilter, addrFilter)
    }

    val stateFilter = param.state map { v => Filters.eq("state", v) }
    val ownerFilter = param.owner map { sales => regex("owner", "(?i)" + sales) }

    val filterList = List(stateFilter, ownerFilter, keywordFilter).flatMap { f => f }

    val filter = if (!filterList.isEmpty)
      and(filterList: _*)
    else
      Filters.exists("_id")

    filter
  }

  import scala.concurrent._
  import org.mongodb.scala.bson.conversions._
  def query(param: QueryParam)(skip: Int, limit: Int): Future[Seq[Facility]] = {
    val filter = getFilter(param)
    val sortBy = getSortBy(param)
    query(filter)(sortBy)(skip, limit)
  }
  def count(param: QueryParam): Future[Long] = count(getFilter(param))

  import org.mongodb.scala.bson.conversions.Bson
  import WorkPoint.wpFilter
  def careHouseFilter(bsons: Bson*) = wpFilter(WorkPointType.CareHouse.id)(bsons: _*)

  def query(filter: Bson)(sortBy: Bson = Sorts.descending("siteInfo.area"))(skip: Int, limit: Int) = {
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

  val northCaseFilter = Filters.in("_id.county", northCounty: _*)
  val southCaseFilter = Filters.nin("_id.county", northCounty: _*)
  def northAll(param: QueryParam) =
    Filters.and(northCaseFilter, getFilter(param))
  def southAll(param: QueryParam) =
    Filters.and(southCaseFilter, getFilter(param))

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

  def get(_id: String) = {
    val f = collection.find(Filters.eq("_id", _id)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def getList(ids: Seq[String]) = {
    val f = collection.find(Filters.in("_id", ids: _*)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def getSummaryMap(ids: Seq[String]) = {
    val f = getList(ids)
    for (bcList <- f) yield {
      val pair =
        for (bc <- bcList) yield bc._id -> ""

      pair.toMap
    }
  }

  def wasteGrabber(no: String) = {
    import org.jsoup._
    val url = s"https://prtr.epa.gov.tw/FacilityInfo/DetailIndex?registrationno=${no}&keyword=${no}"
    try {
      val doc = Jsoup.connect(url).get
      val latElem = doc.getElementById("hidLat")
      val latOpt =
        if (latElem != null && latElem.attr("value") != null)
          Some(latElem.attr("value").toDouble)
        else
          None
      val lonElem = doc.getElementById("hidLon")
      val lonOpt =
        if (lonElem != null && lonElem.attr("value") != null)
          Some(lonElem.attr("value").toDouble)
        else
          None
      val detailInfo = doc.getElementById("divDetailInfo")
      val basicInfo = detailInfo.child(1).child(0)
      val addrElm = basicInfo.child(1)
      val addr = addrElm.text().split("：")(1)

      val latestReport = doc.getElementById("divLatestReport")
      val airDiv = latestReport.child(0)
      val waterDiv = latestReport.child(1)
      val wasteDiv = latestReport.child(2)
      val wasteTab = wasteDiv.child(2)
      val entries = wasteTab.child(0).children()

      val woList =
        for { (entry, idx) <- entries.zipWithIndex if idx > 0 } yield {
          val dateTime = entry.child(0).text().toDateTime("YYYY 年 MM 月")
          val waste = entry.child(1).text().split(" ")
          val wasteCode = waste(0)
          val wasteName = waste(1)
          val method = entry.child(2).text()
          val quantity = entry.child(3).text().toDouble
          val unit = entry.child(4).text()
          WasteOutput(
            date = dateTime.toDate(),
            wasteCode = wasteCode, wasteName = wasteName, method = method, quantity = quantity, unit = unit)
        }
      val locationOpt = for {
        lon <- lonOpt
        lat <- latOpt
      } yield Seq(lon, lat)

      val updates = Updates.combine(
        Updates.set("addr", addr),
        Updates.set("location", locationOpt.getOrElse(None)),
        Updates.addEachToSet("wasteOut", woList.toSeq: _*),
        Updates.set("checkDate", DateTime.now().toDate()))
      val f = collection.updateOne(Filters.eq("_id", no), updates).toFuture()
      f.onFailure(errorHandler)
      true
    } catch {
      case ex: Exception =>
        Logger.error(s"無 ${no} 資料", ex)
        val updates = Updates.set("checkDate", DateTime.now().toDate())
        val f = collection.updateOne(Filters.eq("_id", no), updates).toFuture()
        f.onFailure(errorHandler)
        false
    }
  }

  def grabFactoryInfo(no: String) = {
    import org.jsoup._
    val url = s"https://waste.epa.gov.tw/prog/view_data/view_fac.asp?fac_no=${no}"
    try {
      //val file = new File(current.path.getAbsolutePath + "/import/列管事業機構基本資料.html")
      val doc = Jsoup.connect(url).get
      val infoTab = doc.getElementById("OrgInfoTable")
      val infoList = infoTab.child(1)
      val epDep = infoList.child(7)
      val phone = epDep.child(1).text()
      Logger.debug(s"phone=$phone")
      val contact = epDep.child(3).text()
      Logger.debug(s"contact=$contact")

      val updates = Updates.combine(
        Updates.set("phone", phone),
        Updates.set("contact", contact))
      val f = collection.updateOne(Filters.eq("_id", no), updates).toFuture()
      f.onFailure(errorHandler)
      true
    } catch {
      case ex: Exception =>
        Logger.error(s"unable to handle ${no}", ex)
        false
    }
  }

  def grabProcessPlantInfo(no: String) = {
    import org.jsoup._
    val url = s"https://waste.epa.gov.tw/prog/view_data/view_tre.asp?tre_no=${no}"
    try {
      val doc = Jsoup.connect(url).get
      val infoTab = doc.getElementById("OrgInfoTable")
      val infoList = infoTab.child(1)
      val contactTr = infoList.child(8)
      val contact = contactTr.child(1).text()
      val phoneTr = infoList.child(9)
      val phone = phoneTr.child(1).text()
      Logger.debug(s"phone=$phone contact=$contact")

      val updates = Updates.combine(
        Updates.set("phone", phone),
        Updates.set("contact", contact))
      val f = collection.updateOne(Filters.eq("_id", no), updates).toFuture()
      f.onFailure(errorHandler)
      true
    } catch {
      case ex: Exception =>
        Logger.error(s"unable to handle ${no}", ex)
        false
    }
  }

  def grabWasteInfoList = {
    val oneWeekBefore = DateTime.now() - 7.day
    val filter1 = Filters.and(Filters.or(Filters.eq("addr", null), Filters.eq("location", null)), Filters.eq("checkDate", null))
    val filter2 = Filters.lt("checkDate", oneWeekBefore.toDate())
    val filter = Filters.or(filter1, filter2)
    val f = collection.find(filter).toFuture()
    f.onFailure(errorHandler)
    for (ret <- f) yield ret
  }

  def getList = {
    val f = collection.find().toFuture()
    f.onFailure(errorHandler)
    for (ret <- f) yield ret
  }

  def getFactoryList = {
    val filter = Filters.eq("fcType", 1)
    val f = collection.find(filter).toFuture()
    f.onFailure(errorHandler)
    for (ret <- f) yield ret
  }

  def getFactoryListByPollutant = {
    val filter = Filters.eq("fcType", 1)
    val f = collection.find(filter).sort(Sorts.descending("pollutant.noVOCtotal")).toFuture()
    f.onFailure(errorHandler)
    for (ret <- f) yield ret
  }

  def getProcessPlantList = {
    val filter = Filters.eq("fcType", 2)
    val f = collection.find(filter).toFuture()
    f.onFailure(errorHandler)
    for (ret <- f) yield ret
  }

  def findTop3ProcessPlant(location: Seq[Double], wasteCode: String) = {
    val geometry = geojson.Point(geojson.Position(location: _*))
    val filter = Filters.and(
      Filters.elemMatch("wasteIn", Filters.eq("wasteCode", wasteCode)),
      Filters.nearSphere("location", geometry))
    val f = collection.find(filter).limit(3).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def recyclePlantParser(sheet: org.apache.poi.xssf.usermodel.XSSFSheet) = {
    import org.mongodb.scala.model._
    var rowN = 1
    var finish = false
    var seq = Seq.empty[UpdateOneModel[Nothing]]
    do {
      var row = sheet.getRow(rowN)
      if (row == null)
        finish = true
      else {
        try {
          //WasteInput(wasteCode: String, wasteName: String, method: String, totalQuantity: Double, deadLine: Date, price: Option[Double])
          import com.github.nscala_time.time.Imports._
          val id = row.getCell(1).getStringCellValue
          val name = row.getCell(2).getStringCellValue
          val phone = row.getCell(3).getStringCellValue
          val addr = row.getCell(4).getStringCellValue
          val wasteCode = row.getCell(5).getStringCellValue.drop(1).takeWhile(_ != ')')
          val wasteName = row.getCell(5).getStringCellValue.dropWhile(_ != ')').drop(1)
          val deadLine = row.getCell(9).getDateCellValue
          val wi = WasteInput(wasteCode, wasteName, "", 10000, deadLine, None)
          val updates = Updates.combine(
            Updates.set("name", name),
            Updates.set("phone", phone),
            Updates.set("addr", addr),
            Updates.set("fcType", FacilityType.RecyclePlant.id),
            Updates.addToSet("wasteIn", wi))
          val model = UpdateOneModel(Filters.eq("_id", id), updates, UpdateOptions().upsert(true))
          seq = seq.:+(model)
        } catch {
          case ex: java.lang.NullPointerException =>
          // last row Ignore it...

          case ex: Throwable =>
            Logger.error(s"failed to convert row=$rowN...", ex)
        }
      }
      rowN += 1
    } while (!finish)

    val f = collection.bulkWrite(seq, BulkWriteOptions().ordered(false)).toFuture()

    f.onFailure(errorHandler)
    val ret = waitReadyResult(f)
    Logger.info(s"${ret.getModifiedCount} 再利用廠已經更新  upserts(${ret.getUpserts.length})")
  }
}