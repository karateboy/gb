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

case class Note(date: Date, comment: String, person: String)
case class Input(name: String, code: Option[String], freq: Option[String], volume: Double)
case class Output(name: String, code: Option[String], freq: Option[String], volume: Double)

abstract class IWorkPointID() {
  val wpType: Int
}

abstract class IWorkPoint() {
  def location: Option[Seq[Double]]
  val in: Seq[Input]
  val out: Seq[Output]
  val notes: Seq[Note]
  val owner: Option[String]
}

case class WorkPoint(_id: Document,
                     location: Option[Seq[Double]], in: Seq[Input], out: Seq[Output],
                     notes: Seq[Note], tag: Seq[String], owner: Option[String]) extends IWorkPoint

object WorkPointType extends Enumeration {
  val BuildCase = Value(1)
  val CareHouse = Value(2)
  val DumpSite = Value(3)

  val map = Map(
    BuildCase -> "起造人",
    CareHouse -> "機構",
    DumpSite -> "棄置場")

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

  val codecRegistry = fromRegistries(fromProviders(classOf[WorkPoint],
    classOf[Note], classOf[Input], classOf[Output]), DEFAULT_CODEC_REGISTRY)

  val collection = MongoDB.database.getCollection[WorkPoint](WorkPoint.ColName).withCodecRegistry(codecRegistry)

  implicit val documentWrites: Writes[Document] = new Writes[Document] {
    def writes(v: Document): JsValue = Json.parse(v.toJson())
  }

  implicit val outputWrite = Json.writes[Output]
  implicit val inputWrite = Json.writes[Input]
  implicit val noteWrite = Json.writes[Note]
  implicit val wpWrite = Json.writes[WorkPoint]
  implicit val inRead = Json.reads[Input]
  implicit val outRead = Json.reads[Output]
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
  }

  def getList() = {
    val f = collection.find().toFuture()
    f.onFailure(errorHandler)
    f
  }

}