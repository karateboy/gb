package controllers

import play.api.mvc.Controller
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Future
import play.api.libs.json._
import com.github.nscala_time.time.Imports._
import models._
import scala.concurrent.ExecutionContext.Implicits.global
import models.ModelHelper._

/**
 * Created by user on 2017/1/13.
 */
object OrderManager extends Controller {
  def upsertOrder = Action.async(BodyParsers.parse.json) {
    implicit request =>
      val result = request.body.validate[Order]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        order => {
          val f = Order.upsertOrder(order)
          if (order.date.isEmpty)
            order.date = Some(DateTime.now().getMillis)

          f.recover({
            case ex: Throwable =>
              Logger.error("upsertOrder failed", ex)
              Ok(Json.obj("ok" -> false))
          })

          for (result <- f)
            yield Ok(Json.obj("ok" -> true))
        })
  }

  def checkOrderId(orderId: String) = Security.Authenticated.async {
    implicit request =>
      val f = Order.getOrder(orderId)
      f.recover({
        case ex: Throwable =>
          Logger.error("checkOrderId failed", ex)
          Ok(Json.obj("ok" -> false))
      })

      for (records <- f) yield {
        val ok = records.isEmpty
        Ok(Json.obj("ok" -> ok))
      }
  }

  def getOrder(orderId: String) = Security.Authenticated.async {
    implicit request =>
      val f = Order.getOrder(orderId)
      f.recover({
        case ex: Throwable =>
          Logger.error("checkOrderId failed", ex)
          Ok(Json.obj("ok" -> false))
      })

      for (orderOpt <- f) yield {
        if (orderOpt.isEmpty)
          NoContent
        else {
          Ok(Json.toJson(orderOpt.get))
        }
      }
  }

  def myActiveOrder(userId: String) = Security.Authenticated.async {
    implicit request =>
      val f = Order.myActiveOrder(userId)
      for (orderList <- f) yield {
        Ok(Json.toJson(orderList))
      }

  }

  case class WorkCardSpec(orderId: String, factoryId: String, index: Int, detail: OrderDetail, due: Long, need: Int)
  case class DyeCardSpec(color: String, due: Long, workCardSpecList: Seq[WorkCardSpec])

  def getDyeCardSpec = Security.Authenticated.async {
    implicit request =>
      val f = Order.listActiveOrder()

      for {
        orderList <- f
      } yield {
        def needToProduceF(orderId: String, detailIndex: Int, detail: OrderDetail) = {
          def goodFuture = {
            val fWorkCards = WorkCard.getOrderWorkCards(orderId, detailIndex)
            for (workCards <- fWorkCards) yield {
              val good = workCards map { _.good }
              good.sum
            }
          }
          for (good <- goodFuture)
            yield detail.quantity - good
        }

        val colorWorkCardSpecFutureSeq =
          for {
            order <- orderList
            detail_idx <- order.details.zipWithIndex if !detail_idx._1.complete
            detail = detail_idx._1
            detailIndex = detail_idx._2
            needF = needToProduceF(order._id, detailIndex, detail)
          } yield {

            for (need <- needF) yield {
              if (need > 0)
                Some(detail.color -> WorkCardSpec(order._id, order.factoryId, detailIndex, detail, order.expectedDeliverDate, need))
              else
                None
            }
          }

        val colorWorkCardSpecSeqFuture = Future.sequence(colorWorkCardSpecFutureSeq)

        import scala.collection.mutable.Map
        val dyeWorkCardMap = Map.empty[String, List[WorkCardSpec]]

        val colorWorkCardSpecSeq = waitReadyResult(colorWorkCardSpecSeqFuture)
        for {
          colorWorkCardSpec <- colorWorkCardSpecSeq.flatMap(x => x)
          color = colorWorkCardSpec._1
          workCardSpec = colorWorkCardSpec._2
        } {
          val specList = dyeWorkCardMap.getOrElseUpdate(color, List.empty[WorkCardSpec])
          dyeWorkCardMap.put(color, workCardSpec :: specList)
        }

        val dyeCardSpecList = dyeWorkCardMap map {
          kv =>
            val color = kv._1
            val workSpecList = kv._2
            DyeCardSpec(color, workSpecList.head.due, workSpecList)
        }

        val sortedDyeCardSpecList = dyeCardSpecList.toSeq.sortBy { x => x.due }

        import Order._
        implicit val workCardSpecWrite = Json.writes[WorkCardSpec]
        implicit val dyeCardSpecWrite = Json.writes[DyeCardSpec]

        Ok(Json.toJson(sortedDyeCardSpecList))
      }
  }

  def checkWorkCardId(id: String) = Security.Authenticated.async {
    implicit request =>
      val f = WorkCard.getCard(id)
      f.recover({
        case ex: Throwable =>
          Logger.error("checkOrderId failed", ex)
          Ok(Json.obj("ok" -> false))
      })

      for (records <- f) yield {
        val ok = records.isEmpty
        Ok(Json.obj("ok" -> ok))
      }
  }

/*
  def scheduleDyeWork = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      import WorkCard._
      implicit val scheduleParamRead = Json.reads[ScheduleParam]
      val result = request.body.validate[ScheduleParam]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        param => {
          val now = DateTime.now()
          val rawDyeCard = param.dyeCard
          val dyeCard = rawDyeCard.init

          val rawWorkCards = param.workCards
          val workCards = rawWorkCards map { raw =>
            val workCard = raw.init
            workCard.dyeCardID = Some(dyeCard._id)
            workCard
          }

          val workCardId = workCards.map { _._id }
          dyeCard.workIdList = workCardId
          def updateDyeCardSizeChart = {
            var orderSet = Set.empty[String]
            for (workCard <- workCards)
              orderSet += (workCard.orderId)

            val f = Order.findOrders(orderSet.toSeq)
            for (orders <- f) yield {
              val pair = orders map { order => order._id -> order }
              pair.toMap
            }
          }

          val orderMap = waitReadyResult(updateDyeCardSizeChart)

          val sizeList = workCards.map {
            work =>
              val order = orderMap(work.orderId)
              order.details(work.detailIndex).size
          }
          val sizeSet = Set(sizeList.toSeq: _*)
          val sizeCharts = sizeSet.toSeq map { SizeChart(_, None, None) }
          dyeCard.sizeCharts = Some(sizeCharts)

          val f1 = DyeCard.newCard(dyeCard)
          val f2 = WorkCard.insertCards(workCards)
          val f3 = Future.sequence(workCards.map {
            workCard =>
              Order.addOrderDetailWorkID(workCard.orderId, workCard.detailIndex, workCard._id)
          })

          val f4 = Future.sequence(List(f1, f2, f3))
          for (ret <- f3) yield {
            Ok(Json.obj("ok" -> true))
          }
        })
  }
*/
  
  import Order._
  def queryOrder() = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      implicit val paramRead = Json.reads[QueryOrderParam]
      val result = request.body.validate[QueryOrderParam]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        param => {
          val f = Order.queryOrder(param)
          for (orderList <- f)
            yield Ok(Json.toJson(orderList))
        })
  }

  def closeOrder(_id: String) = Security.Authenticated.async {
    val f = Order.closeOrder(_id)
    for (rets <- f) yield {
      if (rets.isEmpty)
        Ok(Json.obj("ok" -> false, "msg" -> "找不到訂單"))
      else {
        Ok(Json.obj("ok" -> true))
      }
    }
  }

  def deleteOrder(_id: String) = Security.Authenticated.async {
    import WorkCard._
    val param = QueryWorkCardParam(_id = None, orderId = Some(_id), start = None, end = None)
    val f = WorkCard.query(param)
    for (workCardList <- f) yield {
      if (workCardList.isEmpty) {
        Order.deleteOrder(_id)
        Ok(Json.obj("ok" -> true))
      } else {
        Ok(Json.obj("ok" -> false, "msg" -> "訂單已排入生產, 無法刪除"))
      }
    }
  }

}
