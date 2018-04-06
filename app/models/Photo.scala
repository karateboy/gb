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

case class Photo(_id: ObjectId, image: Array[Byte])

object Photo {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }
  import org.mongodb.scala.bson.conversions._

  val ColName = "photo"

  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val codecRegistry = fromRegistries(fromProviders(classOf[Photo]), DEFAULT_CODEC_REGISTRY)

  private val collection = MongoDB.database.getCollection[Photo](ColName).withCodecRegistry(codecRegistry)

  implicit val documentWrites: Writes[Document] = new Writes[Document] {
    def writes(v: Document): JsValue = Json.parse(v.toJson())
  }

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      waitReadyResult(f)
    }

  }

  val noPhotoID = "000000000000000000000000"
  def getPhoto(objID: ObjectId) = {
    val f = collection.find(Filters.eq("_id", objID)).first().toFuture()
    f.onFailure(errorHandler)
    f
  }

  def updatePhoto(photo: Photo) = {
    val f = collection.updateOne(Filters.eq("_id", photo._id), Updates.set("image", photo.image)).toFuture()
    f.onFailure(errorHandler)
    f
  }
  
  def insert(photo: Photo) = {
    val f = collection.insertOne(photo).toFuture()
    f.onFailure(errorHandler)
    f
  }
  
}