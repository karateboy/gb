package models
import play.api._
import models.ModelHelper._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import play.api.Play.current
import com.github.nscala_time.time.Imports._
import java.io._
import java.nio.file.Files
import java.nio.file._
import java.util.Date
import org.mongodb.scala.model._
import org.mongodb.scala.model.Indexes._
import org.mongodb.scala.bson._
import MongoDB._
import scala.util._

case class Contractor(_id: String, addr: Option[String], phone: Option[String], contact: Option[String]){
  def getDmOpt = {
    if(addr.isEmpty || addr.get.length() == 0)
      None
    else
      Some(DM(Some(_id), contact, addr.get))
  }
}

object Contractor {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val codecRegistry = fromRegistries(fromProviders(classOf[Contractor]), DEFAULT_CODEC_REGISTRY)

  val ColName = "contractor"
  val collection = MongoDB.database.getCollection[Contractor](ColName).withCodecRegistry(codecRegistry)

  implicit val bdWrite = Json.writes[Contractor]
  implicit val bdRead = Json.reads[Contractor]

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      waitReadyResult(f)      
    }
  }

  def get(_id: String) = collection.find(Filters.eq("_id", _id)).headOption()

  def upsert(contractor: Contractor) = {
    val f = collection.replaceOne(Filters.eq("_id", contractor._id), contractor, UpdateOptions().upsert(true)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  import scala.concurrent._



  def getMap() = {
    val f = collection.find().toFuture()
    f.onFailure(errorHandler)

    for (builderList <- f) yield {
      val pairs =
        builderList map {
          bd =>
            bd._id -> bd
        }
      Map(pairs: _*)
    }
  }
  
  def getList = {
    val f = collection.find().toFuture()
    f.onFailure(errorHandler)
    f
  }
  
}