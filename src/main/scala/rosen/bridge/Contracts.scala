package rosen.bridge

import helpers.{Configs, MainTokens, Network, Tokens, Utils}
import org.ergoplatform.appkit.{ConstantsBuilder, ErgoContract}
import scorex.crypto.hash.Digest32
import scorex.util.encode.{Base16, Base64}
import io.circe.Json

import java.io.PrintWriter

class Contracts(networkConfig: (Network, MainTokens)) {
  lazy val RWTRepo: (ErgoContract, String) = generateRWTRepoContract()
  lazy val WatcherPermit: (ErgoContract, String) = generateWatcherPermitContract()
  lazy val Commitment: (ErgoContract, String) = generateCommitmentContract()
  lazy val WatcherTriggerEvent: (ErgoContract, String) = generateWatcherTriggerEventContract()
  lazy val Fraud: (ErgoContract, String) = generateFraudContract()
  lazy val lock: (ErgoContract, String) = generateLockContract()
  lazy val guardSign: (ErgoContract, String) = generateGuardSignContract()

  def toJsonAddresses: Json = {
    Json.fromFields(List(
      ("RWTRepo", Json.fromString(RWTRepo._2)),
      ("WatcherPermit", Json.fromString(WatcherPermit._2)),
      ("Fraud", Json.fromString(Fraud._2)),
      ("lock", Json.fromString(lock._2)),
      ("guardSign", Json.fromString(guardSign._2)),
      ("Commitment", Json.fromString(Commitment._2)),
      ("WatcherTriggerEvent", Json.fromString(WatcherTriggerEvent._2))
    ))
  }

  def getContractScriptHash(contract: ErgoContract): Digest32 = {
    scorex.crypto.hash.Blake2b256(contract.getErgoTree.bytes)
  }

  def createContractsJson(networkName: String, networkType: String, networkVersion: String): Unit = {
    val result = {
      Json.fromFields(List(
        ("addresses", this.toJsonAddresses),
        ("tokens", networkConfig._1.tokens.toJson().deepMerge(networkConfig._2.toJson())),
        ("cleanupConfirm", Json.fromInt(networkConfig._1.cleanupConfirm))
      ))
    }

    new PrintWriter(s"contracts-${networkName}-${networkType}-${networkVersion}.json") {
      write(result.toString())
      close()
    }
  }

  private def generateRWTRepoContract(): (ErgoContract, String) = {
    Configs.ergoClient.execute(ctx => {
      val watcherPermitHash = Base64.encode(getContractScriptHash(WatcherPermit._1))
      val RwtRepoScript = Scripts.RwtRepoScript
        .replace("GUARD_NFT", Base64.encode(Base16.decode(networkConfig._2.GuardNFT).get))
        .replace("RSN_TOKEN", Base64.encode(Base16.decode(networkConfig._2.RSN).get))
        .replace("PERMIT_SCRIPT_HASH", watcherPermitHash)
      val contract = ctx.compileContract(ConstantsBuilder.create().build(), RwtRepoScript)
      val address = Utils.getContractAddress(contract)
      println(s"Watcher repo address is : \t\t\t$address")
      (contract, address)
    })
  }

  private def generateWatcherPermitContract(): (ErgoContract, String) = {
    Configs.ergoClient.execute(ctx => {
      val commitmentHash = Base64.encode(getContractScriptHash(Commitment._1))
      val watcherPermitScript = Scripts.WatcherPermitScript
        .replace("REPO_NFT", Base64.encode(Base16.decode(networkConfig._2.RepoNFT).get))
        .replace("COMMITMENT_SCRIPT_HASH", commitmentHash)

      val contract = ctx.compileContract(ConstantsBuilder.create().build(), watcherPermitScript)
      val address = Utils.getContractAddress(contract)
      println(s"Watcher permit address is : \t\t\t$address")
      (contract, address)
    })
  }

  private def generateCommitmentContract(): (ErgoContract, String) = {
    Configs.ergoClient.execute(ctx => {
      val triggerEvent = Base64.encode(getContractScriptHash(WatcherTriggerEvent._1))
      val commitmentScript = Scripts.CommitmentScript
        .replace("REPO_NFT", Base64.encode(Base16.decode(networkConfig._2.RepoNFT).get))
        .replace("EVENT_TRIGGER_SCRIPT_HASH", triggerEvent)

      val contract = ctx.compileContract(ConstantsBuilder.create().build(), commitmentScript)
      val address = Utils.getContractAddress(contract)
      println(s"Commitment address is : \t\t\t$address")
      (contract, address)
    })
  }

  private def generateWatcherTriggerEventContract(): (ErgoContract, String) = {
    Configs.ergoClient.execute(ctx => {
      val fraud = Base64.encode(getContractScriptHash(Fraud._1))
      val triggerScript = Scripts.EventTriggerScript
        .replace("CLEANUP_NFT", Base64.encode(Base16.decode(networkConfig._1.tokens.CleanupNFT).get))
        .replace("GUARD_NFT", Base64.encode(Base16.decode(networkConfig._2.GuardNFT).get))
        .replace("FRAUD_SCRIPT_HASH", fraud)
        .replace("CLEANUP_CONFIRMATION", networkConfig._1.cleanupConfirm.toString)

      val contract = ctx.compileContract(ConstantsBuilder.create().build(), triggerScript)
      val address = Utils.getContractAddress(contract)
      println(s"Watcher trigger event address is : \t\t\t$address")
      (contract, address)
    })
  }

  private def generateFraudContract(): (ErgoContract, String) = {
    Configs.ergoClient.execute(ctx => {
      val fraudScript = Scripts.FraudScript
        .replace("CLEANUP_NFT", Base64.encode(Base16.decode(networkConfig._1.tokens.CleanupNFT).get))
        .replace("REPO_NFT", Base64.encode(Base16.decode(networkConfig._2.RepoNFT).get))

      val contract = ctx.compileContract(ConstantsBuilder.create().build(), fraudScript)
      val address = Utils.getContractAddress(contract)
      println(s"Fraud address is : \t\t\t$address")
      (contract, address)
    })
  }

  private def generateLockContract(): (ErgoContract, String) = {
    Configs.ergoClient.execute(ctx => {
      val lockScript = Scripts.LockScript
        .replace("GUARD_NFT", Base64.encode(Base16.decode(networkConfig._2.GuardNFT).get))

      val contract = ctx.compileContract(ConstantsBuilder.create().build(), lockScript)
      val address = Utils.getContractAddress(contract)
      println(s"lock address is : \t\t\t$address")
      (contract, address)
    })
  }

  private def generateGuardSignContract(): (ErgoContract, String) = {
    Configs.ergoClient.execute(ctx => {
      val guardSignScript = Scripts.GuardSignScript

      val contract = ctx.compileContract(ConstantsBuilder.create().build(), guardSignScript)
      val address = Utils.getContractAddress(contract)
      println(s"guard sign address is : \t\t\t$address")
      (contract, address)
    })
  }
}