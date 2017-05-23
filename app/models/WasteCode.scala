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

case class WasteCode(_id: String, level: String, desc: String)

object WasteCode {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }
    
  val codecRegistry = fromRegistries(fromProviders(classOf[WasteCode]), DEFAULT_CODEC_REGISTRY)
    
  val ColName = "wasteCode"
  val collection = MongoDB.database.getCollection[WasteCode](ColName)

  import org.mongodb.scala.model.Indexes._
  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      f.onSuccess({
        case x =>
          val cf1 = collection.createIndex(ascending("desc")).toFuture()
          val cf2 = collection.createIndex(ascending("level")).toFuture()
          cf1.onFailure(errorHandler)
          cf2.onFailure(errorHandler)
          
          import scala.concurrent._
          val endF = Future.sequence(Seq(cf1, cf2))
          endF.onComplete({
            case x =>
              Logger.debug("ImportFromSQL")
              importFromSQL
          })
      })
    }
  }

  def importFromSQL = {
    import scalikejdbc._
    val list =
      DB localTx { implicit session =>
        sql"""
        Select * 
        From WasteCodeNum
        """.map(rs => WasteCode(rs.string("WasteNum"), rs.string("WasteNum").substring(0, 2), rs.string("WasteName"))).list().apply()
      }

    Logger.debug(s"# of wasteNum = ${list.length}")
    val f = collection.insertMany(list).toFuture()
    f.onFailure(errorHandler)
    f.onSuccess({
      case x =>
        Logger.info("Import WasteCode complete!")  
    })
  }
}