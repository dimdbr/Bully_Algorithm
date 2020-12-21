

import java.util.concurrent.locks.ReentrantLock

import scala.beans.{BeanProperty, BooleanBeanProperty}



object Election{

  var pingLock: ReentrantLock = new ReentrantLock()

  var electionLock: ReentrantLock = new ReentrantLock()

  //By default no election is going on
  @BooleanBeanProperty
  var electionFlag: Boolean = false

  //By default I am allowed to ping
  @BooleanBeanProperty
  var pingFlag: Boolean = true

  var electionDetector: Process = _

  def getElectionDetector(): Process = electionDetector

  def setElectionDetector(electionDetector: Process): Unit = {
    this.electionDetector = electionDetector
  }

  def initialElection(t: Array[RunningThread]): Unit = {
    var temp: Process = new Process(-1, -1)
    for (i <- 0 until t.length
         if temp.getPriority < t(i).getProcess.getPriority) {

      temp = t(i).getProcess
    }
    t(temp.pid - 1).getProcess.CoOrdinatorFlag = true
  }

}

