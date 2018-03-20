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

  val valueKey = "value"
  val ImportDumpSite = Value
  val ImportCareHouse = Value
  val ImportTank = Value
  val ImportGasStation = Value
  val ImportFacility = Value
  val ImportFacilityPollutant = Value
  val ImportProcessPlant1 = Value
  val ImportProcessPlant2 = Value
  val ImportProcessPlant3 = Value
  val GrabWasteInfo = Value
  val GrabFactoryInfo = Value
  val GrabProcessPlantInfo = Value
  val ExportFactorySheet = Value
  val TrimArchitect = Value

  val defaultConfig = Map(
    ImportDumpSite -> Document(valueKey -> false),
    ImportCareHouse -> Document(valueKey -> false),
    ImportTank -> Document(valueKey -> false),
    ImportGasStation -> Document(valueKey -> false),
    ImportFacility -> Document(valueKey -> false),
    ImportFacilityPollutant -> Document(valueKey -> false),
    ImportProcessPlant1 -> Document(valueKey -> false),
    ImportProcessPlant2 -> Document(valueKey -> false),
    ImportProcessPlant3 -> Document(valueKey -> false),
    GrabWasteInfo -> Document(valueKey -> false),
    GrabFactoryInfo -> Document(valueKey -> false),
    GrabProcessPlantInfo -> Document(valueKey -> false),
    ExportFactorySheet -> Document(valueKey -> false),
    TrimArchitect -> Document(valueKey -> false))

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
          import scala.concurrent.duration._
          scala.concurrent.Await.ready(f, Duration.Inf)
        }
    })

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

  def set(_id: SysConfig.Value, v: BsonValue) = upsert(_id, Document(valueKey -> v))
}