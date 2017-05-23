package models
import org.mongodb.scala.bson._
import models.ModelHelper._
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class GeoPoint(longitude: Double, latitude: Double) {
  def toGeoJSON = GeoJSON("Point", Seq(longitude, latitude))
}

case class GeoJSON(geoType: String, coordinates: Seq[Double]) {
  def toDocument = {
    Document("type" -> geoType, "coordinates" -> coordinates)
  }
  def toGeoPoint = {
    assert(geoType == "Point")
    GeoPoint(coordinates(0), coordinates(1))
  }
}

object GeoJSON {
  implicit val geoPointRead = Json.reads[GeoPoint]
  implicit val geoPointWrite = Json.writes[GeoPoint]

  implicit object TransformGeoJSON extends BsonTransformer[GeoJSON] {
    def apply(gj: GeoJSON): BsonDocument = gj.toDocument.toBsonDocument
  }

  def toGeoJSON(implicit doc: Document) = {
    import org.bson.json._
    val geoType = doc.getString("type")
    val coordinates = getArray("coordinates", v => v.asNumber().doubleValue())
    GeoJSON(geoType, coordinates)
  }
}