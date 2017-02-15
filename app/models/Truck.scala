package models
import org.mongodb.scala.bson._
import models.ModelHelper._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext.Implicits.global

case class Truck(_id: String, company: String, weight: Double, bucketType: String,
                 clipper: Boolean, lifter: Boolean, highBucket: Boolean, freezer: Boolean, var loc: GeoPoint, online: Boolean) {
  def toDocument = {
    Document("_id" -> _id, "company" -> company, "weight" -> weight, "bucketType" -> bucketType,
      "clipper" -> clipper, "lifter" -> lifter, "highBucket" -> highBucket, "freezer" -> freezer,
      "loc" -> loc.toGeoJSON, "online" -> online)
  }
}
object Truck {
  import scala.concurrent._
  import scala.concurrent.duration._
  import GeoJSON._

  val ColName = "trucks"
  val collection = MongoDB.database.getCollection(ColName)
  implicit val geoPointRead = Json.reads[GeoPoint]
  implicit val geoPointWrite = Json.writes[GeoPoint]
  implicit val truckRead = Json.reads[Truck]
  implicit val truckWrite = Json.writes[Truck]

  def toTruck(implicit doc: Document) = {
    val _id = doc.getString("_id")
    val company = doc.getString("company")
    val weight = doc.getDouble("weight")
    val bucketType = doc.getString("bucketType")
    val clipper = doc.getBoolean("clipper")
    val lifter = doc.getBoolean("lifter")
    val highBucket = doc.getBoolean("highBucket")
    val freezer = doc.getBoolean("freezer")
    val loc = GeoJSON.toGeoJSON(doc("loc").asDocument()).toGeoPoint
    val online = doc.getBoolean("online")

    Truck(_id = _id, company = company, weight = weight, bucketType = bucketType, clipper = clipper, lifter = lifter,
      highBucket = highBucket, freezer = freezer, loc = loc, online = online)
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
          val f3 = collection.createIndex(Indexes.ascending("online", "weight", "bucketType", "clipper", "lifter", "highBucket", "freezer")).toFuture()
          f1.onFailure(errorHandler)
          f2.onFailure(errorHandler)
          f3.onFailure(errorHandler)
      })
    }
  }

}