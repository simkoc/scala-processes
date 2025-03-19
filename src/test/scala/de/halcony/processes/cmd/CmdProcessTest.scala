package de.halcony.processes.cmd

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.File
import java.util.concurrent.TimeUnit
import scala.io.Source

class CmdProcessTest extends AnyWordSpec with Matchers {

  "creating a cmd process" should {

    "return the result"  in {
      val process : CmdProcess = new CmdProcessBuilder("./resources/testScripts/generateOutput.sh","1","2")
        .start()
      process.waitFor(1000,TimeUnit.MILLISECONDS)
      assert(!process.isAlive)
      assertResult("1 and 2")(process.getStdout.trim)
    }

    "write the result to a file" in {
      val file : File = File.createTempFile("unit_","_test")
      try {
        val process: CmdProcess = new CmdProcessBuilder("./resources/testScripts/generateOutput.sh", "1", "2")
          .collectStdOut(file)
          .start()
        process.waitFor(1000,TimeUnit.MILLISECONDS)
        assert(!process.isAlive)
        val contentSource = Source.fromFile(file)
        try {
          val content = contentSource.getLines().mkString("\n").trim
          assertResult("1 and 2")(content)
        } finally {
          contentSource.close()
        }
      } finally {
        file.delete()
      }
    }

    "discard the output" in {
      val process: CmdProcess = new CmdProcessBuilder("./resources/testScripts/generateOutput.sh", "1", "2")
        .discardStdOut()
        .start()
      process.waitFor(1000, TimeUnit.MILLISECONDS)
      assert(!process.isAlive)
      assertThrows[RuntimeException](process.getStdout)
    }

    "work in the provided directory" in {
      val process: CmdProcess = new CmdProcessBuilder("./readContent.sh", "content.txt")
        .directory(new File("./resources/testScripts/"))
        .start()
      process.waitFor(1000, TimeUnit.MILLISECONDS)
      assert(!process.isAlive)
      assertResult("1 and 2")(process.getStdout.trim)
    }

    "return the result even if it is a lot of output" in {
      val process: CmdProcess = new CmdProcessBuilder("./resources/testScripts/generateLotsaOutput.sh")
        .start()
      process.waitFor(10000, TimeUnit.MILLISECONDS)
      assert(!process.isAlive)
      //process.flushOutputs()
      assertResult(100000)(process.getStdout.split("\n").length)
    }

    "terminate even if the underlying process does not" in {
      val process: CmdProcess = new CmdProcessBuilder("./resources/testScripts/doNotTerminate.sh")
        .start()
      process.waitFor(1000, TimeUnit.MILLISECONDS)
      assume(process.isAlive)
      process.destroy()
      process.waitFor(1000, TimeUnit.MILLISECONDS)
      assume(!process.isAlive)
    }

    "terminate even if the underlying process starts a child that does not" in {
      val process: CmdProcess = new CmdProcessBuilder("./nonTerminatingChild.sh")
        .directory(new File("./resources/testScripts/"))
        .start()
      process.waitFor(1000, TimeUnit.MILLISECONDS)
      assume(process.isAlive(descendants = true))
      process.destroy()
      process.waitFor(1000, TimeUnit.MILLISECONDS)
      assume(!process.isAlive(descendants = true))
    }

  }

}
