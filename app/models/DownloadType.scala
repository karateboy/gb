package models
import play.api._
import play.api.libs.json._
import models.ModelHelper._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

case class DownloadType(id: String, desc: String)
object DownloadType extends Enumeration {
  val Contractor = Value
  val map = Map(
    Contractor -> "承造人")

  implicit val write = Json.writes[DownloadType]

  implicit val tReads: Reads[DownloadType.Value] = EnumUtils.enumReads(DownloadType)
  implicit val tWrites: Writes[DownloadType.Value] = EnumUtils.enumFormat(WorkPointType)

  def getList =
    for (key <- values.toSeq) yield DownloadType(key.toString(), map(key))

}