package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import scala.slick.driver.MySQLDriver.simple._

trait RunningJob {
  def getPointID: String
  def getCurrentTool: String
  def getElapsedTime: String
  def getNodeID: String
  def getChildPID: String
}

class MockRunningJob extends RunningJob {
  var rand = new scala.util.Random
  val pointID = "Point" + rand.nextInt(100).toString
  val currentTool = "FakeTool"
  val startTime = System.nanoTime()
  val nodeID = (Array.fill(4){rand.nextInt(256)}).mkString(".")
  val childPID = rand.nextInt(32768)

  def getPointID: String = { pointID }
  def getCurrentTool: String = { currentTool }
  def getNodeID: String = { nodeID }
  def getChildPID: String = { childPID.toString }

  def getElapsedTime: String = {
    ((System.nanoTime() - startTime) / 1000000000).toString
  }
}

class CPPRunningJob(pointID: Int, childPID: Int, startTime: java.sql.Date, metric: Double) extends RunningJob {
  var rand = new scala.util.Random
  val currentTool = "CPP"
  val nodeID = (Array.fill(4){rand.nextInt(256)}).mkString(".")

  def getPointID: String = { pointID.toString }
  def getCurrentTool: String = { "CPP" }
  def getNodeID: String = { nodeID }
  def getChildPID: String = { childPID.toString }

  def getElapsedTime: String = {
    (((new java.util.Date()).getTime() - startTime.getTime()) / 1000).toString
  }
}  

trait Parent {
  def getRunningJobs: Seq[RunningJob]
}

class MockParent extends Parent {
  var jobs: Seq[RunningJob] = Array.fill(3)(new MockRunningJob).toSeq

  def getRunningJobs: Seq[RunningJob] = {
    jobs
  }

  def reset: Unit = {
    jobs = Array.fill(3)(new MockRunningJob).toSeq
  }
}

class DBParent(name: String) extends Parent {
  val parameters: TableQuery[DSELang.Parameters] = TableQuery[DSELang.Parameters]

  // the query interface for the Coffees table
  val points: TableQuery[DSELang.Points] = TableQuery[DSELang.Points]
  val cpp: TableQuery[DSELang.Cpp_TC] = TableQuery[DSELang.Cpp_TC]

  val url = "jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1"

  // Create a connection (called a "session") to an in-memory H2 database
  val db = Database.forURL(url, driver = "org.h2.Driver")
  db.withSession { implicit session =>

    // Create the schema by combining the DDLs for the Suppliers and Coffees
    // tables using the query interfaces
    (parameters.ddl ++ points.ddl ++ cpp.ddl).create
    //(points.ddl ++ cpp.ddl).create
    //(parameters.ddl ++ points.ddl).create


    /* Create / Insert */

    // Insert some parameters
    points += (0, "PA6", -1, "v0.0.0", -1)
    points += (1, "PA6", -1, "v0.0.0", -2)
    val allPoints: List[(Int, String, Int, String, Int)] = points.list
    allPoints.map( x => println(x))

    parameters ++= Seq (
      (0, "A", "a0"),
      (0, "B", "b0"),
      (0, "C", "c0"),
      (1, "A", "a1"),
      (1, "B", "b1"),
      (1, "C", "c1")
    )
    val allParameters: List[(Int, String, String)] = parameters.list
    allParameters.map( x => println(x))

    val time = new java.util.Date()

    cpp += (0, 0, new java.sql.Date(time.getTime()), 0.0)
    cpp += (1, 1, new java.sql.Date(time.getTime()), 0.1)

    val allCPP = cpp.list
    allCPP.map( x => println(x))
  }

  def getRunningJobs: Seq[RunningJob] = {
    val jobs = Seq.newBuilder[RunningJob]
    db.withSession { implicit session =>
      val allCPP = cpp.list
      allCPP.map( x => jobs += new CPPRunningJob(x._1, x._2, x._3, x._4))
    }
    jobs.result
  }
}

object Application extends Controller {

  implicit object JobFormat extends Writes[RunningJob] {
    def writes(o: RunningJob): JsValue = JsObject(List("pointID" -> JsString(o.getPointID),
         	  	      	                      "currentTool" -> JsString(o.getCurrentTool),
         	  	      	                      "elapsedTime" -> JsString(o.getElapsedTime),
         	  	      	                      "nodeID" -> JsString(o.getNodeID),
         	  	      	                      "childPID" -> Json.toJson(o.getChildPID)))
  }

  val parent = new DBParent("test")

  def index = Action {
    Ok(views.html.index("JackHammer Control Panel.", parent.getRunningJobs))
  }

  def jobs = Action {
    val jsonReply = Json.toJson(parent.getRunningJobs.toSeq)
    Ok(jsonReply)
  }

  def createChild = Action {
    Ok
  }

}