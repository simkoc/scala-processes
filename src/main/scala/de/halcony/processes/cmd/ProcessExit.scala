package de.halcony.processes.cmd

/** Trait representing the state of a process
  *
  */
sealed trait ProcessState

/** Process has terminated
  *
  * @param exitValue the exit value unpon termination
  */
sealed case class Terminated(exitValue: Int) extends ProcessState

/** Process is still alive
  *
  */
sealed case class Alive() extends ProcessState
