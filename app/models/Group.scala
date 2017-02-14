package models

case class GroupInfo(id:String, name:String)
object Group extends Enumeration {
  val Admin = Value
  val Sales = Value
  val PM = Value
  val Tuexture = Value
  val Weaving = Value
  val WhiteSock = Value
  val Dyeing = Value
  val Styling = Value
  val Tidy = Value
  val Packaging = Value

  val map = Map(
    Admin -> "系統管理員",
    Sales -> "業務",
    PM -> "生管部",
    Dyeing -> "漂染部",
    Styling -> "定型部",
    Tidy -> "整理部",
    Packaging -> "裝箱出貨部")
    
  def getInfoList = map.map {m => GroupInfo(m._1.toString, m._2)}.toList  
}