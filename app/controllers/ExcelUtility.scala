package controllers
import play.api._
import play.api.Play.current
import controllers._
import models._
import org.apache.poi.openxml4j.opc._
import org.apache.poi.xssf.usermodel._
import com.github.nscala_time.time.Imports._
import java.io._
import java.nio.file.Files
import java.nio.file._
import org.apache.poi.ss.usermodel._

object ExcelUtility {
  val docRoot = "/report_template/"

  private def prepareTemplate(templateFile: String) = {
    val templatePath = Paths.get(current.path.getAbsolutePath + docRoot + templateFile)
    val reportFilePath = Files.createTempFile("temp", ".xlsx");

    Files.copy(templatePath, reportFilePath, StandardCopyOption.REPLACE_EXISTING)

    //Open Excel
    val pkg = OPCPackage.open(new FileInputStream(reportFilePath.toAbsolutePath().toString()))
    val wb = new XSSFWorkbook(pkg);

    (reportFilePath, pkg, wb)
  }

  def finishExcel(reportFilePath: Path, pkg: OPCPackage, wb: XSSFWorkbook) = {
    val out = new FileOutputStream(reportFilePath.toAbsolutePath().toString());
    wb.write(out);
    out.close();
    pkg.close();

    new File(reportFilePath.toAbsolutePath().toString())
  }

  def exportCareHouse(careHouseList: Seq[CareHouse]) = {
    val (reportFilePath, pkg, wb) = prepareTemplate("careHouse.xlsx")
    val evaluator = wb.getCreationHelper().createFormulaEvaluator()
    val format = wb.createDataFormat()

    def getCareTypeNum(careType: String, careTypeList: Seq[CareType]) = {
      careTypeList.find { ct => ct.name == careType }
    }
    val sheet = wb.getSheetAt(0)
    for {
      careHouse_idx <- careHouseList.zipWithIndex
      rowN = careHouse_idx._2 + 1
      careHouse = careHouse_idx._1
    } {
      val row = sheet.createRow(rowN)
      val isPublic = if (careHouse.isPublic)
        "公立" else "私立"
      row.createCell(0).setCellValue(isPublic)
      row.createCell(1).setCellValue(careHouse.name)
      row.createCell(2).setCellValue(careHouse.principal)
      row.createCell(3).setCellValue(careHouse.district)
      row.createCell(4).setCellValue(careHouse.addr)
      row.createCell(5).setCellValue(careHouse.phone)
      getCareTypeNum("安養", careHouse.careTypes) map {
        ct =>
          row.createCell(6).setCellValue(ct.quantity)
      }

      getCareTypeNum("養護", careHouse.careTypes) map {
        ct =>
          row.createCell(7).setCellValue(ct.quantity)
      }
      getCareTypeNum("長照", careHouse.careTypes) map {
        ct =>
          row.createCell(8).setCellValue(ct.quantity)
      }

      careHouse.beds map {
        bed =>
          row.createCell(9).setCellValue(bed)
      }

      careHouse.waste map {
        waste =>
          row.createCell(10).setCellValue(waste)
      }
    }

    finishExcel(reportFilePath, pkg, wb)
  }

  def exportBuildCase(buildCaseList: Seq[BuildCase]) = {
    val (reportFilePath, pkg, wb) = prepareTemplate("buildCase.xlsx")
    val evaluator = wb.getCreationHelper().createFormulaEvaluator()
    val format = wb.createDataFormat()

    def getCareTypeNum(careType: String, careTypeList: Seq[CareType]) = {
      careTypeList.find { ct => ct.name == careType }
    }
    val sheet = wb.getSheetAt(0)
    val createHelper = wb.getCreationHelper()
    val cellStyle = wb.createCellStyle()
    cellStyle.setDataFormat(
      createHelper.createDataFormat().getFormat("yyyy/m/d"));
    
    for {
      buildCase_idx <- buildCaseList.zipWithIndex
      rowN = buildCase_idx._2 + 1
      buildCase = buildCase_idx._1
    } {
      val row = sheet.createRow(rowN)
      row.createCell(0).setCellValue(buildCase.name)
      row.createCell(1).setCellValue(buildCase.architect)
      row.createCell(2).setCellValue(buildCase.area)
      row.createCell(3).setCellValue(buildCase.addr)
      buildCase.location map {
        location =>
          row.createCell(4).setCellValue(location(0))
          row.createCell(5).setCellValue(location(1))
      }
      buildCase.builder map {
        builder =>
          row.createCell(6).setCellValue(builder)
      }

      buildCase.phone map {
        phone =>
          row.createCell(7).setCellValue(phone)
      }

      row.createCell(8).setCellValue(
        if (buildCase.contracted)
          "是"
        else
          "否")
          
      buildCase.lastVisit map {
        lastVisit =>
          val cell = row.createCell(9)
          cell.setCellValue(lastVisit)
          //val format = BuiltinFormats.getBuiltinFormat(0xe)
          cell.setCellStyle(cellStyle)
      }

      buildCase.sales map {
        sales =>
          row.createCell(10).setCellValue(sales)
      }
    }

    finishExcel(reportFilePath, pkg, wb)
  }

}