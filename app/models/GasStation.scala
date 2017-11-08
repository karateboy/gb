package models
import play.api._
import com.github.nscala_time.time.Imports._
import models.ModelHelper._
import models.ExcelTool._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import play.api.Play.current
import org.apache.poi.openxml4j.opc._
import org.apache.poi.xssf.usermodel._
import org.apache.poi.ss.usermodel._
import java.util.Date
import org.mongodb.scala.model._
import org.mongodb.scala.model.Indexes._

object GasStation {
  val useType = "gasStation"
  def init() {
    val path = current.path.getAbsolutePath + "/import/gasStation.xlsx"
    importXLSX(path)(parser)
  }

  import java.io.File
  def parser(sheet: XSSFSheet)={
    var rowN = 2
    var finish = false
    var seq = IndexedSeq.empty[OilUser]
    do {
      var row = sheet.getRow(rowN)
      if (row == null)
        finish = true
      else {
        try {
          import com.github.nscala_time.time.Imports._
          val id = getStrFromCell(row.getCell(0))
          val name = row.getCell(1).getStringCellValue
          val county = row.getCell(2).getStringCellValue
          val addr = row.getCell(3).getStringCellValue
          val x = getIntFromCell(row.getCell(4))
          val y = getIntFromCell(row.getCell(5))
          val tank = getIntFromCell(row.getCell(6))
          val lonlat = CoordinateTransform.tWD97_To_lonlat(x, y)
          val location = Some(Seq(lonlat._1, lonlat._2))

          val gasStation = OilUser(_id = s"gasStation:$id",
            useType = useType,
            name = name,
            county = county,
            addr = addr,
            location = location,
            tank = tank)
          seq = seq :+ gasStation
        } catch {
          case ex: java.lang.NullPointerException =>
          // last row Ignore it...

          case ex: Throwable =>
            Logger.error(s"failed to convert row=$rowN...", ex)
        }
      }
      rowN += 1
    } while (!finish)

    import scala.concurrent._
    
    seq.map { bc =>
      val f = OilUser.collection.replaceOne(Filters.eq("_id", bc._id), bc, UpdateOptions().upsert(true)).toFuture()
      f.onFailure(errorHandler("Insert gasStation"))
      ModelHelper.waitReadyResult(f)
    }
  }
}