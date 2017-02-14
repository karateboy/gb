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

case class Identity(_id: String, seq: Int)

object Identity {
  import scala.concurrent._
  import scala.concurrent.duration._

  val ColName = "identity"
  val collection = MongoDB.database.getCollection(ColName)
  implicit val userRead = Json.reads[Identity]
  implicit val userWrite = Json.writes[Identity]

  def toDocument(id: Identity) = Document(Json.toJson(id).toString())

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
    }
    val f = collection.count().toFuture()
    f.onSuccess({
      case count: Seq[Long] =>
        if (count(0) == 0) {
          val id1 = Identity("vehicle", 1)
          newID(id1)
        }
    })
    f.onFailure(errorHandler)

  }

  def toIdentity(doc: Document) = {
    val ret = Json.parse(doc.toJson()).validate[Identity]

    ret.fold(error => {
      Logger.error(JsError.toJson(error).toString())
      throw new Exception(JsError.toJson(error).toString)
    },
      id => id)
  }

  def newID(id: Identity) = {
    collection.insertOne(toDocument(id)).toFuture()
  }

  def getNewID(name: String) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._

    val f = collection.findOneAndUpdate(equal("_id", name), Updates.inc("seq", 1)).toFuture()
    for (id <- f)
      yield toIdentity(id(0))
  }
}
