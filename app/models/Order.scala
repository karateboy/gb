package models

import models._
import models.ModelHelper._
import org.mongodb.scala.bson.{ BsonArray, BsonDocument, Document }
import org.mongodb.scala.model.Indexes.ascending
import play.api.Logger
import play.api.libs.json.{ JsError, Json }

import scala.concurrent.ExecutionContext.Implicits.global
import com.github.nscala_time.time.Imports._

/**
 * Created by user on 2017/1/1.
 */

case class OrderDetail(color: String, size: String, quantity: Int, complete: Boolean) {
  def toDocument = {
    Document("color" -> color, "size" -> size,
      "quantity" -> quantity,
      "complete" -> complete)
  }
}

object OrderDetail {
  implicit def toOrderDetail(implicit doc: BsonDocument) = {
    //OrderDetail(color: String, size: String, quantity: Int)
    import org.mongodb.scala.bson._

    val color = doc.getString("color").getValue
    val size = doc.getString("size").getValue
    val quantity = doc.getInt32("quantity").getValue
    val complete = doc.getBoolean("complete").getValue

    OrderDetail(color, size, quantity, complete)
  }
}

case class Order(_id: String, salesId: String, name: String, expectedDeliverDate: Long, finalDeliverDate: Option[Long],
                 factoryId: String,
                 customerId: String, brand: String, var date: Option[Long],
                 details: Seq[OrderDetail], active: Boolean) {
  def toDocument = {
    import org.mongodb.scala.bson._
    implicit object TransformOrderDetail extends BsonTransformer[OrderDetail] {
      def apply(od: OrderDetail): BsonDocument = od.toDocument.toBsonDocument
    }

    Document("_id" -> _id,
      "salesId" -> salesId,
      "name" -> name,
      "expectedDeliverDate" -> expectedDeliverDate,
      "finalDeliverDate" -> finalDeliverDate,
      "factoryId" -> factoryId,
      "customerId" -> customerId,
      "brand" -> brand,
      "date" -> date,
      "details" -> details,
      "active" -> active)
  }
}

object Order {
  val colName = "orders"
  val collection = MongoDB.database.getCollection(colName)

  implicit val odWrite = Json.writes[OrderDetail]
  implicit val odRead = Json.reads[OrderDetail]
  implicit val orderWrite = Json.writes[Order]
  implicit val orderRead = Json.reads[Order]

  def init(colNames: Seq[String]) = {
    if (!colNames.contains(colName)) {
      val f = MongoDB.database.createCollection(colName).toFuture()
      f.onFailure(errorHandler)
      f.onSuccess({
        case _: Seq[t] =>

          val cf1 = collection.createIndex(ascending("date", "customerId", "active")).toFuture()
          val cf2 = collection.createIndex(ascending("salesId", "date", "active")).toFuture()
          val cf3 = collection.createIndex(ascending("active")).toFuture()

          cf1.onFailure(errorHandler)
          cf2.onFailure(errorHandler)
          cf3.onFailure(errorHandler)
      })
      Some(f.mapTo[Unit])
    } else
      None
  }

  def toOrder(doc: Document) = {
    import org.bson.json._

    val _id = doc.getString("_id")
    val salesId = doc.getString("salesId")
    val name = doc.getString("name")
    val expectedDeliverDate = doc("expectedDeliverDate").asInt64().getValue
    val finalDeliverDate = getOptionTime("finalDeliverDate")(doc)
    val factoryId = doc.getString("factoryId")
    val customerId = doc.getString("customerId")
    val brand = doc.getString("brand")
    val date = getOptionTime("date")(doc)
    val details = doc("details").asArray()
    val notices = doc("notices").asArray()
    val packageInfo = doc("packageInfo").asDocument()
    val active = doc.getBoolean("active")

    def toOrderDetialSeq(ar: BsonArray) = {
      import OrderDetail._
      val array = ar.toArray()
      array.map {
        doc => toOrderDetail(doc.asInstanceOf[BsonDocument])
      }
    }

    Order(_id = _id,
      salesId = salesId,
      name = name,
      expectedDeliverDate = expectedDeliverDate,
      date = date,
      finalDeliverDate = finalDeliverDate,
      factoryId = factoryId,
      customerId = customerId,
      brand = brand,
      details = toOrderDetialSeq(details),
      active = active)
  }

  def listActiveOrder() = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._
    val f = collection.find(equal("active", true)).sort(Sorts.ascending("_id")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records map {
      toOrder
    }
  }

  import org.mongodb.scala.model.Filters._

  def upsertOrder(order: Order) = {
    import org.mongodb.scala.model.UpdateOptions
    import org.mongodb.scala.bson.BsonString

    val col = MongoDB.database.getCollection(colName)
    val doc = order.toDocument

    val f = col.replaceOne(equal("_id", doc("_id")), doc, UpdateOptions().upsert(true)).toFuture()
    f.onFailure({
      case ex: Exception => Logger.error(ex.getMessage, ex)
    })
    f
  }

  def getOrder(orderId: String) = {
    val f = collection.find(equal("_id", orderId)).toFuture()
    f.onFailure {
      errorHandler
    }
    for (orders <- f) yield {
      if (orders.isEmpty)
        None
      else
        Some(toOrder(orders(0)))
    }
  }

  def getOrders(orderIds: Seq[String]) = {
    val f = collection.find(in("_id", orderIds:_*)).toFuture()
    f.onFailure {
      errorHandler
    }
    for (orders <- f) yield {
       orders map toOrder
    }
  }
  def findOrders(orderIdList: Seq[String]) = {
    import org.mongodb.scala.model._
    val f = collection.find(in("_id", orderIdList: _*)).sort(Sorts.ascending("_id")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records map {
      toOrder
    }
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
      yield records map {
      toOrder
    }
  }

  def getHistoryOrder(begin: Long, end: Long) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._
    val f = collection.find(and(gte("date", begin), lt("date", end))).sort(Sorts.ascending("_id")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records map {
      toOrder
    }
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

  def setOrderDetailComplete(orderId: String, index: Int, complete: Boolean) = {
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
    val startFilter = param.start map { start => gte("expectedDeliverDate", start)}
    val endFilter = param.end map { end => lt("expectedDeliverDate", end)}

    val filterList = List(idFilter, brandFilter, nameFilter, factoryFilter, 
        customerFilter, startFilter, endFilter).flatMap { f => f }
    val filter = if(!filterList.isEmpty) 
        and(filterList: _*)
        else
          Filters.exists("_id")

    val f = collection.find(filter).sort(Sorts.ascending("_id")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records map {
      toOrder
    }
  }

  def closeOrder(_id: String) = {
    import org.mongodb.scala.model.Updates._
    val f = collection.findOneAndUpdate(equal("_id", _id), set("active", false)).toFuture()
    f
  }
  
  def deleteOrder(_id:String) = {
    collection.deleteOne(equal("_id", _id)).toFuture()
  }
}
