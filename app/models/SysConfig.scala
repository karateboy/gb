package models
import play.api.libs.json._
import models.ModelHelper._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import org.mongodb.scala.model._
import org.mongodb.scala.bson._

object SysConfig extends Enumeration {
  val ColName = "sysConfig"
  val collection = MongoDB.database.getCollection(ColName)

  val ImportDumpSite = Value

  val defaultConfig = Map(
    ImportDumpSite -> Document("value" -> false))

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
    }

    val f = collection.count().toFuture()
    f.onSuccess({
      case count: Long =>
        if (count != defaultConfig.size) {
          val docs = defaultConfig map {
            kv =>
              kv._2 + ("_id" -> kv._1.toString)
          }

          val f = collection.insertMany(docs.toList, new InsertManyOptions().ordered(false)).toFuture()
          f.onFailure(errorHandler)
          waitReadyResult(f)
        }
    })
    f.onFailure(errorHandler)
    waitReadyResult(f)
  }

  def upsert(_id: SysConfig.Value, doc: Document) = {
    val uo = new UpdateOptions().upsert(true)
    val f = collection.replaceOne(Filters.equal("_id", _id.toString()), doc, uo).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def get(_id: SysConfig.Value) = {
    val f = collection.find(Filters.eq("_id", _id.toString())).headOption()
    f.onFailure(errorHandler)
    for (ret <- f) yield {
      val doc = ret.getOrElse(defaultConfig(_id))
      doc("value")
    }
  }

  def set(_id: SysConfig.Value, v: BsonValue) = upsert(_id, Document("value" -> v))  
}