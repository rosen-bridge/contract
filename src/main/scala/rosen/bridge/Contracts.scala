package rosen.bridge

import helpers.{Network, NetworkGeneral, Utils}
import org.ergoplatform.appkit.{ConstantsBuilder, ErgoContract}
import scorex.util.encode.{Base16, Base64}
import io.circe.Json

import java.io.PrintWriter
import scala.io.{BufferedSource, Source}

class Contracts(networkGeneral: NetworkGeneral, networkConfig: Network) {
  lazy val WatcherCollateral: (ErgoContract, String) = generateWatcherCollateralContract()
  lazy val RWTRepo: (ErgoContract, String) = generateRWTRepoContract()
  lazy val WatcherPermit: (ErgoContract, String) = generateWatcherPermitContract()
  lazy val Commitment: (ErgoContract, String) = generateCommitmentContract()
  lazy val WatcherTriggerEvent: (ErgoContract, String) = generateWatcherTriggerEventContract()
  lazy val Fraud: (ErgoContract, String) = generateFraudContract()
  lazy val Lock: (ErgoContract, String) = generateLockContract()
  lazy val GuardSign: (ErgoContract, String) = generateGuardSignContract()
  lazy val RepoConfig: (ErgoContract, String) = generateRepoConfigContract()

  def readScript(path: String) = {
    val scriptSource: BufferedSource = Source.fromFile("src/main/scala/rosen/bridge/scripts/" + path, "utf-8")
    val script: String = scriptSource.getLines.mkString("\n")
    scriptSource.close()
    script
  }

  def toJsonAddresses(networkName: String): Json = {
    val lockAddress = if (networkName != "ergo") networkConfig.lockAddress else Lock._2
    Json.fromFields(List(
      ("RWTRepo", Json.fromString(RWTRepo._2)),
      ("WatcherPermit", Json.fromString(WatcherPermit._2)),
      ("Fraud", Json.fromString(Fraud._2)),
      ("lock", Json.fromString(lockAddress)),
      ("guardSign", Json.fromString(GuardSign._2)),
      ("Commitment", Json.fromString(Commitment._2)),
      ("WatcherTriggerEvent", Json.fromString(WatcherTriggerEvent._2)),
      ("WatcherCollateral", Json.fromString(WatcherCollateral._2)),
      ("RepoConfig", Json.fromString(RepoConfig._2)),
      ("MinimumFeeAddress", Json.fromString(networkGeneral.minimumFeeAddress)),
    ))
  }

  def createContractsJson(networkName: String, networkType: String, networkVersion: String): Unit = {
    val result = {
      Json.fromFields(List(
        ("addresses", this.toJsonAddresses(networkName)),
        ("tokens", networkConfig.tokens.toJson().deepMerge(networkGeneral.mainTokens.toJson())),
        ("cleanupConfirm", Json.fromInt(networkConfig.cleanupConfirm))
      ))
    }

    new PrintWriter(s"contracts-${networkName}-${networkType}-${networkVersion}.json") {
      write(result.toString())
      close()
    }
  }

  private def generateWatcherCollateralContract(): (ErgoContract, String) = {
    networkGeneral.ergoNetwork.ergoClient.execute(ctx => {
      val watcherCollateralScript = readScript("Collateral.es")
        .replace("REPO_NFT", Base64.encode(Base16.decode(networkGeneral.mainTokens.RepoNFT).get))
      val contract = ctx.compileContract(ConstantsBuilder.create().build(), watcherCollateralScript)
      val address = Utils.getContractAddress(contract, networkGeneral.ergoNetwork.addressEncoder)
      println(s"Watcher collateral address is : \t\t\t$address")
      (contract, address)
    })
  }

  private def generateRWTRepoContract(): (ErgoContract, String) = {
    networkGeneral.ergoNetwork.ergoClient.execute(ctx => {
      val watcherPermitHash = Base64.encode(Utils.getContractScriptHash(WatcherPermit._1))
      val watcherCollateralHash = Base64.encode(Utils.getContractScriptHash(WatcherCollateral._1))
      val RwtRepoScript = readScript("RwtRepo.es")
        .replace("REPO_CONFIG_NFT", Base64.encode(Base16.decode(networkConfig.tokens.RepoConfigNFT).get))
        .replace("RSN_TOKEN", Base64.encode(Base16.decode(networkGeneral.mainTokens.RSN).get))
        .replace("PERMIT_SCRIPT_HASH", watcherPermitHash)
        .replace("WATCHER_COLLATERAL_SCRIPT_HASH", watcherCollateralHash)
      val contract = ctx.compileContract(ConstantsBuilder.create().build(), RwtRepoScript)
      val address = Utils.getContractAddress(contract, networkGeneral.ergoNetwork.addressEncoder)
      println(s"Watcher repo address is : \t\t\t$address")
      (contract, address)
    })
  }

  private def generateWatcherPermitContract(): (ErgoContract, String) = {
    networkGeneral.ergoNetwork.ergoClient.execute(ctx => {
      val commitmentHash = Base64.encode(Utils.getContractScriptHash(Commitment._1))
      val watcherPermitScript = readScript("Permit.es")
        .replace("REPO_NFT", Base64.encode(Base16.decode(networkGeneral.mainTokens.RepoNFT).get))
        .replace("COMMITMENT_SCRIPT_HASH", commitmentHash)

      val contract = ctx.compileContract(ConstantsBuilder.create().build(), watcherPermitScript)
      val address = Utils.getContractAddress(contract, networkGeneral.ergoNetwork.addressEncoder)
      println(s"Watcher permit address is : \t\t\t$address")
      (contract, address)
    })
  }

  private def generateCommitmentContract(): (ErgoContract, String) = {
    networkGeneral.ergoNetwork.ergoClient.execute(ctx => {
      val triggerEvent = Base64.encode(Utils.getContractScriptHash(WatcherTriggerEvent._1))
      val commitmentScript = readScript("Commitment.es")
        .replace("REPO_NFT", Base64.encode(Base16.decode(networkGeneral.mainTokens.RepoNFT).get))
        .replace("REPO_CONFIG_NFT", Base64.encode(Base16.decode(networkConfig.tokens.RepoConfigNFT).get))
        .replace("EVENT_TRIGGER_SCRIPT_HASH", triggerEvent)

      val contract = ctx.compileContract(ConstantsBuilder.create().build(), commitmentScript)
      val address = Utils.getContractAddress(contract, networkGeneral.ergoNetwork.addressEncoder)
      println(s"Commitment address is : \t\t\t$address")
      (contract, address)
    })
  }

  private def generateWatcherTriggerEventContract(): (ErgoContract, String) = {
    networkGeneral.ergoNetwork.ergoClient.execute(ctx => {
      val fraud = Base64.encode(Utils.getContractScriptHash(Fraud._1))
      val lock = Base64.encode(Utils.getContractScriptHash(Lock._1))
      val triggerScript = readScript("EventTrigger.es")
        .replace("CLEANUP_NFT", Base64.encode(Base16.decode(networkConfig.tokens.CleanupNFT).get))
        .replace("LOCK_SCRIPT_HASH", lock)
        .replace("FRAUD_SCRIPT_HASH", fraud)
        .replace("CLEANUP_CONFIRMATION", networkConfig.cleanupConfirm.toString)

      val contract = ctx.compileContract(ConstantsBuilder.create().build(), triggerScript)
      val address = Utils.getContractAddress(contract, networkGeneral.ergoNetwork.addressEncoder)
      println(s"Watcher trigger event address is : \t\t\t$address")
      (contract, address)
    })
  }

  private def generateFraudContract(): (ErgoContract, String) = {
    networkGeneral.ergoNetwork.ergoClient.execute(ctx => {
      val fraudScript = readScript("Fraud.es")
        .replace("CLEANUP_NFT", Base64.encode(Base16.decode(networkConfig.tokens.CleanupNFT).get))
        .replace("REPO_NFT", Base64.encode(Base16.decode(networkGeneral.mainTokens.RepoNFT).get))

      val contract = ctx.compileContract(ConstantsBuilder.create().build(), fraudScript)
      val address = Utils.getContractAddress(contract, networkGeneral.ergoNetwork.addressEncoder)
      println(s"Fraud address is : \t\t\t$address")
      (contract, address)
    })
  }

  private def generateLockContract(): (ErgoContract, String) = {
    networkGeneral.ergoNetwork.ergoClient.execute(ctx => {
      val lockScript = readScript("Lock.es")
        .replace("GUARD_NFT", Base64.encode(Base16.decode(networkGeneral.mainTokens.GuardNFT).get))

      val contract = ctx.compileContract(ConstantsBuilder.create().build(), lockScript)
      val address = Utils.getContractAddress(contract, networkGeneral.ergoNetwork.addressEncoder)
      println(s"lock address is : \t\t\t$address")
      (contract, address)
    })
  }

  private def generateGuardSignContract(): (ErgoContract, String) = {
    networkGeneral.ergoNetwork.ergoClient.execute(ctx => {
      val guardSignScript = readScript("GuardSign.es")

      val contract = ctx.compileContract(ConstantsBuilder.create().build(), guardSignScript)
      val address = Utils.getContractAddress(contract, networkGeneral.ergoNetwork.addressEncoder)
      println(s"guard sign address is : \t\t\t$address")
      (contract, address)
    })
  }

  private def generateRepoConfigContract(): (ErgoContract, String) = {
    networkGeneral.ergoNetwork.ergoClient.execute(ctx => {
      val testScript = readScript("RepoConfig.es")
        .replace("GUARD_NFT", Base64.encode(Base16.decode(networkGeneral.mainTokens.GuardNFT).get))
      
      val contract = ctx.compileContract(ConstantsBuilder.create().build(), testScript)
      val address = Utils.getContractAddress(contract, networkGeneral.ergoNetwork.addressEncoder)
      println(s"repo config address is : \t\t\t$address")
      (contract, address)
    })
  }
}
