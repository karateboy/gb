package models
import play.api._
import play.api.libs.json._
import models.ModelHelper._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

import java.io._
import java.nio.file.Files
import java.nio.file._
import java.util.Date

import org.mongodb.scala.model._
import org.mongodb.scala.bson._
import MongoDB._

case class LatLng(lat: Double, lng: Double)
case class Note(date: Date, comment: String, person: String)
case class Summary(title: String, content: String)

abstract class IWorkPointID() {
  val wpType: Int
}

abstract class IWorkPoint() {
  def location: Option[Seq[Double]]
  val notes: Seq[Note]
  val owner: Option[String]
  val state: Option[String]
  val dm: Boolean
}

case class DM(company: Option[String], contact: Option[String], addr: String)
case class WorkPoint(
  _id:      Document,
  location: Option[Seq[Double]], 
  notes: Seq[Note], owner: Option[String], state: Option[String], var summary: Option[Summary], dm: Boolean) extends IWorkPoint

case class WorkPointType(_id: Int, typeID: String, name: String)
object WorkPointType extends Enumeration {
  val BuildCase = Value(1)
  val CareHouse = Value(2)
  val DumpSite = Value(3)
  val Tank = Value(4)
  val GasStation = Value(5)
  val Facility = Value(6)

  val map = Map(
    BuildCase -> "起造人",
    CareHouse -> "長照機構",
    DumpSite -> "棄置場",
    Tank -> "油槽",
    GasStation -> "加油站",
    Facility -> "有槽事業")

  implicit val write = Json.writes[WorkPointType]

  implicit val tReads: Reads[WorkPointType.Value] = EnumUtils.enumReads(WorkPointType)
  implicit val tWrites: Writes[WorkPointType.Value] = EnumUtils.enumFormat(WorkPointType)

  val workList = List(BuildCase, CareHouse, Tank)

  def getList =
    for (key <- values.toSeq) yield WorkPointType(key.id, key.toString(), map(key))

  def getWorkList =
    for (key <- workList) yield WorkPointType(key.id, key.toString(), map(key))
}

object WorkPoint {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }
  import org.mongodb.scala.bson.conversions._

  val ColName = "workPoint"

  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val codecRegistry = fromRegistries(fromProviders(
    classOf[WorkPoint],
    classOf[Note], classOf[Summary]), DEFAULT_CODEC_REGISTRY)

  private val collection = MongoDB.database.getCollection[WorkPoint](WorkPoint.ColName).withCodecRegistry(codecRegistry)

  implicit val documentWrites: Writes[Document] = new Writes[Document] {
    def writes(v: Document): JsValue = Json.parse(v.toJson())
  }

  implicit val summaryWrite = Json.writes[Summary]
  implicit val latlngRead = Json.reads[LatLng]
  implicit val noteWrite = Json.writes[Note]
  implicit val wpWrite = Json.writes[WorkPoint]
  implicit val summaryRead = Json.reads[Summary]
  implicit val noteRead = Json.reads[Note]
  //implicit val wpRead = Json.reads[WorkPoint]

  def wpFilter(wpType: Int)(bsons: Bson*): Bson = {
    val seq = bsons.+:(Filters.eq("_id.wpType", wpType))
    Filters.and(seq: _*)
  }

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      waitReadyResult(f)

      val cf1 = collection.createIndex(Indexes.ascending("_id.wpType")).toFuture()
      val cf2 = collection.createIndex(Indexes.geo2dsphere("location")).toFuture()
      val cf3 = collection.createIndex(
        Indexes.compoundIndex(Indexes.ascending("_id.wpType"), Indexes.geo2dsphere("location"))).toFuture()

      cf1.onFailure(errorHandler)
      cf2.onFailure(errorHandler)
      cf3.onFailure(errorHandler)
    }
    
    if (!waitReadyResult(SysConfig.get(SysConfig.UnsetWorkPointIO)).asBoolean().getValue) {
      if (unsetIO) {
        SysConfig.set(SysConfig.UnsetWorkPointIO, BsonBoolean(true))
      }
    }
  }
  
  def unsetIO = {
    val updates = Updates.combine(Updates.unset("in"), Updates.unset("out"))
    val f = collection.updateMany(Filters.exists("_id"), updates).toFuture()
    f.onFailure(errorHandler)
    true
  }

  def getList() = {
    val f = collection.find().toFuture()
    f.onFailure(errorHandler)
    f
  }

  def getAreaList(typeIdList: Seq[Int], bottomLeft: LatLng, upperRight: LatLng) = {

    val filter1 = Filters.in("_id.wpType", typeIdList: _*)
    val filter2 = Filters.geoWithinBox("location", bottomLeft.lng, bottomLeft.lat, upperRight.lng, upperRight.lat)
    val filter = Filters.and(filter1, filter2)

    val f = collection.find(filter).toFuture()
    f.onFailure(errorHandler)
    for {
      wp <- f
      bcWP <- BuildCase2.populateSummary(wp)
      chWP <- CareHouse.populateSummary(wp)
      dsWP <- DumpSite.populateSummary(wp)
      tankWP <- Tank.populateSummary(wp)
      gasWP <- GasStation.populateSummary(wp)
    } yield bcWP ++ chWP ++ dsWP ++ tankWP ++ gasWP
  }

}