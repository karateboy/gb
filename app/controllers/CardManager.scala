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
import models.ModelHelper._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._

object CardManager extends Controller {
  def getWorkCard(id: String) = Security.Authenticated.async {
    implicit request =>
      val f = WorkCard.getCard(id)
      for (card <- f) yield {
        if (card.isEmpty)
          Results.NoContent
        else
          Ok(Json.toJson(card))
      }
  }

  def getWorkCards = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>

      val result = request.body.validate[Seq[String]]

      result.fold(err => {
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        }
      }, ids => {
        val f = WorkCard.getCards(ids)
        for (cards <- f) yield {
          Ok(Json.toJson(cards))
        }
      })
  }

  def getActiveWorkCards = Security.Authenticated.async {
    val f = WorkCard.getActiveWorkCards()
    for (workCards <- f)
      yield Ok(Json.toJson(workCards))
  }

  def getOrderDetailWorkCards(orderId: Long, detailIndex: Int) = Security.Authenticated.async {
    val f = WorkCard.getOrderWorkCards(orderId, detailIndex)
    for (cards <- f) yield {
      Ok(Json.toJson(cards))
    }
  }

  import WorkCard._
  def queryWorkCard = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      implicit val paramRead = Json.reads[QueryWorkCardParam]
      val result = request.body.validate[QueryWorkCardParam]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        param => {
          val f = WorkCard.query(param)
          for (cardList <- f)
            yield Ok(Json.toJson(cardList))
        })
  }

}