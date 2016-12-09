package test

import scalikejdbc._
import skinny.DBSettings
import skinny.orm.{Alias, SkinnyCRUDMapperWithId}

object Main extends App {
  DBSettings.initialize()
  implicit val session = AutoSession
  Creator.all.map(_.apply())
  val lineId = Line.createWithAttributes('name -> "Tokyo", 'startStationId -> 1L)
  Station.createWithAttributes('name -> "Tokaido", 'lineId -> lineId)
  println(Line.findAll())
}

case class Line(
    id: Long,
    name: String,
    startStationId: Long,
    stations: Seq[Station] = Nil)

object Line extends SkinnyCRUDMapperWithId[Long, Line] {
  override val defaultAlias: Alias[Line] = createAlias("l")
  override def extract(rs: WrappedResultSet, n: ResultName[Line]): Line = autoConstruct(rs, n, "stations")
  override def idToRawValue(id: Long): Any = id
  override def rawValueToId(value: Any): Long = value.toString.toLong

  hasMany[Station]( // byDefaultだと何故かエラーになる?
    many = Station -> Station.defaultAlias,
    on = (l, s) => sqls.eq(l.id, s.lineId),
    merge = (line, stations) => line.copy(stations = stations)
  ).byDefault
}

case class Station(id: Long, name: String, lineId: Long)

object Station extends SkinnyCRUDMapperWithId[Long, Station] {
  override def defaultAlias: Alias[Station] = createAlias("s")
  override def extract(rs: WrappedResultSet, n: ResultName[Station]): Station = autoConstruct(rs, n)
  override def idToRawValue(id: Long): Any = id
  override def rawValueToId(value: Any): Long = value.toString.toLong
}

//
object Creator {
  val line: SQLExecution = sql"create table line (id bigint not null auto_increment, name varchar(255) not null, start_station_id bigint not null)".execute()
  val station: SQLExecution = sql"create table station (id bigint not null auto_increment, name varchar(255) not null, line_id bigint not null)".execute()
  val all: Seq[SQLExecution] = Seq(line, station)
}
