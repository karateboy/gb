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
import models.ModelHelper._

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

  private def exportTemplate(templateFile: String, newFileName: String) = {
    val templatePath = Paths.get(current.path.getAbsolutePath + docRoot + templateFile)
    val reportFilePath = Paths.get(current.path.getAbsolutePath + "/export/" + newFileName + ".xlsx")

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

  /*  def exportCareHouse(careHouseList: Seq[CareHouse]) = {
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
  }*/

  def exportDM(dmList: Seq[DM]) = {
    val (reportFilePath, pkg, wb) = prepareTemplate("dm.xlsx")
    val evaluator = wb.getCreationHelper().createFormulaEvaluator()
    val format = wb.createDataFormat()
    val sheet = wb.getSheetAt(0)

    val dateStyle = wb.createCellStyle();
    dateStyle.setDataFormat(format.getFormat("yyyy/mm/dd"))

    for {
      (dm, idx) <- dmList.zipWithIndex
      rowN = idx + 1
    } {
      val row = sheet.createRow(rowN)
      row.createCell(0).setCellValue(dm.company.getOrElse(""))
      row.createCell(1).setCellValue(dm.contact.getOrElse(""))
      row.createCell(2).setCellValue(dm.addr)
    }
    finishExcel(reportFilePath, pkg, wb)
  }

  def exportBuildCase(buildCaseList: Seq[BuildCase2], builderMap: Map[String, Builder]) = {
    val (reportFilePath, pkg, wb) = prepareTemplate("buildCase.xlsx")
    val evaluator = wb.getCreationHelper().createFormulaEvaluator()
    val format = wb.createDataFormat()
    val sheet = wb.getSheetAt(0)

    val dateStyle = wb.createCellStyle();
    dateStyle.setDataFormat(format.getFormat("yyyy/mm/dd"))

    for {
      (buildCase, idx) <- buildCaseList.zipWithIndex
      rowN = idx + 1
    } {
      val row = sheet.createRow(rowN)
      val c0 = row.createCell(0)
      c0.setCellStyle(dateStyle)
      c0.setCellValue(buildCase.permitDate)
      row.createCell(1).setCellValue(buildCase._id.county)
      row.createCell(2).setCellValue(buildCase.builder)
      if (!buildCase.personal) {
        val builder = builderMap(buildCase.builder)
        row.createCell(3).setCellValue(builder.contact)
        row.createCell(4).setCellValue(builder.addr)
        row.createCell(5).setCellValue(builder.phone)
      } else {
        row.createCell(3).setCellValue("-")
        row.createCell(4).setCellValue("-")
        row.createCell(5).setCellValue("-")
      }
      row.createCell(6).setCellValue(buildCase.architect)
      row.createCell(7).setCellValue(buildCase.siteInfo.area.getOrElse(0d))
      row.createCell(8).setCellValue(buildCase.siteInfo.addr)
    }
    finishExcel(reportFilePath, pkg, wb)
  }

  def exportCareHouse(careHouseList: Seq[CareHouse]) = {
    val (reportFilePath, pkg, wb) = prepareTemplate("careHouse.xlsx")
    val evaluator = wb.getCreationHelper().createFormulaEvaluator()
    val format = wb.createDataFormat()
    val sheet = wb.getSheetAt(0)

    val dateStyle = wb.createCellStyle();
    dateStyle.setDataFormat(format.getFormat("yyyy/mm/dd"))

    for {
      (careHouse, idx) <- careHouseList.zipWithIndex
      rowN = idx + 1
    } {
      val row = sheet.createRow(rowN)
      row.createCell(0).setCellValue(careHouse._id.name)
      row.createCell(1).setCellValue(careHouse._id.county)
      row.createCell(2).setCellValue(careHouse.addr)
      row.createCell(3).setCellValue(careHouse.phone)
      row.createCell(4).setCellValue(careHouse.bed)
    }

    finishExcel(reportFilePath, pkg, wb)
  }

  def exportFacility(facilityList: Seq[Facility]) = {
    val (reportFilePath, pkg, wb) = prepareTemplate("careHouse.xlsx")
    val evaluator = wb.getCreationHelper().createFormulaEvaluator()
    val format = wb.createDataFormat()
    val sheet = wb.getSheetAt(0)

    val dateStyle = wb.createCellStyle();
    dateStyle.setDataFormat(format.getFormat("yyyy/mm/dd"))

    for {
      (facility, idx) <- facilityList.zipWithIndex
      rowN = idx + 1
    } {
      val row = sheet.createRow(rowN)
    }

    finishExcel(reportFilePath, pkg, wb)
  }
  
  def exportFactorySheet(newFileName: String, factory: Facility) = {
    val escapedFileName = newFileName.replace("/", "_").replace("\\", "_")
    val (reportFilePath, pkg, wb) = exportTemplate("factory.xlsx", escapedFileName)
    val evaluator = wb.getCreationHelper().createFormulaEvaluator()
    val format = wb.createDataFormat()
    val sheet = wb.getSheetAt(0)

    var missingWasteCodeSet = Set.empty[String]

    sheet.getRow(1).getCell(0).setCellValue(factory.name)
    for (contact <- factory.contact)
      sheet.getRow(2).getCell(1).setCellValue(contact)

    for (phone <- factory.phone)
      sheet.getRow(2).getCell(3).setCellValue(phone)

    for (addr <- factory.addr)
      sheet.getRow(6).getCell(1).setCellValue(addr)

    for (location <- factory.location) {
      val gasStationF = GasStation.getNearest(location)
      val gasStation = waitReadyResult(gasStationF)
      sheet.getRow(7).getCell(1).setCellValue(s"${gasStation._id.name}:${gasStation.addr}")
    }

    for (wasteOutList <- factory.wasteOut) {
      sheet.getRow(0).getCell(6).setCellValue(wasteOutList.head.date)
      for {
        (wo, idx) <- wasteOutList.zipWithIndex
        row = idx + 9
      } {
        sheet.getRow(row).getCell(0).setCellValue(wo.wasteCode)
        sheet.getRow(row).getCell(1).setCellValue(wo.wasteName)
        sheet.getRow(row).getCell(2).setCellValue(wo.quantity)
        for (location <- factory.location) {
          val top3 = waitReadyResult(Facility.findTop3ProcessPlant(location, wo.wasteCode))
          if (top3.isEmpty) {
            missingWasteCodeSet = missingWasteCodeSet + wo.wasteCode
          }

          val desp = top3 map { proc =>
            s"${proc.name.take(4)}${proc.phone.getOrElse("")}"
          }
          sheet.getRow(row).getCell(4).setCellValue(desp.mkString(","))
        }
      }
    }

    finishExcel(reportFilePath, pkg, wb)
    missingWasteCodeSet
  }
}