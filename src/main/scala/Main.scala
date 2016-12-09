import scalikejdbc._
import skinny.DBSettings
import skinny.orm.feature.associations.Association
import skinny.orm.{Alias, SkinnyCRUDMapperWithId}

object Main extends App {
  DBSettings.initialize()
  implicit val session = AutoSession
  Creator.line.apply()
  Creator.station.apply()
  val lineId = Line.createWithAttributes('name -> "Tokyo", 'startStationId -> 1L)
  Station.createWithAttributes('name -> "Tokaido", 'lineId -> lineId)
  println(Station.findAll())
  println(Line.joins(Line.stationsRef, Line.startStationRef).findAll())
}

case class Line(
    id: Long,
    name: String,
    startStationId: Long,
    stations: Seq[Station] = Nil,
    startStation: Option[Station] = None)

object Line extends SkinnyCRUDMapperWithId[Long, Line] {
  override val defaultAlias: Alias[Line] = createAlias("l")
  override def extract(rs: WrappedResultSet, n: ResultName[Line]): Line = autoConstruct(rs, n, "stations", "startStation")
  override def idToRawValue(id: Long): Any = id
  override def rawValueToId(value: Any): Long = value.toString.toLong

  lazy val stationsRef: Association[Line] = hasMany[Station]( // byDefaultだと何故かエラーになる?
    many = Station -> Station.defaultAlias,
    on = (l, s) => sqls.eq(l.id, s.lineId),
    merge = (line, stations) => line.copy(stations = stations)
  )

  lazy val startStationRef: Association[Line] = hasOne[Station](
    right = Station,
    merge = (line, station) => line.copy(startStation = station)
  )
}

case class Station(id: Long, name: String, lineId: Long, line: Option[Line] = None)

object Station extends SkinnyCRUDMapperWithId[Long, Station] {
  override def defaultAlias: Alias[Station] = createAlias("s")
  override def extract(rs: WrappedResultSet, n: ResultName[Station]): Station = autoConstruct(rs, n, "line")
  override def idToRawValue(id: Long): Any = id
  override def rawValueToId(value: Any): Long = value.toString.toLong

  belongsTo[Line](
    right = Line,
    merge = (station, line) => station.copy(line = line)
  ).byDefault
}

//
object Creator {
  val line: SQLExecution = sql"create table line (id bigint not null auto_increment, name varchar(255) not null, start_station_id bigint not null)".execute()
  val station: SQLExecution = sql"create table station (id bigint not null auto_increment, name varchar(255) not null, line_id bigint not null)".execute()
}
