
class Process {

  var pid: Int = _

  var downflag: Boolean = _

  var CoOrdinatorFlag: Boolean = _

  var priority: Int = _

  def isCoOrdinatorFlag(): Boolean = CoOrdinatorFlag

  def setCoOrdinatorFlag(isCoOrdinator: Boolean): Unit = {
    this.CoOrdinatorFlag = isCoOrdinator
  }

  def isDownflag(): Boolean = downflag

  def setDownflag(downflag: Boolean): Unit = {
    this.downflag = downflag
  }

  def getPid(): Int = pid

  def setPid(pid: Int): Unit = {
    this.pid = pid
  }

  def getPriority(): Int = priority

  def setPriority(priority: Int): Unit = {
    this.priority = priority
  }

  def this(pid: Int, priority: Int) = {
    this()
    this.pid = pid
    this.downflag = false
    this.priority = priority
    this.CoOrdinatorFlag = false
  }

}