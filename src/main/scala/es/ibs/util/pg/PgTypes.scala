package es.ibs.util.pg

private[pg] trait PgTypes {
  import java.sql.Types

  // aliases for types
  protected val PG_NULL: Int = Types.NULL

  protected val PG_INT: Int = Types.INTEGER
  protected val PG_BIGINT: Int = Types.BIGINT
  protected val PG_NUMERIC: Int = Types.DECIMAL
  protected val PG_BOOLEAN: Int = Types.BOOLEAN

  protected val PG_TEXT: Int = Types.VARCHAR
  protected val PG_UUID: Int = Types.OTHER

  protected val PG_DATE: Int = Types.DATE
  protected val PG_TIMESTAMP: Int = Types.TIMESTAMP

  protected val PG_BYTEA: Int = Types.LONGVARBINARY

  protected val PG_ARRAY: Int = Types.ARRAY

  protected val PG_JSONB_IN: Int = -100500
  protected val PG_JSONB_OUT: Int = Types.OTHER
}
