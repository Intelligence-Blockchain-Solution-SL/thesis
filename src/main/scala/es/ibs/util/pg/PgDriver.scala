package es.ibs.util.pg

import java.sql.{Connection, DatabaseMetaData, PreparedStatement, ResultSet}
import java.util.{Date, Properties}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.postgresql.jdbc.PgResultSet
import org.postgresql.util.PGobject
import play.api.libs.json.{JsValue, Json}
import es.ibs.util._

// https://github.com/brettwooldridge/HikariCP/wiki/Rapid-Recovery
// https://jdbc.postgresql.org/documentation/head/connect.html#connection-parameters

/* config:
  pg {
    DataSourceProperties {
      # sslmode = require
      socketTimeout = 60
      ApplicationName = wdsf
    }
    PoolName = "HP"
    JdbcUrl = "jdbc:postgresql://localhost/wdsf"
    Username = wdsf
    Password = "123"
    ReadOnly = true

    MaximumPoolSize = 3
  }
*/
trait PgDriver extends PgTypes {

  protected val dataSource: HikariDataSource

  // universal parameters processor for any statement type (offs is for func only)
  private final def Parametrize(db: Connection, ps: PreparedStatement, p: Seq[Product], offs: Int = 1) {
    offs until p.size + offs foreach { i =>
      p(i - offs) match {
        // jsonb
        case (PG_JSONB_IN, v: JsValue) =>
          con(new org.postgresql.util.PGobject) { o =>
            o.setType("jsonb")
            o.setValue(v.toString())
            ps.setObject(i, o)
          }
        // value with simple type...
        // ... null/not null ...
        case (t: Int, v: Option[Any]) =>
          v.fold(ps.setNull(i, t))(some => ps.setObject(i, some, t))
        // ... not null ...
        case (t: Int, v: Any) =>
          ps.setObject(i, v, t)
        // ... null
        case (t: Int, null) =>
          ps.setNull(i, t)
        // array of explicitly named postgres type (t)
        case (t: String, v: Iterable[_]) =>
          ps.setArray(i, db.createArrayOf(t, v.map(_.asInstanceOf[AnyRef]).toArray))
        case (t: String, v: Array[_]) =>
          ps.setArray(i, db.createArrayOf(t, v.map(_.asInstanceOf[AnyRef])))
        // null value for array
        case (_: String, null) =>
          ps.setNull(i, PG_ARRAY)
        // errors
        case (t: Any, v: Any) =>
          throw new Exception(s"[PgDriver.Parametrize] type -> value error ('${t.getClass}' -> '${v.getClass}')")
        case a: Any =>
          throw new Exception(s"[PgDriver.Parametrize] ERRER: '$a' ")
      }
    }
  }

  // function CALL
  final def dbCall[A](f: String, p: Seq[Product] = Seq.empty, r: Int = PG_NULL): A = {
    val proc = r == PG_NULL
    using(dataSource.getConnection) { db =>
      using(db.prepareCall(s"{ ${if(proc) "" else "?="} call $f(${Seq.fill(p.size)('?').mkString(",")}) }")) { c =>
        // set result, process parameters, execute
        if(!proc) c.registerOutParameter(1, r)
        Parametrize(db, c, p, if(proc) 1 else 2)
        c.execute()
        // process result
        if(proc) ().asInstanceOf[A]
        else if(r == PG_ARRAY) c.asInstanceOf[PgResultSet].getArray(1).asInstanceOf[A]
        else if(r == PG_JSONB_OUT) Json.parse(c.getObject(1).asInstanceOf[PGobject].getValue).asInstanceOf[A]
        else c.getObject(1).asInstanceOf[A]
      }
    }
  }

  // INSERT / UPDATE / DELETE
  final def dml(stmt: String, params: Product*): Int = {
    using(dataSource.getConnection) { db =>
      using(db.prepareStatement(stmt)) { ps =>
        Parametrize(db, ps, params)
        ps.executeUpdate()
      }
    }
  }

  // SELECT / INSERT+returning private base (warning! Iterator is not iterating here, so wrappers required)
  private final def queryImpl[A, B](q: String, p: Seq[Product])(row: ResultSet => A)(fetch: Iterator[A] => B) = {
    // prepare/execute (workaround ==> .asInstanceOf[HikariProxyResultSet].unwrap[PgResultSet])
    using(dataSource.getConnection) { db =>
      using(db.prepareStatement(q)) { s =>
        Parametrize(db, s, p)
        using(s.executeQuery()) { rs =>
          // postprocessing data, generating result
          fetch(new Iterator[A] {
            def hasNext: Boolean = rs.next()
            def next(): A = row(rs)
          })
        }
      }
    }
  }

  // INSERT+returning helpers
  final def insertRetOne[A](ins: String, params: Product*)(r: ResultSet => A): A =
    selectOne(ins, params:_*)(r) // strictly fetch only one value

  final def insertRetList[A](ins: String, params: Product*)(r: ResultSet => A): List[A] =
    select(ins, params:_*)(r)

  // SELECT helpers
  final def select[A](query: String, params: Product*)(r: ResultSet => A): List[A] =
    queryImpl(query, params)(r)(_.toList)

  final def selectMap[A, B](query: String, params: Product*)(r: ResultSet => (A, B)): Map[A, B] =
    queryImpl(query, params)(r)(_.toMap)

  final def selectOne[A](query: String, params: Product*)(r: ResultSet => A): A =
    queryImpl(query, params)(r)(f => { if (!f.hasNext) throw PgNoDataFoundException else f.next() }) // strictly fetch only one value

  final def selectOneOption[A](query: String, params: Product*)(r: ResultSet => A): Option[A] =
    queryImpl(query, params)(r)(f => { if (f.hasNext) Some(f.next()) else None }) // strictly fetch only one value

  // several useful routines

  final def DB_INFO(): String = {
    using(dataSource.getConnection) { db =>
      s"Connected to [${dbCall("version", r = PG_TEXT)}] with [${db.getMetaData.getDriverName}] as [${db.getMetaData.getUserName}] and [ro=${db.isReadOnly};ti=${db.getTransactionIsolation}]" // call version() for additionally validate the connection
    }
  }

  final def getMetaData: DatabaseMetaData = using(dataSource.getConnection)(_.getMetaData)

  final def dbNow: Date = dbCall[java.util.Date]("now", r = PG_TIMESTAMP)
}

object PgDriver {
  import org.slf4j.bridge.SLF4JBridgeHandler
  SLF4JBridgeHandler.install()

  def dataSourceFromProperties(properties: Properties) = new HikariDataSource(new HikariConfig(properties))
  def apply(properties: Properties): PgDriver = new PgDriver { val dataSource: HikariDataSource = dataSourceFromProperties(properties) }

  def dataSourceFromParams(jdbcUrl: String, username: String, password: String, poolSize: Int = 1, props: Map[String, Any] = Map.empty): HikariDataSource = {
    new HikariDataSource(con(new HikariConfig()) { cfg =>
      cfg.setJdbcUrl(jdbcUrl)
      cfg.setUsername(username)
      cfg.setPassword(password)
      cfg.setMaximumPoolSize(poolSize)
      props foreach { case (k, v) => cfg.addDataSourceProperty(k, v) }
    })
  }
  def apply(jdbcUrl: String, username: String, password: String, poolSize: Int = 1, props: Map[String, Any] = Map("sslmode" -> "required")): PgDriver =
    new PgDriver { val dataSource: HikariDataSource = dataSourceFromParams(jdbcUrl, username, password, poolSize, props) }
}