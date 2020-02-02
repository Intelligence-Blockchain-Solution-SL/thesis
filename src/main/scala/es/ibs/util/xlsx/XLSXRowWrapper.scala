package es.ibs.util.xlsx

import java.util.Date
import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.{XSSFCell, XSSFRow}
import es.ibs.util._

class XLSXRowWrapper(row: XSSFRow, private val wb: XLSXWorkbook) {

  private var i = -1
  private[xlsx] def _cell(block: XSSFCell => Unit): Unit = { i += 1; block(row.createCell(i)) }

  def apply(s: String): Unit = _cell { c =>
    c.setCellValue(s)
    c.setCellStyle(wb.textCellStyle)
  }

  def apply(s: String, url: String): Unit = _cell { c =>
    c.setCellValue(s)
    c.setHyperlink(con(wb.helper.createHyperlink(HyperlinkType.URL))(_.setAddress(url)))
    c.setCellStyle(wb.hrefCellStyle)
    c.setCellType(CellType.STRING)
  }

  def apply(d: Date, time: Boolean = true): Unit = _cell { c =>
    c.setCellValue(d)
    if(time) c.setCellStyle(wb.dateToMinCellStyle) else c.setCellStyle(wb.dateCellStyle)
    c.setCellType(CellType.NUMERIC)
  }

  def apply(bd: BigDecimal, decimal: Int): Unit = _cell { c =>
    c.setCellValue(bd.doubleValue())
    c.setCellStyle(wb.bigDecimalDataStyle(decimal))
    c.setCellType(CellType.NUMERIC)
  }

  def apply(l: Long): Unit = _cell { c =>
    c.setCellValue(l)
    c.setCellStyle(wb.longCellStyle)
    c.setCellType(CellType.NUMERIC)
  }

  def apply(d: Option[Date], time: Boolean): Unit =
    if (d.isEmpty) apply("") else apply(d.get,time)

  def apply(bd: Option[BigDecimal], decimal: Int): Unit =
    if (bd.isEmpty) apply("") else apply(bd.get,decimal)

  def apply(l: Option[Long]): Unit =
    if (l.isDefined) apply(l.get) else apply("")

}
