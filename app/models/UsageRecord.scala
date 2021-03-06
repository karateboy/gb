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

case class UsageRecordID(name: String, month: Date)
case class UsageRecord(_id: UsageRecordID, buildCase: Seq[BuildCaseID], builder: Seq[String])
object UsageRecord {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val codecRegistry = fromRegistries(
    fromProviders(classOf[UsageRecord], classOf[UsageRecordID], classOf[BuildCaseID]), DEFAULT_CODEC_REGISTRY)

  val ColName = "usageRecord"
  val collection = MongoDB.database.getCollection[UsageRecord](ColName).withCodecRegistry(codecRegistry)

  implicit val chWrite = Json.writes[CareHouseID]
  implicit val bcdWrite = Json.writes[BuildCaseID]
  implicit val irdWrite = Json.writes[UsageRecordID]
  implicit val irWrite = Json.writes[UsageRecord]

  implicit val chRead = Json.reads[CareHouseID]
  implicit val bcdRead = Json.reads[BuildCaseID]
  implicit val irdRead = Json.reads[UsageRecordID]
  implicit val irRead = Json.reads[UsageRecord]

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      val cif = collection.createIndex(Indexes.ascending("_id.name", "_id.month"), IndexOptions().unique(true)).toFuture()
      cif.onFailure(errorHandler)
    }
  }

  def emptyRecord(name: String, offset: Int) =
    UsageRecord(getUsageRecordID(name, offset), Seq.empty[BuildCaseID], Seq.empty[String])

  def getUsageRecordID(name: String, offset: Int = 0) = {
    val month = DateTime.now().withDayOfMonth(5).withMillisOfDay(0) + offset.month
    UsageRecordID(name, month.toDate())
  }

  def addBuildCaseUsage(name: String, bcID: BuildCaseID) = {
    val _id = getUsageRecordID(name)
    val f = collection.updateOne(Filters.eq("_id", _id),
      Updates.combine(Updates.setOnInsert("_id", _id),
        Updates.setOnInsert("builder", Seq.empty[String]),
        Updates.addToSet("buildCase", bcID)), UpdateOptions().upsert(true)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def addBuilderUsage(name: String, builderID: String) = {
    val _id = getUsageRecordID(name)
    val f = collection.updateOne(Filters.eq("_id", _id),
      Updates.combine(Updates.setOnInsert("_id", _id),
        Updates.setOnInsert("buildCase", Seq.empty[BuildCaseID]),
        Updates.addToSet("builder", builderID)), UpdateOptions().upsert(true)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def getRecord(name: String, offset: Int) = {
    val _id = getUsageRecordID(name, offset)
    val f = collection.find(Filters.eq("_id", _id)).toFuture()
    f.onFailure(errorHandler)
    f
  }
}