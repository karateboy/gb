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

object Boiler {
  val useType = "boiler"
  def init() {
    val path = current.path.getAbsolutePath + "/import/boiler.xlsx"
    importXLSX(path)(parser)
  }
  //
  import java.io.File
  def parser(sheet: XSSFSheet) = {
    var rowN = 1
    var finish = false
    var seq = IndexedSeq.empty[OilUser]
    do {
      var row = sheet.getRow(rowN)
      if (row == null)
        finish = true
      else {
        try {
          import com.github.nscala_time.time.Imports._
          val county = row.getCell(0).getStringCellValue
          val id = getStrFromCell(row.getCell(1))
          val name = row.getCell(2).getStringCellValue
          val addr = row.getCell(3).getStringCellValue
          val x = row.getCell(4).getNumericCellValue
          val y = row.getCell(5).getNumericCellValue
          val lonlat = CoordinateTransform.tWD97_To_lonlat(x, y)
          val location = Seq(lonlat._1, lonlat._2)
          val tank = row.getCell(6).getNumericCellValue.toInt

          val oilUser = OilUser(_id = s"boiler:$id",
            useType = useType,
            county = county,
            name = name,
            addr = addr,
            location = Some(location),
            tank = tank)
          seq = seq :+ oilUser
        } catch {
          case ex: java.lang.NullPointerException =>
          // last row Ignore it...

          case ex: Throwable =>
            Logger.error(s"failed to convert row=$rowN...", ex)
        }
      }
      rowN += 1
    } while (!finish)

    seq.map { bc =>
      val f = OilUser.collection.replaceOne(Filters.eq("_id", bc._id), bc, UpdateOptions().upsert(true)).toFuture()
      f.onFailure(errorHandler("Insert boiler"))
      ModelHelper.waitReadyResult(f)
    }
    
    import scala.concurrent._
//    val f = OilUser.collection.insertMany(seq, InsertManyOptions().ordered(false)).toFuture()
//    f.onFailure(errorHandler("Insert boiler"))
//    f
  }
}