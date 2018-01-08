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
  val _id: IWorkPointID
  val wpType: Int
  def location: Option[Seq[Double]]
  val in: Seq[Input]
  val out: Seq[Output]
}

case class WorkPointID(wpType: Int) extends IWorkPointID
case class WorkPoint(_id: IWorkPointID, wpType: Int, 
    location: Option[Seq[Double]], in: Seq[Input], out: Seq[Output]) extends IWorkPoint
    
object WorkPoint {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val ColName = "workPoint"

  val BuildCase = 1
  val CareHouse = 2
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val codecRegistry = fromRegistries(fromProviders(classOf[WorkPoint], classOf[WorkPointID]), DEFAULT_CODEC_REGISTRY)

  val collection = MongoDB.database.getCollection[WorkPoint](WorkPoint.ColName).withCodecRegistry(codecRegistry)

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      waitReadyResult(f)

      val cf1 = collection.createIndex(Indexes.ascending("wpType")).toFuture()
      val cf2 = collection.createIndex(Indexes.geo2dsphere("location")).toFuture()
      val cf3 = collection.createIndex(
          Indexes.compoundIndex(Indexes.ascending("wpType"), Indexes.geo2dsphere("location"))).toFuture()

      cf1.onFailure(errorHandler)
      cf2.onFailure(errorHandler)
      cf3.onFailure(errorHandler)
    }
  }

}