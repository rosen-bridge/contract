package rosen.bridge

import info.BuildInfo
import helpers.Utils
import scopt.OptionParser

case class Config(
                   networkType: String = "",
                   networkVersion: String = "",
                   mode: String = ""
                 )

object RosenContractsExecutor extends App {

  val parser: OptionParser[Config] = new OptionParser[Config]("RosenContracts") {
    cmd("contracts")
      .action((_, c) => c.copy(mode = "contracts"))
      .text("generate contract file.")
      .children(

        opt[String]('t', "type")
          .action((x, c) => c.copy(networkType = x.toLowerCase()))
          .text("network type")
          .required(),

        opt[String]('v', "version")
          .action((x, c) => c.copy(networkVersion = x))
          .text("Contracts file version")
          .optional()
      )

    cmd("tokens")
      .action((_, c) => c.copy(mode = "tokens"))
      .text("generate tokens file.")
      .children(
        opt[String]('t', "type")
          .action((x, c) => c.copy(networkType = x.toLowerCase()))
          .text("network type")
          .required(),

        opt[String]('v', "version")
          .action((x, c) => c.copy(networkVersion = x))
          .text("tokens file version")
          .optional()
      )

    cmd("all")
      .action((_, c) => c.copy(mode = "all"))
      .text("generate contracts and tokens file for all or specific type.")
      .children(
        opt[String]('f', "filter")
          .action((x, c) => c.copy(networkType = x.toLowerCase()))
          .text("network type filter")
          .optional(),

        opt[String]('v', "version")
          .action((x, c) => c.copy(networkVersion = x))
          .text("files version")
          .optional()
      )

    help("help").text("prints this usage text")
  }

  parser.parse(args, Config()) match {
    case Some(config) =>
        val networkVersion = if (config.networkVersion.nonEmpty) config.networkVersion else BuildInfo.version
        if (config.mode == "contracts") {
          Utils.createContracts(networkVersion, config.networkType)
          System.exit(0)
        }
        else if (config.mode == "tokens") {
          Utils.createTokenMap(networkVersion, config.networkType)
          System.exit(0)
        }
        else if (config.mode == "all") {
          Utils.createContracts(networkVersion, config.networkType)
          Utils.createTokenMap(networkVersion, config.networkType)
          System.exit(0)
      }

    case None =>
      // arguments are bad, error message will have been displayed
      System.exit(0)
  }
}

