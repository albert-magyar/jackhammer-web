package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._

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

trait Parent {
  def getRunningJobs: Array[RunningJob]
}

class MockParent extends Parent {
  var jobs: Array[RunningJob] = Array.fill(3)(new MockRunningJob)

  def getRunningJobs: Array[RunningJob] = {
    jobs
  }

  def reset: Unit = {
    jobs = Array.fill(3)(new MockRunningJob)
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

  val parent = new MockParent

  val test = DSELang.HelloDSE.test

  def index = Action {
    Ok(views.html.index("JackHammer Control Panel.", parent.getRunningJobs))
  }

  def jobs = Action {
    val jsonReply = Json.toJson(parent.getRunningJobs.toSeq)
    Ok(jsonReply)
  }

}