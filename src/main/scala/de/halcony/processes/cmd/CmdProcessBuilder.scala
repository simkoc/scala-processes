package de.halcony.processes.cmd

import wvlet.log.LogLevel

import java.io.File
import java.lang.ProcessBuilder.Redirect

/** CmdProcessBuilder used to create a CmdProcess
 *
 * @param cmd the cmd line to be run
 */
class CmdProcessBuilder(cmd : String*) {

  private val builder : ProcessBuilder = new ProcessBuilder(cmd*)
  private val processWrapper : CmdProcess = new CmdProcess()

  private var isStdOutSet : Boolean = false
  private def stdOutSet() : Unit = {
    if(isStdOutSet) throw new RuntimeException("stdout handling already defined")
    isStdOutSet = true
  }

  private var isStdErrSet : Boolean = false
  private def stdErrSet() : Unit = {
    if(isStdErrSet) throw new RuntimeException("stderr handling already defined")
    isStdErrSet = true
  }

  /** sets the working directory of the spawned cmd process
   *
   * @param folder the working directory
   * @return the current builder
   */
  def directory(folder : File) : CmdProcessBuilder = {
    if(!folder.isDirectory)
      throw new RuntimeException(s"the provided path is not a directory: ${folder.getAbsolutePath}")
    builder.directory(folder)
    this
  }

  /** redirects stdout of the process into a file
   *
   * @param collector the file to write the stdout into
   * @return the current builder
   */
  def collectStdOut(collector : File) : CmdProcessBuilder = {
    stdOutSet()
    builder.redirectOutput(collector)
    this
  }

  /** the spawned process collects stdout as a string (default behavior)
   *
   * @return the current builder
   */
  def collectStdOut(): CmdProcessBuilder = {
    val sb: StringBuilder = new StringBuilder()
    collectStdOut(sb)
  }

  /** the spawned process collects stdout in the provided collector
   *
   * @param collector the collector to write stdout into
   * @return the current builder
   */
  def collectStdOut(collector : StringBuilder) : CmdProcessBuilder = {
    stdOutSet()
    processWrapper.setStdoutCollector(collector)
    this
  }

  /** the spawned process discards all output to stdout
   *
   * @return the current builder
   */
  def discardStdOut() : CmdProcessBuilder = {
    stdOutSet()
    builder.redirectOutput(Redirect.DISCARD)
    this
  }

  /** the spawned process redirects stderr of the process into a file
   *
   * @param collector the file to write stderr into
   * @return the current builder
   */
  def collectStdErr(collector : File) : CmdProcessBuilder = {
    stdErrSet()
    builder.redirectError(collector)
    this
  }

  /** the spawned process collects stderr as a string (default behavior)
   *
   * @return the current builder
   */
  def collectStdErr(): CmdProcessBuilder = {
    val sb: StringBuilder = new StringBuilder()
    collectStdErr(sb)
  }

  /** the spawned process collects stderr in the provided collector
   *
   * @return the current builder
   */
  def collectStdErr(collector : StringBuilder) : CmdProcessBuilder = {
    stdErrSet()
    processWrapper.setStderrCollector(collector)
    this
  }

  /** the spawned process discards all output to stderr
   *
   * @return the current builder
   */
  def discardStdErr() : CmdProcessBuilder = {
    stdErrSet()
    builder.redirectError(Redirect.DISCARD)
    this
  }


  /** set the log level of the spawned process (default: ERROR)
   *
   * @param level the log level
   * @return the current builder
   */
  def setLogLevel(level : LogLevel) : CmdProcessBuilder = {
    processWrapper.setLogLevel(level)
    this
  }


  /** start the build process
   *
   * @return the now running processes
   */
  def start() : CmdProcess = {
    if(!isStdOutSet) this.collectStdOut()
    if(!isStdErrSet) this.collectStdErr()
    processWrapper.start(builder.start())
  }

}
