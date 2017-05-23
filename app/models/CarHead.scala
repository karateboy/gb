package models
import com.github.nscala_time.time.Imports._
import org.mongodb.scala.bson._
import models.ModelHelper._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext.Implicits.global

case class CarHead(_id: String, company: String, weight: Double, var loc: GeoPoint, freeFrom: Long) {
  def toVehicle = Vehicle(_id, Vehicle.carHead, company, loc, new DateTime(freeFrom))
  def toDocument = {
    Document("_id" -> _id, "company" -> company, "weight" -> weight, "loc" -> loc.toGeoJSON, "freeFrom" -> freeFrom)
  }
}

object CarHead {
  import GeoJSON._

  val ColName = "carHead"
  val collection = MongoDB.database.getCollection(ColName)
  implicit val truckRead = Json.reads[CarHead]
  implicit val truckWrite = Json.writes[CarHead]

  def toCarHead(implicit doc: Document) = {
    val _id = doc.getString("_id")
    val company = doc.getString("company")
    val weight = doc.getDouble("weight")
    val freeFrom = doc.getLong("freeFrom")
    val loc = GeoJSON.toGeoJSON(doc("loc").asDocument()).toGeoPoint

    CarHead(_id = _id, company = company, weight = weight, loc = loc, freeFrom = freeFrom)
  }
  
  def init(colNames: Seq[String]) {
    import org.mongodb.scala.model.Indexes
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      f.onSuccess({
        case _: Seq[_] =>
          val f1 = collection.createIndex(Indexes.ascending("company")).toFuture()
          val f2 = collection.createIndex(Indexes.geo2dsphere("loc")).toFuture()
          val f3 = collection.createIndex(Indexes.ascending("weight")).toFuture()
          f1.onFailure(errorHandler)
          f2.onFailure(errorHandler)
          f3.onFailure(errorHandler)
      })
    }
  }
}