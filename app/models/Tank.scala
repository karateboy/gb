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

object Tank {
  val useType = "tank"
  def init() {
    val path = current.path.getAbsolutePath + "/import/tank.xlsx"
    importXLSX(path)(parser)
  }
  //
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
          val county = row.getCell(0).getStringCellValue
          val addr = row.getCell(1).getStringCellValue
          val id = addr
          val x = row.getCell(2).getNumericCellValue
          val y = row.getCell(3).getNumericCellValue
          val lonlat = CoordinateTransform.tWD97_To_lonlat(x, y)
          val location = Seq(lonlat._1, lonlat._2)
          val tank = row.getCell(4).getNumericCellValue.toInt

          val tankCase = OilUser(_id = s"tank:$id",
            useType = useType,
            name = addr,
            county = county,
            addr = addr,
            location = Some(location),
            tank = tank)
          seq = seq :+ tankCase
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
      f.onFailure(errorHandler("Insert tank"))
      ModelHelper.waitReadyResult(f)
    }
    
    //val f = OilUser.collection.insertMany(seq, InsertManyOptions().ordered(false)).toFuture()
    //f.onFailure(errorHandler("Insert tank"))
    //f
  }
}