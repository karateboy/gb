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
  val ImportFacilityPollutant = Value
  val ImportProcessPlant1 = Value
  val ImportProcessPlant2 = Value
  val GrabWasteInfo = Value
  val GrabFactoryInfo = Value
  val GrabProcessPlantInfo = Value
  val ExportFactorySheet = Value
  val ImportRecyclePlant = Value
  val ExtractFacilityCounty = Value
  val UnsetWorkPointIO = Value
  val ImportBuildCase = Value

  val defaultConfig = Map(
    ImportDumpSite -> Document(valueKey -> false),
    ImportCareHouse -> Document(valueKey -> false),
    ImportTank -> Document(valueKey -> false),
    ImportGasStation -> Document(valueKey -> false),
    ImportFacilityPollutant -> Document(valueKey -> false),
    ImportProcessPlant1 -> Document(valueKey -> false),
    ImportProcessPlant2 -> Document(valueKey -> false),
    GrabWasteInfo -> Document(valueKey -> false),
    GrabFactoryInfo -> Document(valueKey -> false),
    GrabProcessPlantInfo -> Document(valueKey -> false),
    ExportFactorySheet -> Document(valueKey -> false),
    ImportRecyclePlant -> Document(valueKey -> false),
    ExtractFacilityCounty -> Document(valueKey -> false),
    UnsetWorkPointIO -> Document(valueKey -> false),
    ImportBuildCase -> Document(valueKey -> true))

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
    }

    val idSet = values map { _.toString() }
    //Clean up unused
    val f1 = collection.deleteMany(Filters.not(Filters.in("_id", idSet.toList: _*))).toFuture()
    f1.onFailure(errorHandler)
    val updateModels =
      for ((k, defaultDoc) <- defaultConfig) yield {
        UpdateOneModel(
          Filters.eq("_id", k.toString()),
          Updates.setOnInsert(valueKey, defaultDoc(valueKey)), UpdateOptions().upsert(true))
      }

    val f2 = collection.bulkWrite(updateModels.toList, BulkWriteOptions().ordered(false)).toFuture()

    import scala.concurrent._
    val f = Future.sequence(List(f1, f2))
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