package models

import models._
import models.ModelHelper._
import org.mongodb.scala.bson.{ BsonArray, BsonDocument, Document }
import org.mongodb.scala.model.Indexes.ascending
import play.api.Logger
import play.api.libs.json.{ JsError, Json }
import org.mongodb.scala.model.geojson.Point

import scala.concurrent.ExecutionContext.Implicits.global
import com.github.nscala_time.time.Imports._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Created by user on 2017/1/1.
 */

case class OrderDetail(wasteCode: String, unit: String, quantity: Int, complete: Boolean)
case class Contract(salesId: String, customerId: Long, deliverDate: Long, details: Seq[OrderDetail], amount: Long)
case class Order(_id: Long, contact: String, address: String, phone: String, notifiedDate: Long, contacted: Boolean,
                 contract: Option[Contract], active: Boolean) {
  def newOrderID = {
    val newIDF = Identity.getNewID(Identity.Order)
    for (newID <- newIDF)
      yield Order(newID.seq, contact, address, phone, DateTime.now.getMillis, false, None, true)
  }
}

object Order {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val codecRegistry = fromRegistries(fromProviders(classOf[Order], classOf[OrderDetail], classOf[Contract]), DEFAULT_CODEC_REGISTRY)

  val colName = "orders"
  val collection = MongoDB.database.getCollection[Order](colName).withCodecRegistry(codecRegistry)

  implicit val odWrite = Json.writes[OrderDetail]
  implicit val odRead = Json.reads[OrderDetail]
  implicit val cWrite = Json.writes[Contract]
  implicit val cRead = Json.reads[Contract]
  implicit val orderWrite = Json.writes[Order]
  implicit val orderRead = Json.reads[Order]

  def init(colNames: Seq[String]) = {
    if (!colNames.contains(colName)) {
      val f = MongoDB.database.createCollection(colName).toFuture()
      f.onFailure(errorHandler)
      f.onSuccess({
        case _: Seq[t] =>

          val cf3 = collection.createIndex(ascending("active")).toFuture()

          cf3.onFailure(errorHandler)
      })
      Some(f.mapTo[Unit])
    } else
      None
  }

  def listActiveOrder() = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._
    val f = collection.find(equal("active", true)).sort(Sorts.ascending("_id")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records
  }

  import org.mongodb.scala.model.Filters._

  def insertOrder(order: Order) = {
    import org.mongodb.scala.model.UpdateOptions
    import org.mongodb.scala.bson.BsonString

    val f = collection.insertOne(order).toFuture()
    f.onFailure({
      case ex: Exception => Logger.error(ex.getMessage, ex)
    })
    f
  }

  def upsertOrder(order: Order) = {
    import org.mongodb.scala.model.UpdateOptions
    import org.mongodb.scala.bson.BsonString

    val f = collection.replaceOne(equal("_id", order._id), order, UpdateOptions().upsert(true)).toFuture()
    f.onFailure({
      case ex: Exception => Logger.error(ex.getMessage, ex)
    })
    f
  }

  def getOrder(orderId: Long) = {
    val f = collection.find(equal("_id", orderId)).toFuture()
    f.onFailure {
      errorHandler
    }
    for (orders <- f) yield {
      if (orders.isEmpty)
        None
      else
        Some(orders(0))
    }
  }

  def getOrders(orderIds: Seq[String]) = {
    val f = collection.find(in("_id", orderIds: _*)).toFuture()
    f.onFailure {
      errorHandler
    }
    for (orders <- f) yield {
      orders
    }
  }
  def findOrders(orderIdList: Seq[String]) = {
    import org.mongodb.scala.model._
    val f = collection.find(in("_id", orderIdList: _*)).sort(Sorts.ascending("_id")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records
  }

  def unhandledOrder() = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._
    val f = collection.find(and(equal("active", true), equal("contacted", false)))
      .sort(Sorts.ascending("notifiedDate")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records
  }

  def myActiveOrder(salesId: String) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._
    val f = collection.find(and(equal("active", true), equal("salesId", salesId)))
      .sort(Sorts.ascending("_id")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records
  }

  def getHistoryOrder(begin: Long, end: Long) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._
    val f = collection.find(and(gte("date", begin), lt("date", end))).sort(Sorts.ascending("_id")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records
  }

  def addOrderDetailWorkID(orderId: String, index: Int, workCardID: String) = {
    import org.mongodb.scala.bson._
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model.Updates._

    val fieldName = "details." + index + ".workCardIDs"
    val col = MongoDB.database.getCollection(colName)
    val f = col.updateOne(and(equal("_id", orderId)), addToSet(fieldName, workCardID)).toFuture()
    f.onFailure({
      case ex: Exception => Logger.error(ex.getMessage, ex)
    })
    f
  }

  def setOrderDetailComplete(orderId: Long, index: Int, complete: Boolean) = {
    import org.mongodb.scala.bson._
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model.Updates._

    val fieldName = "details." + index + ".complete"
    val col = MongoDB.database.getCollection(colName)
    val f = col.updateOne(and(equal("_id", orderId)), set(fieldName, complete)).toFuture()
    f.onFailure({
      case ex: Exception => Logger.error(ex.getMessage, ex)
    })
    f
  }

  case class QueryOrderParam(_id: Option[String], brand: Option[String], name: Option[String],
                             factoryId: Option[String], customerId: Option[String],
                             start: Option[Long], end: Option[Long])
  def queryOrder(param: QueryOrderParam) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._

    val idFilter = param._id map { _id => regex("_id", _id) }
    val brandFilter = param.brand map { brand => regex("brand", brand) }
    val nameFilter = param.name map { name => regex("name", name) }
    val factoryFilter = param.factoryId map { factoryId => regex("factorId", factoryId) }
    val customerFilter = param.customerId map { customerId => regex("customerId", customerId) }
    val startFilter = param.start map { start => gte("expectedDeliverDate", start) }
    val endFilter = param.end map { end => lt("expectedDeliverDate", end) }

    val filterList = List(idFilter, brandFilter, nameFilter, factoryFilter,
      customerFilter, startFilter, endFilter).flatMap { f => f }
    val filter = if (!filterList.isEmpty)
      and(filterList: _*)
    else
      Filters.exists("_id")

    val f = collection.find(filter).sort(Sorts.ascending("_id")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records
  }

  def closeOrder(_id: Long) = {
    import org.mongodb.scala.model.Updates._
    val f = collection.findOneAndUpdate(equal("_id", _id), set("active", false)).toFuture()
    f
  }

  def deleteOrder(_id: Long) = {
    collection.deleteOne(equal("_id", _id)).toFuture()
  }
}
