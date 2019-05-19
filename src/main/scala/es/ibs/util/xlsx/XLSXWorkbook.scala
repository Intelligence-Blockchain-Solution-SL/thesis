package es.ibs.util.xlsx

import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFCreationHelper, XSSFDataFormat, XSSFWorkbook}
import es.ibs.util.{con, using}

class XLSXWorkbook extends XSSFWorkbook{

  import org.apache.poi.ss.usermodel.Font
  import org.apache.poi.ss.usermodel.IndexedColors

  private val props = this.getProperties
  private val propsC = props.getCoreProperties
  propsC.setCreator(util.Properties.versionMsg)

  lazy val helper: XSSFCreationHelper = this.getCreationHelper
  lazy val format: XSSFDataFormat = this.createDataFormat

  lazy private val textDataFmt: Short = format.getFormat("@")
  lazy private val dateDataFmt: Short = format.getFormat("yyyy-mm-dd")
  lazy private val dateToMinDataFmt: Short  = format.getFormat("yyyy-mm-dd hh:mm")
  lazy private val longDataFmt: Short = format.getFormat("0") // will use builtin
  lazy private val bigDecimalDataFormat: IndexedSeq[Short] = IndexedSeq(
    format.getFormat("0"),
    format.getFormat("0.0"),
    format.getFormat("0.00"),
    format.getFormat("0.000"),
    format.getFormat("0.0000"),
    format.getFormat("0.00000")
  )

  lazy val textCellStyle: XSSFCellStyle = con(this.createCellStyle())(_.setDataFormat(textDataFmt))
  lazy val dateCellStyle: XSSFCellStyle = con(this.createCellStyle())(_.setDataFormat(dateDataFmt))
  lazy val dateToMinCellStyle: XSSFCellStyle = con(this.createCellStyle())(_.setDataFormat(dateToMinDataFmt))
  lazy val longCellStyle: XSSFCellStyle = con(this.createCellStyle())(_.setDataFormat(longDataFmt))
  lazy val bigDecimalDataStyle: IndexedSeq[XSSFCellStyle] = bigDecimalDataFormat.map(f => con(this.createCellStyle())(_.setDataFormat(f)))

  lazy val hrefCellStyle: XSSFCellStyle = con(this.createCellStyle) { cs =>
    cs.setDataFormat(textDataFmt)
    cs.setFont(con(this.createFont) { f => f.setUnderline(Font.U_SINGLE); f.setColor(IndexedColors.BLUE.getIndex) })
  }

  lazy val headerTextStyle: XSSFCellStyle = con(this.createCellStyle) { cs =>
    cs.setAlignment(HorizontalAlignment.CENTER)
    cs.setDataFormat(textDataFmt)
    cs.setFont(con(this.createFont)(_.setBold(true)))
  }

  def writeToFile(filename: String ): Unit = {
    import java.io.FileOutputStream
    using(new FileOutputStream(filename))(this.write(_))
  }

  def toBytes: Array[Byte] = {
    import java.io.ByteArrayOutputStream
    using(new ByteArrayOutputStream) { s => this.write(s); s.toByteArray }
  }

  def createSheetW(sheetname: String): XLSXSheetWrapper = new XLSXSheetWrapper(super.createSheet(sheetname), this)
}
