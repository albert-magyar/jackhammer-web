package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.meta.MTable

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

  val url = "jdbc:mysql://localhost:3306/" + name

  // Create a connection (called a "session") to a persistent mysql database
  val db = Database.forURL(url, driver="com.mysql.jdbc.Driver", user="root", password="workingonourproject")
  db.withSession { implicit session =>

    // Create the schema by combining the DDLs for the Suppliers and Coffees
    // tables using the query interfaces
    var ddls = List[slick.driver.MySQLDriver.DDL]()
    var allExist = true
    if (MTable.getTables("POINTS").list.isEmpty)     { ddls = ddls ++ List(points.ddl); allExist = false }
    if (MTable.getTables("PARAMETERS").list.isEmpty) { ddls = ddls ++ List(parameters.ddl); allExist = false }
    if (MTable.getTables("CPP_TC").list.isEmpty)     { ddls = ddls ++ List(cpp.ddl); allExist = false }
    if (!allExist) { ddls.reduce(_ ++ _).create }
  }

  def createChild() = {
    db.withSession { implicit session =>
      // Add to points
      val pols = points.list.map( x => x._1).sorted
      val point_id:Int = if(pols.isEmpty) 0 else pols.last + 1
      val project  = "PA6"
      val f_space  = 0
      val src_ver  = "-"
      val dsgn_cf  = 0
      points += (point_id,project,f_space,src_ver,dsgn_cf)

      // Add to parameters
      val pals  = parameters.list.map( x => x._3).sorted
      val p = if(pals.isEmpty) 0 else pals.last
      val a_val = p + (new scala.util.Random).nextInt(2)
      val b_val = p + (new scala.util.Random).nextInt(2)
      val c_val = p + (new scala.util.Random).nextInt(2)
      parameters += ((point_id, "A", a_val))
      parameters += ((point_id, "B", b_val))
      parameters += ((point_id, "C", c_val))
      

      val jols = cpp.list.map( x => x._2).sorted
      val job_id:Int    = if(jols.isEmpty) 0 else jols.last + 1
      val time = new java.util.Date()
      val date          = new java.sql.Date(time.getTime())
      var metric:Double = 0.0
      cpp += ((point_id,job_id,date,metric))
      Thread sleep 1000
      val q = for { c <- cpp if c.POINT_ID === point_id } yield c.METRIC
      q.update((new scala.util.Random).nextInt(200)*0.5)
    }
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

  val parent = new DBParent("watwat")
  parent.createChild()
  parent.createChild()
  parent.createChild()
  parent.createChild()

  def index = Action {
    Ok(views.html.index("JackHammer Control Panel.", parent.getRunningJobs))
  }

  def jobs = Action {
    val jsonReply = Json.toJson(parent.getRunningJobs.toSeq)
    Ok(jsonReply)
  }
}
