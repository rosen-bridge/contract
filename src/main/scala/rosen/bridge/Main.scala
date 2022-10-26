package rosen.bridge

import helpers.Utils
import scopt.OptionParser

case class Config(
                   networkName: String = "",
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
        opt[String]('n', "network")
          .action((x, c) => c.copy(networkName = x.toLowerCase()))
          .text("network name")
          .required(),

        opt[String]('t', "type")
          .action((x, c) => c.copy(networkType = x.toLowerCase()))
          .text("network type")
          .required(),

        opt[String]('v', "version")
          .action((x, c) => c.copy(networkVersion = x))
          .text("Contracts file version")
          .required()
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
          .required()
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
          .required()
      )

    help("help").text("prints this usage text")
  }

  parser.parse(args, Config()) match {
    case Some(config) =>
      if (config.mode == "contracts") {
        Utils.createContracts(config.networkVersion, config.networkName, config.networkType)
        System.exit(0)
      }
      else if (config.mode == "tokens") {
        Utils.createTokenMap(config.networkVersion, config.networkType)
        System.exit(0)
      }
      else if (config.mode == "all") {
        Utils.createContracts(config.networkVersion, networkType = config.networkType)
        Utils.createTokenMap(config.networkVersion, config.networkType)
        System.exit(0)
      }

    case None =>
      // arguments are bad, error message will have been displayed
      System.exit(0)
  }
}

