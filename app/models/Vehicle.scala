package models
case class Vehicle(_id:String, vehicleType:String, frontHeightCm:Option[Int], backHeightCm:Option[Int],
    length:Option[Int], Width:Option[Int], height:Option[Int], meters:Double, tons:Double,
    bucketType:String)
object Vehicle {
  
}