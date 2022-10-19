package rosen.bridge

import helpers.{Configs, ErgoNetwork, MainTokens, Network, Utils}
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

    help("help").text("prints this usage text")
  }

  parser.parse(args, Config()) match {
    case Some(config) =>
      if (config.mode == "contracts") {
        val networkConfig: (ErgoNetwork, Network, MainTokens) = Utils.selectConfig(config.networkName, config.networkType)
        val contracts = new Contracts(networkConfig._1, (networkConfig._2, networkConfig._3))
        contracts.createContractsJson(
          config.networkName,
          config.networkType,
          config.networkVersion,
        )
        println("Json of Contracts created!")
        scala.sys.exit()
      }
      else if (config.mode == "tokens") {
        val fileType = config.networkType ++ ".json"
        val tokensMap = TokensMap.readTokensFromFiles(Configs.tokensMapDirPath, List(fileType))
        val idKeys = TokensMap.createIdKeysJson()
        TokensMap.createTokensMapJsonFile(tokensMap.deepMerge(idKeys).toString(), config.networkType, config.networkVersion)
        println("Json of TokensMap created!")
        scala.sys.exit()
      }

    case None =>
      // arguments are bad, error message will have been displayed
      scala.sys.exit()
  }
}

