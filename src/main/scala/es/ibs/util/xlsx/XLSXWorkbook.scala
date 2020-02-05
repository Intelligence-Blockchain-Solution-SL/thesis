package es.ibs.util.xlsx

import org.apache.poi.ss.usermodel.{FillPatternType, HorizontalAlignment}
import org.apache.poi.xssf.usermodel.{XSSFCreationHelper, XSSFDataFormat, XSSFWorkbook}
import es.ibs.util.{con, using}

class XLSXWorkbook extends XSSFWorkbook {

  import org.apache.poi.ss.usermodel.Font
  import org.apache.poi.ss.usermodel.IndexedColors

  private val props = this.getProperties
  private val propsC = props.getCoreProperties
  propsC.setCreator(util.Properties.versionMsg)

  lazy val helper: XSSFCreationHelper = this.getCreationHelper
  lazy val format: XSSFDataFormat = this.createDataFormat

  lazy private val textDataFmt: Short = format.getFormat("@")
  lazy private val dateDataFmt: Short = format.getFormat("yyyy-mm-dd")
  lazy private val dateToMinDataFmt: Short = format.getFormat("yyyy-mm-dd hh:mm")
  lazy private val longDataFmt: Short = format.getFormat("0") // will use builtin
  lazy private val bigDecimalDataFormat: IndexedSeq[Short] = IndexedSeq(
    format.getFormat("0"),
    format.getFormat("0.0"),
    format.getFormat("0.00"),
    format.getFormat("0.000"),
    format.getFormat("0.0000"),
    format.getFormat("0.00000")
  )

  val self = this

  lazy val regularRowStyle = new RowStyleSet {
    override lazy val textCellStyle = con(self.createCellStyle())(_.setDataFormat(textDataFmt))
    override lazy val dateCellStyle = con(self.createCellStyle())(_.setDataFormat(dateDataFmt))
    override lazy val dateToMinCellStyle = con(self.createCellStyle())(_.setDataFormat(dateToMinDataFmt))
    override lazy val longCellStyle = con(self.createCellStyle())(_.setDataFormat(longDataFmt))
    override lazy val bigDecimalDataStyle = bigDecimalDataFormat.map(f => con(self.createCellStyle())(_.setDataFormat(f)))
    override lazy val hrefCellStyle = con(self.createCellStyle) { cs =>
      cs.setDataFormat(textDataFmt)
      cs.setFont(con(self.createFont) { f => f.setUnderline(Font.U_SINGLE); f.setColor(IndexedColors.BLUE.getIndex) })
    }
    override lazy val headerTextStyle = con(self.createCellStyle) { cs =>
      cs.setAlignment(HorizontalAlignment.CENTER)
      cs.setDataFormat(textDataFmt)
      cs.setFont(con(self.createFont)(_.setBold(true)))
    }
  }

  private val highlightedColor1 = IndexedColors.LIGHT_YELLOW.getIndex

  lazy val highlighted1RowStyle = new RowStyleSet {
    override lazy val textCellStyle =
      con(self.createCellStyle()) { s => s.cloneStyleFrom(regularRowStyle.textCellStyle); s.setFillForegroundColor(highlightedColor1); s.setFillPattern(FillPatternType.SOLID_FOREGROUND) }
    override lazy val dateCellStyle =
      con(self.createCellStyle()) { s => s.cloneStyleFrom(regularRowStyle.dateCellStyle); s.setFillForegroundColor(highlightedColor1); s.setFillPattern(FillPatternType.SOLID_FOREGROUND) }
    override lazy val dateToMinCellStyle =
      con(self.createCellStyle()) { s => s.cloneStyleFrom(regularRowStyle.dateToMinCellStyle); s.setFillForegroundColor(highlightedColor1); s.setFillPattern(FillPatternType.SOLID_FOREGROUND) }
    override lazy val longCellStyle =
      con(self.createCellStyle()) { s => s.cloneStyleFrom(regularRowStyle.longCellStyle); s.setFillForegroundColor(highlightedColor1); s.setFillPattern(FillPatternType.SOLID_FOREGROUND) }
    override lazy val bigDecimalDataStyle = bigDecimalDataFormat.indices.map(i =>
      con(self.createCellStyle()) { s => s.cloneStyleFrom(regularRowStyle.bigDecimalDataStyle(i)); s.setFillForegroundColor(highlightedColor1); s.setFillPattern(FillPatternType.SOLID_FOREGROUND) })
    override lazy val hrefCellStyle =
      con(self.createCellStyle()) { s => s.cloneStyleFrom(regularRowStyle.hrefCellStyle); s.setFillForegroundColor(highlightedColor1); s.setFillPattern(FillPatternType.SOLID_FOREGROUND) }
    override lazy val headerTextStyle =
      con(self.createCellStyle()) { s => s.cloneStyleFrom(regularRowStyle.headerTextStyle); s.setFillForegroundColor(highlightedColor1); s.setFillPattern(FillPatternType.SOLID_FOREGROUND) }
  }


  def writeToFile(filename: String): Unit = {
    import java.io.FileOutputStream
    using(new FileOutputStream(filename))(this.write(_))
  }

  def toBytes: Array[Byte] = {
    import java.io.ByteArrayOutputStream
    using(new ByteArrayOutputStream) { s => this.write(s); s.toByteArray }
  }

  def createSheetW(sheetname: String): XLSXSheetWrapper = new XLSXSheetWrapper(super.createSheet(sheetname), this)
}
