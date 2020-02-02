package es.ibs.util.xlsx

import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.{XSSFAutoFilter, XSSFSheet}
import es.ibs.util._

class XLSXSheetWrapper(val sh: XSSFSheet, private val wb: XLSXWorkbook) {

  def setHeaderCaptions(captions: String*): Unit = {
    val r = appendRow
    captions foreach { cs => r._cell { c => c.setCellStyle(wb.headerTextStyle); c.setCellValue(cs) } }
  }

  def setWidths(widths: Int*): Unit = {
    widths.zipWithIndex foreach { case (w, i) => sh.setColumnWidth(i, w * 256) }
  }

  def setHeader(captions: (String, Int)*): Unit = {
    setHeaderCaptions(captions.map(_._1):_*)
    setWidths(captions.map(_._2):_*)
  }

  def appendRow: XLSXRowWrapper = new XLSXRowWrapper(sh.createRow((sh.getPhysicalNumberOfRows == 0) ? 0 | sh.getLastRowNum + 1), wb)

  def setAutoFilter(): XSSFAutoFilter =
      sh.setAutoFilter(new CellRangeAddress(0, Math.max(sh.getLastRowNum - 1,0),
        0, sh.getRow(0).getLastCellNum-1))

  def autoSizeColumns(): Unit = 0 to sh.getRow(0).getLastCellNum foreach sh.autoSizeColumn
}
