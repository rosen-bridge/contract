package rosen.bridge

import helpers.{Configs, Utils}
import org.ergoplatform.appkit.{ConstantsBuilder, ErgoContract}
import scorex.crypto.hash.Digest32
import scorex.util.encode.{Base16, Base64}

object Contracts {
  lazy val RWTRepo: ErgoContract = generateRWTRepoContract()
  lazy val WatcherPermit: ErgoContract = generateWatcherPermitContract()
  lazy val Commitment: ErgoContract = generateCommitmentContract()
  lazy val WatcherTriggerEvent: ErgoContract = generateWatcherTriggerEventContract()
  lazy val Fraud: ErgoContract = generateFraudContract()
  lazy val lock: ErgoContract = generateLockContract()
  lazy val guardSign: ErgoContract = generateGuardSignContract()

  def getContractScriptHash(contract: ErgoContract): Digest32 = {
    scorex.crypto.hash.Blake2b256(contract.getErgoTree.bytes)
  }

  private def generateRWTRepoContract(): ErgoContract = {
    Configs.ergoClient.execute(ctx => {
      val watcherPermitHash = Base64.encode(getContractScriptHash(WatcherPermit))
      val RwtRepoScript = Scripts.RwtRepoScript
        .replace("GUARD_NFT", Base64.encode(Base16.decode(Configs.tokens.GuardNFT).get))
        .replace("RSN_TOKEN", Base64.encode(Base16.decode(Configs.tokens.RSN).get))
        .replace("PERMIT_SCRIPT_HASH", watcherPermitHash)
      val contract = ctx.compileContract(ConstantsBuilder.create().build(), RwtRepoScript)
      val address = Utils.getContractAddress(contract)
      println(s"Watcher repo address is : \t\t\t$address")
      contract
    })
  }

  private def generateWatcherPermitContract(): ErgoContract = {
    Configs.ergoClient.execute(ctx => {
      val commitmentHash = Base64.encode(getContractScriptHash(Commitment))
      val watcherPermitScript = Scripts.WatcherPermitScript
        .replace("REPO_NFT", Base64.encode(Base16.decode(Configs.tokens.RepoNFT).get))
        .replace("COMMITMENT_SCRIPT_HASH", commitmentHash)

      val contract = ctx.compileContract(ConstantsBuilder.create().build(), watcherPermitScript)
      val address = Utils.getContractAddress(contract)
      println(s"Watcher permit address is : \t\t\t$address")
      contract
    })
  }

  private def generateCommitmentContract(): ErgoContract = {
    Configs.ergoClient.execute(ctx => {
      val triggerEvent = Base64.encode(getContractScriptHash(WatcherTriggerEvent))
      val commitmentScript = Scripts.CommitmentScript
        .replace("REPO_NFT", Base64.encode(Base16.decode(Configs.tokens.RepoNFT).get))
        .replace("EVENT_TRIGGER_SCRIPT_HASH", triggerEvent)

      val contract = ctx.compileContract(ConstantsBuilder.create().build(), commitmentScript)
      val address = Utils.getContractAddress(contract)
      println(s"Commitment address is : \t\t\t$address")
      contract
    })
  }

  private def generateWatcherTriggerEventContract(): ErgoContract = {
    Configs.ergoClient.execute(ctx => {
      val fraud = Base64.encode(getContractScriptHash(Fraud))
      val triggerScript = Scripts.EventTriggerScript
        .replace("CLEANUP_NFT", Base64.encode(Base16.decode(Configs.tokens.CleanupNFT).get))
        .replace("GUARD_NFT", Base64.encode(Base16.decode(Configs.tokens.GuardNFT).get))
        .replace("FRAUD_SCRIPT_HASH", fraud)
        .replace("CLEANUP_CONFIRMATION", Configs.cleanupConfirm.toString)

      val contract = ctx.compileContract(ConstantsBuilder.create().build(), triggerScript)
      val address = Utils.getContractAddress(contract)
      println(s"Watcher trigger event address is : \t\t\t$address")
      contract
    })
  }

  private def generateFraudContract(): ErgoContract = {
    Configs.ergoClient.execute(ctx => {
      val fraudScript = Scripts.FraudScript
        .replace("CLEANUP_NFT", Base64.encode(Base16.decode(Configs.tokens.CleanupNFT).get))
        .replace("REPO_NFT", Base64.encode(Base16.decode(Configs.tokens.RepoNFT).get))

      val contract = ctx.compileContract(ConstantsBuilder.create().build(), fraudScript)
      val address = Utils.getContractAddress(contract)
      println(s"Fraud address is : \t\t\t$address")
      contract
    })
  }

  private def generateLockContract(): ErgoContract = {
    Configs.ergoClient.execute(ctx => {
      val lockScript = Scripts.LockScript
        .replace("GUARD_NFT", Base64.encode(Base16.decode(Configs.tokens.GuardNFT).get))

      val contract = ctx.compileContract(ConstantsBuilder.create().build(), lockScript)
      val address = Utils.getContractAddress(contract)
      println(s"lock address is : \t\t\t$address")
      contract
    })
  }

  private def generateGuardSignContract(): ErgoContract = {
    Configs.ergoClient.execute(ctx => {
      val guardSignScript = Scripts.GuardSignScript

      val contract = ctx.compileContract(ConstantsBuilder.create().build(), guardSignScript)
      val address = Utils.getContractAddress(contract)
      println(s"lock address is : \t\t\t$address")
      contract
    })
  }
}
