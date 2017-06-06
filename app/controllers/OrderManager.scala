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
  def newOrder = Action.async(BodyParsers.parse.json) {
    implicit request =>
      val result = request.body.validate[Order]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        order => {
          val newOrderF = order.newOrderID
          val resultFF =
            for (order <- newOrderF) yield {
              Logger.debug(s"new order _id=${order._id}")
              val f = Order.insertOrder(order)

              f.recover({
                case ex: Throwable =>
                  Logger.error("insert Order failed", ex)
                  Ok(Json.obj("ok" -> false))
              })

              for (result <- f)
                yield Ok(Json.obj("ok" -> true))
            }

          resultFF.flatMap { x => x }
        })
  }
  def upsertOrder = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      val result = request.body.validate[Order]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        order => {
          Logger.debug(s"upsert order _id=${order._id}")
          val f = Order.upsertOrder(order)

          f.recover({
            case ex: Throwable =>
              Logger.error("upsertOrder failed", ex)
              Ok(Json.obj("ok" -> false))
          })

          for (result <- f)
            yield Ok(Json.obj("ok" -> true))
        })
  }

  def unhandledOrder() = Security.Authenticated.async {
    implicit request =>
      val f = Order.unhandledOrder
      for (orderList <- f) yield {
        Logger.debug("#=" + orderList.length)
        Ok(Json.toJson(orderList))
      }
  }

  def getOrder(orderId: Long) = Security.Authenticated.async {
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

  def closeOrder(_id: Long) = Security.Authenticated.async {
    val f = Order.closeOrder(_id)
    for (rets <- f) yield {
      Ok(Json.obj("ok" -> true))
    }
  }

  def deleteOrder(_id: Long) = Security.Authenticated.async {
    val f = Order.deleteOrder(_id)
    for (ret <- f)
      yield Ok(Json.obj("ok" -> true))
  }

}
