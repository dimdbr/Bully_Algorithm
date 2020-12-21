
 object Main {

  def main(args: Array[String]): Unit = {
    // write your code here
    val total_processes: Int = 6
    val t: Array[RunningThread] = Array.ofDim[RunningThread](total_processes)
    for (i <- 0 until total_processes) {
      //passing process id, priority, total no. of processes to running thread
      t(i) = new RunningThread(new Process(i + 1, i + 1), total_processes)

    }
    try Election.initialElection(t)
    catch {
      case e: NullPointerException => println(e.getMessage)

    }

    for (i <- 0 until  total_processes) {

      //start every thread
      new Thread(t(i)).start()
    }
  }

}