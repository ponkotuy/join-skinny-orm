import scalikejdbc._
import skinny.DBSettings
import skinny.orm.feature.associations.Association
import skinny.orm.{Alias, SkinnyCRUDMapperWithId, SkinnyJoinTable}

object Main extends App {
  DBSettings.initialize()
  implicit val session = AutoSession

  // Create all table
  Creator.all.map(_.apply())

  // Insert data
  val lineId = Line.createWithAttributes('name -> "Tokyo", 'startStationId -> 1L)
  Station.createWithAttributes('name -> "Tokaido", 'lineId -> lineId)
  val trainId = Train.createWithAttributes('name -> "Nozomi")
  LineTrain.createWithAttributes('lineId -> lineId, 'trainId -> trainId)

  // Query
  println(Station.findAll())
  println(Line.joins(Line.stationsRef, Line.startStationRef).findAll())
}

case class Line(
    id: Long,
    name: String,
    startStationId: Long, // このカラムは現実で作ってはいけない。なぜならstationがlineIdを必要とするのにstartStationIdはstationIdを必要とし、循環しているからである
    stations: Seq[Station] = Nil,
    startStation: Option[Station] = None,
    trains: Seq[Train] = Nil)

object Line extends SkinnyCRUDMapperWithId[Long, Line] {
  override val defaultAlias: Alias[Line] = createAlias("l")
  override def extract(rs: WrappedResultSet, n: ResultName[Line]): Line =
    autoConstruct(rs, n, "stations", "startStation", "trains")
  override def idToRawValue(id: Long): Any = id
  override def rawValueToId(value: Any): Long = value.toString.toLong

  lazy val stationsRef: Association[Line] = hasMany[Station]( // byDefaultだと循環してしまうのでやらない
    many = Station -> Station.defaultAlias,
    on = (l, s) => sqls.eq(l.id, s.lineId),
    merge = (line, stations) => line.copy(stations = stations)
  )

  lazy val startStationRef: Association[Line] = hasOne[Station](
    right = Station,
    merge = (line, station) => line.copy(startStation = station)
  )

  hasManyThrough[Train](
    through = LineTrain,
    many = Train,
    merge = (line, trains) => line.copy(trains = trains)
  ).byDefault
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

case class LineTrain(lineId: Long, trainId: Long)

object LineTrain extends SkinnyJoinTable[LineTrain] {
  override val defaultAlias: Alias[LineTrain] = createAlias("lt")
}

case class Train(id: Long, name: String)

object Train extends SkinnyCRUDMapperWithId[Long, Train] {
  override val defaultAlias: Alias[Train] = createAlias("t")

  override def extract(rs: WrappedResultSet, n: ResultName[Train]): Train = autoConstruct(rs, n)

  override def idToRawValue(id: Long): Any = id

  override def rawValueToId(value: Any): Long = value.toString.toLong
}

//
object Creator {
  val line: SQLExecution = sql"create table line (id bigint not null auto_increment, name varchar(255) not null, start_station_id bigint not null)".execute()
  val station: SQLExecution = sql"create table station (id bigint not null auto_increment, name varchar(255) not null, line_id bigint not null)".execute()
  val lineTrain: SQLExecution = sql"create table line_train (line_id bigint not null, train_id bigint not null)".execute()
  val train: SQLExecution = sql"create table train (id bigint not null auto_increment, name varchar(255) not null)".execute()
  val all: Seq[SQLExecution] = Seq(line, station, lineTrain, train)
}
