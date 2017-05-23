package models
import com.github.nscala_time.time.Imports._
/*
case class Vehicle(_id:String, vehicleType:String, frontHeightCm:Option[Int], backHeightCm:Option[Int],
    length:Option[Int], Width:Option[Int], height:Option[Int], meters:Double, tons:Double,
    bucketType:String)
*/

case class Vehicle(_id: String, vehicleType:Vehicle.Value, company: String, var loc: GeoPoint, freeFrom: DateTime){
  
}

object Vehicle extends Enumeration{
  val truck = Value
  val carHead = Value
  val carTail = Value  
}