package models
import play.api._
import com.github.nscala_time.time.Imports._
import models.ModelHelper._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import play.api.Play.current

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
case class FacilityID(no: String, name: String, fcType: Int)
case class WasteInput(wasteCode: String, wasteName: String, method: String, totalQuantity: Double, deadLine: Date, price: Option[Double])
case class Facility(_id: FacilityID, addr: Option[String], phone: Option[String],
                    var location: Option[Seq[Double]] = None, in: Seq[WasteInput] = Seq.empty[WasteInput],
                    out: Seq[Output] = Seq.empty[Output], notes: Seq[Note] = Seq.empty[Note],
                    tag: Seq[String] = Seq.empty[String], owner: Option[String] = None, state: Option[String] = None, dm: Boolean = false,
                    voc: Double = 0, tsp: Double = 0, sox: Double = 0, nox: Double = 0, pollutant: Double = 0, grade: Option[String] = None) {
  def getSummary = {
    val content = s"電話：${phone}<br>" +
      s"地址：${addr}<br>"

    Summary(_id.name, content)
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
    sortBy:    String              = "tsp+")

  import WorkPoint.outputWrite
  import WorkPoint.outRead
  import WorkPoint.noteRead
  import WorkPoint.noteWrite
  implicit val wiRead = Json.reads[WasteInput]
  implicit val wiWrite = Json.writes[WasteInput]
  implicit val fcIdRead = Json.reads[FacilityID]
  implicit val fcRead = Json.reads[Facility]
  implicit val fcIdWrite = Json.writes[FacilityID]
  implicit val fcWrite = Json.writes[Facility]

  val codecRegistry = fromRegistries(
    fromProviders(classOf[Facility], classOf[FacilityID], classOf[Note], classOf[WasteInput], classOf[Output]), DEFAULT_CODEC_REGISTRY)

  val ColName = "facility"
  val collection = MongoDB.database.getCollection[Facility](ColName).withCodecRegistry(codecRegistry)

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      waitReadyResult(f)

      val cf0 = collection.createIndex(Indexes.ascending("_id.no")).toFuture()
      val cf1 = collection.createIndex(Indexes.ascending("_id.fcType")).toFuture()
      val cf2 = collection.createIndex(Indexes.ascending("_id.name")).toFuture()
      val cf3 = collection.createIndex(Indexes.geo2dsphere("location")).toFuture()
      val cf4 = collection.createIndex(
        Indexes.compoundIndex(Indexes.ascending("_id.fcType"), Indexes.geo2dsphere("location"))).toFuture()

      val cf5 = collection.createIndex(Indexes.ascending("tsp")).toFuture()
      val cf6 = collection.createIndex(Indexes.ascending("in.code")).toFuture()
      val cf7 = collection.createIndex(Indexes.ascending("grade")).toFuture()

      cf0.onFailure(errorHandler)
      cf1.onFailure(errorHandler)
      cf2.onFailure(errorHandler)
      cf3.onFailure(errorHandler)
      cf4.onFailure(errorHandler)
      cf5.onFailure(errorHandler)
      cf6.onFailure(errorHandler)
      cf7.onFailure(errorHandler)
    }

    for (imported <- SysConfig.get(SysConfig.ImportFacility)) {
      if (!imported.asBoolean().getValue) {
        if (importFacility) {
          SysConfig.set(SysConfig.ImportFacility, BsonBoolean(true))
        }
      }

      for (imported <- SysConfig.get(SysConfig.ImportFacilityPollutant)) {
        if (!imported.asBoolean().getValue) {
          if (importAirPollutant) {
            SysConfig.set(SysConfig.ImportFacilityPollutant, BsonBoolean(true))
          }
        }
      }
    }

    for (imported <- SysConfig.get(SysConfig.ImportProcessPlant1)) {
      if (!imported.asBoolean().getValue) {
        if (importProcessPlant1) {
          SysConfig.set(SysConfig.ImportProcessPlant1, BsonBoolean(true))
        }
      }
    }
    
    for (imported <- SysConfig.get(SysConfig.ImportProcessPlant2)) {
      if (!imported.asBoolean().getValue) {
        if (importProcessPlant2) {
          SysConfig.set(SysConfig.ImportProcessPlant2, BsonBoolean(true))
        }
      }
    }
    
    for (imported <- SysConfig.get(SysConfig.ImportProcessPlant3)) {
      if (!imported.asBoolean().getValue) {
        if (importProcessPlant3) {
          SysConfig.set(SysConfig.ImportProcessPlant3, BsonBoolean(true))
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
            val id = FacilityID(props.fac_no, props.fac_name, fcType)
            val location = if (fcObj.geometry.coordinates.length == 2)
              Some(fcObj.geometry.coordinates)
            else
              None
            Facility(id, props.fac_addr, None, location)
        }

        val writeModelSeq = facilities map { facility =>
          val filter = Filters.and(
            Filters.eq("_id", facility._id),
            Filters.gt("pollutant", 0))
          ReplaceOneModel(filter, facility, UpdateOptions().upsert(true))
        }

        val f = collection.bulkWrite(writeModelSeq, BulkWriteOptions().ordered(false)).toFuture()

        f.onFailure(errorHandler)
        waitReadyResult(f)
        Logger.info("facilities has been inserted.")
      })

    true
  }

  def importAirPollutant() = {
    import com.github.tototoshi.csv._
    val reader = CSVReader.open(new File(current.path.getAbsolutePath + "/import/air_pollutant.csv"))
    val recordList = reader.allWithHeaders()
    val updateModelList = recordList map { pollutant =>
      val updates = Updates.combine(
        Updates.set("voc", pollutant("VOCs").toDouble),
        Updates.set("tsp", pollutant("TSP").toDouble),
        Updates.set("sox", pollutant("SOx").toDouble),
        Updates.set("nox", pollutant("NOx").toDouble),
        Updates.set("pollutant", pollutant("Total(不含VOCs)").toDouble))
      UpdateOneModel(Filters.eq("_id.no", pollutant("FacilityID")), updates)
    }
    Logger.info(s"Updating ${updateModelList.length} pollutants")

    val f = collection.bulkWrite(updateModelList, BulkWriteOptions().ordered(false)).toFuture()
    f.onFailure(errorHandler)
    waitReadyResult(f)
    Logger.info("pollutants has been updated.")

    true
  }

  def importProcessPlant(fileName:String, grade:String) = {
    val elem = xml.XML.loadFile(new File(current.path.getAbsolutePath + "/import/" + fileName))
    var plantWasteInputMap = Map.empty[String, Seq[WasteInput]]
    elem match {
      case <EmsData>{ emsData @ _* }</EmsData> =>
        Logger.info(s"${emsData.length}")
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
        }
    }
    Logger.info(s"total ${plantWasteInputMap.size} plants")
    val updateModels = for ((plantNo, wiSeq) <- plantWasteInputMap) yield {
      val updates = Updates.combine(Updates.set("in", wiSeq), Updates.set("grade", grade))
      UpdateOneModel(Filters.eq("_id.no", plantNo), updates)
    }

    val f = collection.bulkWrite(updateModels.toList, BulkWriteOptions().ordered(false)).toFuture()
    waitReadyResult(f)
    Logger.info(s"${grade}級處理機構資訊已更新.")

    true
  }

  def importCleanPlant(fileName:String, grade:String) = {
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
      val updates = Updates.combine(Updates.set("in", wiSeq), Updates.set("grade", grade))
      UpdateOneModel(Filters.eq("_id.no", plantNo), updates)
    }

    val f = collection.bulkWrite(updateModels.toList, BulkWriteOptions().ordered(false)).toFuture()
    waitReadyResult(f)
    Logger.info(s"${grade}級處理機構資訊已更新.")

    true
  }

  def importProcessPlant1 = importProcessPlant("甲級處理機構.xml", "甲")
  def importProcessPlant2 = importProcessPlant("乙級處理機構.xml", "乙")
  def importProcessPlant3 = importCleanPlant("丙級處理機構.xml", "丙")
  
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

  def getNorthOwnerless(param: QueryParam) = query(northOwnerless(param))(getSortBy(param)) _
  def getNorthOwnerlessCount(param: QueryParam) = count(northOwnerless(param))
  def getSouthOwnerless(param: QueryParam) = query(southOwnerless(param))(getSortBy(param)) _
  def getSouthOwnerlessCount(param: QueryParam) = count(southOwnerless(param))

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

  def populateSummary(fcIdList: Seq[FacilityID]) = {
  }
}