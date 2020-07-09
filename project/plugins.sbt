logLevel := Level.Warn

addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.25.0")

addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.4.0")

// dependencyUpdates: show a list of project dependencies that can be updated
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.4")
