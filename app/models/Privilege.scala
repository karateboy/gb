package models
import play.api._
import play.api.mvc._
import play.api.Logger
import models.ModelHelper._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.language.implicitConversions

case class MenuRight(id: MenuRight.Value, desp: String)
object MenuRight extends Enumeration {
  implicit val mReads: Reads[MenuRight.Value] = EnumUtils.enumReads(MenuRight)
  implicit val mWrites: Writes[MenuRight.Value] = EnumUtils.enumWrites
  implicit val mrWrites = Json.writes[MenuRight]

  val RealtimeInfo = Value("RealtimeInfo")
  val DataQuery = Value("DataQuery")
  val Report = Value("Report")
  val SystemManagement = Value("SystemManagement")

  val map = Map(
    RealtimeInfo -> "即時資訊",
    DataQuery -> "數據查詢",
    Report -> "報表查詢",
    SystemManagement -> "系統管理")
  def getDisplayName(v: MenuRight.Value) = {
    map(v)
  }
}

case class Privilege(allowedMenuRights: Seq[MenuRight.Value])

import scala.concurrent.ExecutionContext.Implicits.global
object Privilege {
  implicit val privilegeWrite = Json.writes[Privilege]
  implicit val privilegeRead = Json.reads[Privilege]

  lazy val defaultPrivilege = Privilege(MenuRight.values.toSeq)
  val emptyPrivilege = Privilege(Seq.empty[MenuRight.Value])
}