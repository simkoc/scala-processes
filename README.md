# scala processes (`0.5.0`)

While working with the command line and parallelizing execution I encountered the same code patterns over
and over again. This library is my attempt at factorizing those patterns into one neat dependency.


## Install using sbt

You need to add the dependency
```
    "de.halcony"                 %% "scala-processes"                % "(version)"
```

as well as the resolver

```
resolvers ++= Seq(
    "Sonatype OSS Snapshots" at "https://s01.oss.sonatype.org/content/repositories/public",
)
```


## Usage

There are currently two subpackages `cmd` and `threading`. They address issues I encountered, trying to reliably start and terminate command line processes while getting their output and
scaling the processing of tasks using threading via a thread pool.

#### Cmd

My main grievances with the standard API in Scala is that you have basically no control over the process
and cannot kill it if it refuses to die. When it comes to the Java API reading the stderr and stdout stream
is patently annoying as it either blocks and prevents the process from running its course if you do not read it while the process is running or you have to
spawn and manage your own reader thread. This utility lib is supposed to address either issue.

```
import de.halcony.processes.cmd._

//....
val process : CmdProcess = new CmdProcessBuilder("programm","and","parameter")
    .directory(new File("/path/to/directory/to/run/in/")) // optional
    .start()
process.waitFor(1000,TimeUnit.MILLISECONDS)
process.isAlive(descendants=true) // or simply process.isAlive if you do not care about decendants
process.getExitValue
process.getStdout  // unless you discarded the output in the builder or redirect to file
process.getStdErr  // unless you discarded the output in the builder or redirect to file


// you can also
new CommandProcessBuilder(...).
    .collectStdOut(new File(....)) // collect the output in a file directly
    .discardStdOut() //discard the output 
   
// every stdout command is mirrored for stderr 
```

### 
