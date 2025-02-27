package rosen.bridge

import io.circe.{Json, parser}

import java.io.{File, PrintWriter}
import scala.io.Source

object TokensMap {

  /**
   * get list of files with specific extensions
   * @param dir target directory
   * @param extensions ex: ["json", "txt"]
   * @return
   */
  def getListOfFiles(dir: File, extensions: List[String]): List[File] = {
    dir.listFiles.filter(_.isFile).toList.filter { file =>
      extensions.exists(file.getName.endsWith(_))
    }
  }

  /**
   * read file of Tokens from a dir and create Json
   * @param filePath ex: "./tokensMap"
   * @param supportedFileTypes list of string ex: List("testnet.json")
   * @return
   */
  def readTokensFromFiles(filePath: String, supportedFileTypes: List[String]): Json = {
    try {
      new File(filePath).exists
      val files = getListOfFiles(new File(filePath), supportedFileTypes)
      if(files.isEmpty) throw new Throwable(s"""There aren't file with these ${supportedFileTypes} extensions.""")
      var jsonData: String = ""
      files.foreach(file => {
        val jsonFile: String = file.toString
        if (jsonData == "") jsonData = Source.fromFile(jsonFile).mkString
        else jsonData = jsonData ++ "," ++ Source.fromFile(jsonFile).mkString
      })
      val fixJson = s"""{ \"tokens\": [ $jsonData ] }"""
      parser.parse(fixJson).getOrElse(
        Json.fromFields(
          List(
            ("tokens", Json.fromFields(List.empty))
          )
        ))
    }
    catch {
      case _: Exception =>
        println(s"""Please create directory ${filePath} and put your token inside that.""")
        scala.sys.exit()
      case e: Throwable =>
        println(e)
        scala.sys.exit()
    }
  }

  /**
   * Write data ro a json file
   * @param data appended tokens' file
   * @param networkType ex: "testnet"
   * @param networkVersion ex: "1.0.0"
   */
  def createTokensMapJsonFile(data: String, networkType: String, networkVersion: String): Unit = {
    new PrintWriter(s"tokensMap-${networkType}-${networkVersion}.json") {
      write(data)
      close()
    }
  }
}
