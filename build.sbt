seq(samskivert.POMUtil.pomToSettings("pom.xml") :_*)

crossPaths := false

javacOptions ++= Seq(
  "-Xlint", "-Xlint:-serial", "-source", "1.6", "-target", "1.6"
)

autoScalaLibrary := false // no scala-library dependency

libraryDependencies += "com.novocode" % "junit-interface" % "0.7" % "test->default"

// filter the super-source directory from the build
unmanagedSources in Compile ~= (_.filterNot(_.getPath.indexOf("com/samskivert/gwt") != -1))

// add our sources to the main jar file (including super-sources)
unmanagedResourceDirectories in Compile <+= baseDirectory / "src/main/java"
