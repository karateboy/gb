package models
import play.api._
import com.github.nscala_time.time.Imports._
import models.ModelHelper._
import models._
import org.mongodb.scala.bson.Document
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

case class Identity(_id: String, seq: Long)

object Identity {
  import scala.concurrent._
  import scala.concurrent.duration._
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val codecRegistry = fromRegistries(fromProviders(classOf[Identity]), DEFAULT_CODEC_REGISTRY)

  val Vehicle = "vehicle"
  val Order = "order"
  val Customer = "customer"

  val ColName = "identity"
  val collection = MongoDB.database.getCollection[Identity](ColName).withCodecRegistry(codecRegistry)
  implicit val userRead = Json.reads[Identity]
  implicit val userWrite = Json.writes[Identity]

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
    }
    val f = collection.count().toFuture()
    f.onSuccess({
      case count =>
        if (count == 0) {
          val id1 = Identity(Vehicle, 1)
          val id2 = Identity(Order, 1)
          val id3 = Identity(Customer, 1)
          newID(id1)
          newID(id2)
          newID(id3)
        }
    })
    f.onFailure(errorHandler)

  }

  def newID(id: Identity) = {
    collection.insertOne(id).toFuture()
  }

  def getNewID(name: String) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._

    val f = collection.findOneAndUpdate(equal("_id", name), Updates.inc("seq", 1)).toFuture()
    for (id <- f)
      yield id
  }
}
