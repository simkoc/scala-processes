package de.halcony.processes.cmd

import wvlet.log.LogLevel.ERROR
import wvlet.log.{LogLevel, LogSupport}

import scala.jdk.CollectionConverters._
import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.util.concurrent.TimeUnit

/** A cmd line process
 *
 */
class CmdProcess extends LogSupport {

  val READ_BUFFER_SIZE = 1000

  this.logger.setLogLevel(ERROR)

  private var wrappedProcess : Option[Process] = None
  private def getProcess : Process = wrappedProcess match {
    case Some(process) => process
    case None => throw new RuntimeException("process has never been started")
  }

  private var shallBeAlive = true
  private def getShallBeAlive : Boolean = synchronized { shallBeAlive }
  private def setShallBeAlive(value : Boolean) = synchronized {
    shallBeAlive = value
    shallBeAlive
  }

  private var stdoutCollector : Option[StringBuilder] = None
  private var stdoutCollectorThread : Option[Thread] = None
  private var stdoutCollectorError : Option[Exception] = None
  private var stderrCollector : Option[StringBuilder] = None
  private var stderrCollectorThread : Option[Thread] = None
  private var stderrCollectorError : Option[Exception] = None


  private def createStreamReaderThread(inputStream : InputStream, collector : StringBuilder, errorStream : Boolean, readBufferSize : Int = READ_BUFFER_SIZE) : Thread = {
    val logger = this.logger
    new Thread( () => {
      try {
        val reader = new BufferedReader(new InputStreamReader(inputStream))
        try {
          val buffer : Array[Char] = Array.ofDim(readBufferSize)
          while ((reader.ready() || this.isAlive) && getShallBeAlive) {
            val read = reader.read(buffer)
            if(read != -1)
              collector.append(String.copyValueOf(buffer,0,read))
            if (!reader.ready()) {
              reader.synchronized {
                reader.wait(100)
              }
            }
          }
        } finally {
          reader.close()
        }
      } catch {
        case e : Exception =>
          if(errorStream) {
            logger.error("[ErrorStream]" + e.getMessage, e)
            stderrCollectorError = Some(e)
          } else {
            logger.error("[StdoutStream]" + e.getMessage, e)
            stdoutCollectorError = Some(e)
          }
      }
    })
  }

  def flushOutputs(): Unit = {
    stdoutCollectorThread match {
      case Some(thread) => thread.join()
      case None =>
    }
    stderrCollectorThread match {
      case Some(thread) => thread.join()
      case None =>
    }
  }

  protected[cmd] def setStdoutCollector(collector : StringBuilder) : CmdProcess = {
    stdoutCollector = Some(collector)
    this
  }

  protected[cmd] def setStderrCollector(collector : StringBuilder) : CmdProcess = {
    stderrCollector = Some(collector)
    this
  }

  protected[cmd] def start(process : Process) : CmdProcess = {
    wrappedProcess = Some(process)
    stdoutCollector.foreach {
      collector =>
        stdoutCollectorThread = Some(createStreamReaderThread(process.getInputStream, collector, false))
        stdoutCollectorThread.get.start()
    }
    stderrCollector.foreach {
      collector =>
        stderrCollectorThread = Some(createStreamReaderThread(process.getErrorStream, collector, false))
        stderrCollectorThread.get.start()
    }
    this
  }

  /** set the log level of the current process
   *
   * @param level the log level
   * @return the current process
   */
  def setLogLevel(level : LogLevel) : CmdProcess = {
    this.logger.setLogLevel(level)
    this
  }

  /** returns the wrapped java.Process
   *
   * @return the wrapped java.Process
   */
  def getWrappedProcess : Process = getProcess


  def isAlive : Boolean = {
    getProcess.isAlive
  }

  /** checks if the underlying process is alive
   *
   * @return true if the underlying process is alive
   */
  def isAlive(descendants : Boolean = false): Boolean = {
    if(!descendants) {
      getProcess.isAlive
    } else {
      getProcess.descendants().toList.asScala.exists(_.isAlive) || getProcess.isAlive
    }
  }

  /** wait for the process to terminate
   *
   * @param timeout timeout length
   * @param unit timeout unit
   * @return whether the process terminated within the allotted time
   */
  def waitFor(timeout : Long, unit : TimeUnit) : Boolean = {
    val ret = getProcess.waitFor(timeout, unit)
    if(!isAlive) flushOutputs()
    ret
  }


  /** waits for the process to terminate and handles success/failure
   *
   * @param timeout timout length
   * @param unit timeout unit
   * @param onSuccess what to do if the process terminates on time
   * @param onFailure what to do if the process does not terminate on time
   * @tparam T the return type of both succces and failure
   * @return the value returned by success/failure
   */
  def waitFor[T](timeout : Long, unit : TimeUnit, onSuccess : => T, onFailure : => T) : T = {
    waitFor(timeout,unit)
    if(isAlive) {
      onSuccess
    } else {
      onFailure
    }
  }

  /** interrupt/destroy the currently running process
   *
   */
  def destroy() : Unit = {
    setShallBeAlive(false)
    getProcess.descendants().forEach {
      processHandle => processHandle.destroy()
    }
    getProcess.destroy()
  }

  /** interrupt/destroy the current process and handle failure to do so (after a graceperiod of 100ms)
   *
   * @param orElse what to do if the process did not terminate
   */
  def destroyOrElse(orElse :  => Unit) : Unit = {
    this.destroyOrElse(100,TimeUnit.MILLISECONDS,orElse)
  }

  /** interrupt/destroy the current process and handle failure to do so
   *
   * @param gracePeriod graceperiod to check if successfull
   * @param unit graceperiod time unit
   * @param orElse what to do if the process did not terminate
   */
  def destroyOrElse(gracePeriod : Long, unit : TimeUnit, orElse : => Unit) : Unit = {
    this.destroy()
    this.waitFor(gracePeriod,unit)
    if(this.isAlive) {
      orElse
    }
  }

  /** destroys the process forcibly (die!!!!!)
   *
   */
  def destroyForcibly(): Unit = {
    setShallBeAlive(false)
    getProcess.descendants().forEach {
      processHandle => processHandle.destroyForcibly()
    }
    getProcess.destroyForcibly()
  }

  /** get the stdout output (so far) of the process
   *
   * @return the output (so far) of the process
   */
  def getStdout : String = stdoutCollector match {
    case Some(collector) => collector.toString()
    case None => throw new RuntimeException("no in-process stdout collection configured")
  }


  /** get the stderr output (so far) of the process
   *
   * @return the output (so far) of the process
   */
  def getStdErr : String = stderrCollector match {
    case Some(collector) => collector.toString()
    case None => throw new RuntimeException("no in-process stderr collection configured")
  }



  /** get the process exit value
   *
   * @return the exit value (error if not yet terminated)
   */
  def getExitValue : Int = {
    getProcess.exitValue()
  }

}
