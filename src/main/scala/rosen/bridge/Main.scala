package rosen.bridge

import helpers.{MainTokens, Tokens, Utils}
import scopt.OptionParser

import java.io.PrintWriter

case class Config(
                   networkName: String = "",
                   networkType: String = "",
                   networkVersion: String = ""

                 )

object RosenContractsExecutor extends App {

  def createJson(networkName: String, networkType: String, networkVersion: String, contracts: Contracts, mainTokens: MainTokens, tokens: Tokens): Unit = {

    val data =
      s"""
         |{
         |  "addresses": ${contracts.toJsonAddresses},
         |  "tokens": ${tokens.toJson()},
         |  "mainTokens": ${mainTokens.toJson()}
         |}
         |""".stripMargin

    new PrintWriter(s"${networkName}-${networkType}-${networkVersion}.json") {
      write(data)
      close()
    }

  }

  val parser: OptionParser[Config] = new OptionParser[Config]("RosenContracts") {
    opt[String]('n', "network")
      .action((x, c) => c.copy(networkName = x.toLowerCase()))
      .text("network name")
      .required()

    opt[String]('t', "type")
      .action((x, c) => c.copy(networkType = x.toLowerCase()))
      .text("network type")
      .required()

    opt[String]('v', "version")
      .action((x, c) => c.copy(networkVersion = x))
      .text("Contracts version")
      .required()

    help("help").text("prints this usage text")
  }

  parser.parse(args, Config()) match {
    case Some(config) =>
      val networkConfig: (Tokens, MainTokens) = Utils.selectConfig(config.networkName, config.networkType)
      val contracts = new Contracts(networkConfig)
      createJson(
        config.networkName,
        config.networkType,
        config.networkVersion,
        contracts,
        networkConfig._2,
        networkConfig._1
      )

    case None =>
    // arguments are bad, error message will have been displayed
  }
}

