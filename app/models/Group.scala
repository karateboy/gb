package models

case class GroupInfo(id:String, name:String)
object Group extends Enumeration {
  val Admin = Value
  val Sales = Value
  val Driver = Value

  val map = Map(
    Admin -> "系統管理員",
    Sales -> "業務",
    Driver -> "司機"
)
    
  def getInfoList = map.map {m => GroupInfo(m._1.toString, m._2)}.toList  
}