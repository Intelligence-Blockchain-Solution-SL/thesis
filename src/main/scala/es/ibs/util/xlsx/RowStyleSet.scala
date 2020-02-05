package es.ibs.util.xlsx

import org.apache.poi.xssf.usermodel.XSSFCellStyle

trait RowStyleSet {
  val textCellStyle: XSSFCellStyle
  val dateCellStyle: XSSFCellStyle
  val dateToMinCellStyle: XSSFCellStyle
  val longCellStyle: XSSFCellStyle
  val bigDecimalDataStyle: IndexedSeq[XSSFCellStyle]
  val hrefCellStyle: XSSFCellStyle
  val headerTextStyle: XSSFCellStyle
}
