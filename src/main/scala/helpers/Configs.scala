package helpers

import com.typesafe.config.{Config, ConfigFactory, ConfigObject}
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{ErgoClient, NetworkType, RestApiErgoClient}

import scala.collection.mutable

trait ConfigHelper {
  val config: Config  = ConfigFactory.load()

  /**
   * Read the config and return the value of the key
   *
   * @param key     key to find
   * @param default default value if the key is not found
   * @return value of the key
   */
  def readKey(key: String, default: String = null): String = {
    try {
      config.getString(key)
    } catch {
      case _: Throwable =>
        println(s"$key is required.")
        sys.exit()
    }
  }

  def readKeyDynamic(config: ConfigObject, key: String, default: String = null): String = {
    try {
      if(config.containsKey(key)) config.toConfig.getString(key)
      else if(default.nonEmpty) default
      else throw new Throwable(s"${key} not found!")
    } catch {
      case ex: Throwable =>
        println(ex.getMessage)
        null
    }
  }
}


object Configs extends ConfigHelper {
  object node {
    lazy val apiKey: String = readKey("node.apiKey")
    lazy val url: String = readKey("node.url")
    lazy val networkType: NetworkType = if (readKey("node.networkType").toLowerCase.equals("mainnet")) NetworkType.MAINNET else NetworkType.TESTNET
  }
  private lazy val explorerUrlConf = readKey("explorer.url", "")
  lazy val explorer: String = if (explorerUrlConf.isEmpty) RestApiErgoClient.getDefaultExplorerUrl(node.networkType) else explorerUrlConf
  lazy val fee: Long = readKey("fee.default", "1000000").toLong
  lazy val maxFee: Long = readKey("fee.max", "1000000").toLong
  lazy val minBoxValue: Long = readKey("box.min").toLong
  val ergoClient: ErgoClient = RestApiErgoClient.create(node.url, node.networkType, node.apiKey, explorer)
  lazy val addressEncoder = new ErgoAddressEncoder(node.networkType.networkPrefix)

  private lazy val ergoTokensConfig = config.getObject("tokens")
  var allNetworksToken = mutable.Map.empty[(String, String), Tokens]
  ergoTokensConfig.keySet().forEach(networkName => {
    val networkConfig = ergoTokensConfig.get(networkName).asInstanceOf[ConfigObject]
    networkConfig.keySet().forEach(networkType => {
      val networkDataConfig = networkConfig.get(networkType).asInstanceOf[ConfigObject]
      allNetworksToken((networkName, networkType)) = Tokens(
        readKeyDynamic(networkDataConfig, "RWTId"),
        readKeyDynamic(networkDataConfig, "CleanupNFT"),
        readKeyDynamic(networkDataConfig, "cleanup-confirm").toInt
      )
    })
  })

  private lazy val ergoMainTokensConfig = config.getObject("main-tokens")
  var mainTokens = mutable.Map.empty[String, MainTokens]
  ergoMainTokensConfig.keySet().forEach(networkType => {
    val mainTokenConfig = ergoMainTokensConfig.get(networkType).asInstanceOf[ConfigObject]
    mainTokens(networkType) = MainTokens(
      readKeyDynamic(mainTokenConfig, "RepoNFT"),
      readKeyDynamic(mainTokenConfig, "GuardNFT"),
      readKeyDynamic(mainTokenConfig, "RSN")
    )
  })
}
