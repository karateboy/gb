package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Play.current
import com.github.nscala_time.time.Imports._
import models._
import PdfUtility._

object PeriodReport extends Enumeration {
  val DailyReport = Value("daily")
  val MonthlyReport = Value("monthly")
  val MinMonthlyReport = Value("MinMonthly")
  val YearlyReport = Value("yearly")
  def map = Map(DailyReport -> "日報", MonthlyReport -> "月報",
    MinMonthlyReport -> "分鐘月報",
    YearlyReport -> "年報")

}

object Report extends Controller {

}