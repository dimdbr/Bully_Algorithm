import java.io.{IOException, PrintWriter}
import java.net.{InetAddress, ServerSocket, Socket}
import java.util.Scanner

import scala.beans.{BeanProperty, BooleanBeanProperty}
import scala.util.Random
import scala.util.control.Breaks._



class RunningThread extends Runnable{

  private var messageFlag: Array[Boolean] = _
  var r: Random = new Random()
  var total_processes: Int =_
  var sock: Array[ServerSocket] = _
  var process: Process = _

  def getProcess(): Process = process

  def setProcess(process:Process): Unit={
    this.process = process
  }
  def isMessageFlag(index: Int): Boolean = this.messageFlag(index)

  def setMessageFlag(messageFlag: Boolean, index: Int): Unit = {
    this.messageFlag(index) = messageFlag
  }
  def this(process: Process, total_processes:Int) = {
    this()
    this.process = process
    this.total_processes=total_processes
    this.r = new Random()
    this.messageFlag = Array.ofDim[Boolean](total_processes)
    this.sock =  Array.ofDim[ServerSocket](total_processes)
    for (i <- 0 until total_processes) {

      this.messageFlag(i) = false
    }
  }


  private def recovery(): Unit = {
    synchronized {
      //if election is going on then wait
      while (Election.isElectionFlag) println(
        "Process[" + this.process.getPid + "]: -> Recovered from Crash")
      //Find current co-ordinator.
      try {
        Election.pingLock.lock()
        Election.setPingFlag(false)
        val outgoing: Socket = new Socket(InetAddress.getLocalHost, 12345)
        val scan: Scanner = new Scanner(outgoing.getInputStream)
        val out: PrintWriter = new PrintWriter(outgoing.getOutputStream, true)
        println(
          "Process[" + this.process.getPid + "]:-> Who is the co-ordinator?")
        out.println("Who is the co-ordinator?")
        out.flush()
        val pid: String = scan.nextLine()
        val priority: String = scan.nextLine()
        if (this.process.getPriority > java.lang.Integer.parseInt(priority)) {
          //Bully Condition
          out.println("Resign")
          out.flush()
          println(
            "Process[" + this.process.getPid + "]: Resign -> Process[" +
              pid +
              "]")
          val resignStatus: String = scan.nextLine()
          if (resignStatus.==("Successfully Resigned")) {
            this.process.setCoOrdinatorFlag(true)
            sock(this.process.getPid - 1) =
              new ServerSocket(10000 + this.process.getPid)
            println(
              "Process[" + this.process.getPid + "]: -> Bullyed current co-ordinator Process[" +
                pid +
                "]")
          }
        } else {
          out.println("Don't Resign")
          out.flush()
        }
        Election.pingLock.unlock()
        return
      } catch {
        case ex: IOException => println(ex.getMessage)

      }
    }
  }

  private def pingCoOrdinator(): Unit = {
    synchronized {
      try {
        Election.pingLock.lock()
        if (Election.isPingFlag) {
          println("Process[" + this.process.getPid + "]: Are you alive?")
          val outgoing: Socket = new Socket(InetAddress.getLocalHost, 12345)
          outgoing.close()
        }
      } catch {
        case ex: Exception => {
          Election.setPingFlag(false)
          Election.setElectionFlag(true)
          Election.setElectionDetector(this.process)
          println(
            "process[" + this.process.getPid + "]: -> Co-Ordinator is down\n" +
              "process[" +
              this.process.getPid +
              "]: ->Initiating Election")
        }

      } finally Election.pingLock.unlock()
    }
  }

  private def executeJob(): Unit = {
    val temp: Int = r.nextInt(20)
    var i: Int = 0
    while (i <= temp) {
      try Thread.sleep((temp + 1) * 100)
      catch {
        case e: InterruptedException => {
          println("Error Executing Thread:" + process.getPid)
          println(e.getMessage)
        }

      }
      { i += 1; i - 1 }
    }
  }

  private def sendMessage(): Boolean = synchronized {
    var response: Boolean = false
    try {
      Election.electionLock.lock()
      if (Election.isElectionFlag &&
        ! this.isMessageFlag(this.process.getPid - 1) &&
        this.process.priority >= Election.getElectionDetector.getPriority) {
        var i: Int = this.process.getPid + 1
        while (i <= this.total_processes) {
          try {
            val electionMessage: Socket =
              new Socket(InetAddress.getLocalHost, 10000 + i)
            println(
              "Process[" + this.process.getPid + "] -> Process[" + i +
                "]  responded to election message successfully")
            electionMessage.close()
            response = true
          } catch {
            case ex: IOException =>
              println(
                "Process[" + this.process.getPid + "] -> Process[" + i +
                  "] did not respond to election message")

            case ex: Exception => println(ex.getMessage)

          }
          { i += 1; i - 1 }
        }
        //My message sending is done
        this.setMessageFlag(true, this.process.getPid - 1)
        Election.electionLock.unlock()
        response
      } else {
        throw new Exception()
      }
    } catch {
      case ex1: Exception => {
        Election.electionLock.unlock()
        true
      }

    }
  }

  private def serve(): Unit = {
    synchronized {
      try {
        var done: Boolean = false
        var incoming: Socket = null
        val s: ServerSocket = new ServerSocket(12345)
        Election.setPingFlag(true)
        // min 5 requests and max 10 requests
        val temp: Int = this.r.nextInt(5) + 5
        for (counter <- 0 until temp) {
          incoming = s.accept()
          if (Election.isPingFlag) {
            println("Process[" + this.process.getPid + "]:Yes")
          }
          val scan: Scanner = new Scanner(incoming.getInputStream)
          val out: PrintWriter =
            new PrintWriter(incoming.getOutputStream, true)
          while (scan.hasNextLine() && !done) {
            val line: String = scan.nextLine()
            if (line.==("Who is the co-ordinator?")) {
              println(
                "Process[" + this.process.getPid + "]:-> " + this.process.getPid)
              out.println(this.process.getPid)
              out.flush()
              out.println(this.process.getPriority)
              out.flush()
            } else if (line.==("Resign")) {
              this.process.setCoOrdinatorFlag(false)
              out.println("Successfully Resigned")
              out.flush()
              incoming.close()
              s.close()
              println(
                "Process[" + this.process.getPid + "]:-> Successfully Resigned")
              return
            } else if (line.==("Don't Resign")) {
              done = true
            }
          }
        }
        //after serving 5-10 requests go down for random time
        this.process.setCoOrdinatorFlag(false)
        this.process.setDownflag(true)
        try {
          incoming.close()
          s.close()
          sock(this.process.getPid - 1).close()
          //(this.r.nextInt(10) + 1) * 10000);//going down
          Thread.sleep(15000)
          recovery()
        } catch {
          case e: Exception => println(e.getMessage)

        }
      } catch {
        case ex: IOException => println(ex.getMessage)

      }
    }
  }

  override def run(): Unit = {
    try {
      sock(this.process.getPid -1) = new ServerSocket(
      10000 + this.process.getPid)
    }
    catch {
      case ex: IOException => println(ex.getMessage)

    }
    while (true) if (process.isCoOrdinatorFlag) {
      //serve other processes
      serve()
    } else {
      breakable{
      while (true) {
        //Execute some task
        executeJob()
        //Ping the co-ordinator
        pingCoOrdinator()
        //Do Election
        if (Election.isElectionFlag) {
          if (!sendMessage()) {
            //Election is Done
            Election.setElectionFlag(false)
            println("New Co-Ordinator: Process[" + this.process.getPid + "]")
            this.process.setCoOrdinatorFlag(true)
            for (i <- 0 until total_processes) {
              this.setMessageFlag(false, i)
              break
            }

          }
        }


        }
      }
    }
  }

}
