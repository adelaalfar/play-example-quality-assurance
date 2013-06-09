import sbt._
import Keys._
import play.Project._

object QualityAssurance {

  def all: Seq[sbt.Project.Setting[_]] = List(
    CheckStyleSettings.all,
    FindbugsSettings.all
  ).flatten

  object CheckStyleSettings {

    val checkStyle = TaskKey[Unit]("checkstyle", "run CheckStyle")
    val checkStyleTask = checkStyle <<=
      (streams, baseDirectory, sourceDirectory in Compile, target) map {
        (streams, base, src, target) =>
        import com.puppycrawl.tools.checkstyle.Main.{ main => CsMain }
        import streams.log
        val outputFile = (target / "checkstyle-report.txt").getAbsolutePath

        val args = List(
          "-c", (base / "project" / "checkstyle-config.xml").getAbsolutePath,
          "-f", "plain",
          "-r", src.getAbsolutePath,
          "-o", outputFile
        )
        log info ("Running checkstyle...")
        trappingExits {
          CsMain(args.toArray)
        }
        // Print out results.
        val source = scala.io.Source.fromFile(outputFile)
        log info (source.mkString)
        source.close()
      }

    val all = Seq(checkStyleTask)
  }


  object FindbugsSettings {

    val findbugs = TaskKey[Unit]("findbugs", "run FindBugs")
    val findbugsTask = findbugs <<=
      (streams, baseDirectory, sourceDirectory in Compile, target) map {
        (streams, base, src, target) =>
        import edu.umd.cs.findbugs.FindBugs2.{ main => FindbugsMain }
        import streams.log
        val outputFile = (target / "findbugs-report.txt").getAbsolutePath

        val args = List(
            "-textui",
            "-sourcepath", "app",
            "-output", outputFile,
            "target/scala-2.10/classes"
        )
        log info ("Running FindBugs...")
        trappingExits {
          FindbugsMain(args.toArray)
        }
        // Print out results.
        val source = scala.io.Source.fromFile(outputFile)
        log info (source.mkString)
        source.close()
      }

    val all = Seq(findbugsTask)
  }

  def trappingExits(thunk: => Unit): Unit = {
    val originalSecManager = System.getSecurityManager
    case class NoExitsException() extends SecurityException
    System setSecurityManager new SecurityManager() {
      import java.security.Permission
      override def checkPermission(perm: Permission) {
        if (perm.getName startsWith "exitVM") throw NoExitsException()
      }
    }
    try {
      thunk
    } catch {
      case _: NoExitsException =>
      case e => throw e
    } finally {
      System setSecurityManager originalSecManager
    }
  }
}
