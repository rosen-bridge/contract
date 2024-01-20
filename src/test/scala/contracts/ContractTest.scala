package contracts

import helpers.{Configs, ErgoNetwork, MainTokens, Network, Utils}
import org.ergoplatform.appkit.{ErgoProver, ErgoToken}
import rosen.bridge.Contracts
import scorex.util.encode.Base16
import testUtils.{Boxes, Commitment, TestSuite}


class ContractTest extends TestSuite {
  val sk: BigInt = Utils.randBigInt

  val networkConfig: (ErgoNetwork, Network, MainTokens) = Utils.selectConfig("cardano", "mainnet")
  val contracts = new Contracts(networkConfig._1, (networkConfig._2, networkConfig._3))

  def getProver(): ErgoProver = {
    networkConfig._1.ergoClient.execute(ctx => {
      ctx.newProverBuilder().withDLogSecret(sk.bigInteger).build()
    })
  }

  def generateRandomWIDList(count: Int): Seq[Array[Byte]] = {
    var res: Seq[Array[Byte]] = Seq()
    while (count > res.length) {
      res = res ++ Seq(Base16.decode(Boxes.getRandomHexString()).get)
    }
    res
  }

  property("test get permit when there is no other permit on network") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 2e9.toLong, new ErgoToken(networkConfig._3.RSN, 200L))
        val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId).convertToInputWith(Boxes.getRandomHexString(), 1)
        val repoBox = Boxes.createRepo(ctx, 100000, 1L, 100L, Seq(), Seq()).convertToInputWith(Boxes.getRandomHexString(), 0)
        val repoOut = Boxes.createRepo(ctx, 99900, 101L, 99L, Seq(repoBox.getId.getBytes), Seq(100L))
        val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
        val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 3L))
        val watcherCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, repoBox.getId.getBytes)
        val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
          .fee(Configs.fee)
          .addOutputs(repoOut, permitBox, WID, watcherCollateral)
          .addDataInputs(repoConfig)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("transaction not signed")
      }
    })
  }

  property("test get permit when there is another permit on network") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 2e9.toLong, new ErgoToken(networkConfig._3.RSN, 200L))
        val otherWID = Base16.decode(Boxes.getRandomHexString()).get
        val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId).convertToInputWith(Boxes.getRandomHexString(), 1)
        val repoBox = Boxes.createRepo(ctx, 100000, 5801L, 100L, Seq(otherWID), Seq(5800L)).convertToInputWith(Boxes.getRandomHexString(), 0)
        val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 99L, Seq(otherWID, repoBox.getId.getBytes), Seq(5800L, 100L))
        val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
        val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 3L))
        val watcherCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, repoBox.getId.getBytes)
        val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
          .fee(Configs.fee)
          .addDataInputs(repoConfig)
          .addOutputs(repoOut, permitBox, WID, watcherCollateral)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          fail("transaction not signed")
      }
    })
  }

  property("test get permit while minting just one wid") {
    networkConfig._1.ergoClient.execute(ctx => {
      assertThrows[AnyRef] {
        val prover = getProver()
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 2e9.toLong, new ErgoToken(networkConfig._3.RSN, 300L))
        val otherWID = Base16.decode(Boxes.getRandomHexString()).get
        val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId).convertToInputWith(Boxes.getRandomHexString(), 1)
        val repoBox = Boxes.createRepo(ctx, 100000, 5801L, 100L, Seq(otherWID), Seq(5800L)).convertToInputWith(Boxes.getRandomHexString(), 0)
        val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 99L, Seq(otherWID, repoBox.getId.getBytes), Seq(5800L, 100L))
        val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
        val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 1L))
        val watcherCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, repoBox.getId.getBytes)
        val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
          .fee(Configs.fee)
          .addDataInputs(repoConfig)
          .addOutputs(repoOut, permitBox, WID, watcherCollateral)
          .sendChangeTo(prover.getAddress)
          .build()
        val signedTx = prover.sign(tx)
        println(signedTx.toJson(false))
      }
    })
  }

  property("test get permit while created permit first token is not RWT") {
    networkConfig._1.ergoClient.execute(ctx => {
      val prover = getProver()
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 2e9.toLong, new ErgoToken(networkConfig._3.RSN, 300L))
      val otherWID = Base16.decode(Boxes.getRandomHexString()).get
      val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId).convertToInputWith(Boxes.getRandomHexString(), 1)
      val repoBox = Boxes.createRepo(ctx, 100000, 5801L, 100L, Seq(otherWID), Seq(5800L)).convertToInputWith(Boxes.getRandomHexString(), 0)
      val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 99L, Seq(otherWID, repoBox.getId.getBytes), Seq(5800L, 100L))
      val permitBox = Boxes.createInvalidPermitBox(ctx, 100L, repoBox.getId.getBytes)
      val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 3L))
      val watcherCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, repoBox.getId.getBytes)
      val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
        .fee(Configs.fee)
        .addDataInputs(repoConfig)
        .addOutputs(repoOut, permitBox, WID, watcherCollateral)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        val signedTx = prover.sign(tx)
        println(signedTx.toJson(false))
      }
    })
  }

  /**
   * @target get permit transaction signing should throw error when repoConfig is invalid
   * @dependencies
   * @scenario
   * - mock user input
   * - mock input repo box with 100 AWC
   * - mock invalid repoConfig box as data input
   * - mock valid output repo box with 100 AWC and collateral box
   * - mock valid output wid and permit box
   * - build and sign the get permit transaction
   * @expected
   * - sign error for invalid repo config box
   */
  property("get permit transaction signing should throw error when repoConfig is invalid") {
    networkConfig._1.ergoClient.execute(ctx => {
      val prover = getProver()
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 2e9.toLong, new ErgoToken(networkConfig._3.RSN, 200L))
      val otherWID = Base16.decode(Boxes.getRandomHexString()).get
      val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, Boxes.getRandomHexString()).convertToInputWith(Boxes.getRandomHexString(), 1)
      val repoBox = Boxes.createRepo(ctx, 100000, 5801L, 100L, Seq(otherWID), Seq(5800L)).convertToInputWith(Boxes.getRandomHexString(), 0)
      val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 99L, Seq(otherWID, repoBox.getId.getBytes), Seq(5800L, 100L))
      val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
      val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 3L))
      val watcherCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, repoBox.getId.getBytes)
      val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
        .fee(Configs.fee)
        .addDataInputs(repoConfig)
        .addOutputs(repoOut, permitBox, WID, watcherCollateral)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  /**
   * @target get permit transaction signing should throw error when creating a fake collateral not containing AWC
   * @dependencies
   * @scenario
   * - mock user input
   * - mock input repo box with 100 AWC
   * - mock valid repoConfig box as data input
   * - mock output repo box with 100 AWC and collateral box without AWC
   * - mock valid output wid and permit box
   * - build and sign the get permit transaction
   * @expected
   * - sign error for the collateral not containing AWC
   */
  property("get permit transaction signing should throw error when creating a fake collateral not containing AWC") {
    networkConfig._1.ergoClient.execute(ctx => {
      val prover = getProver()
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 2e9.toLong, new ErgoToken(networkConfig._3.RSN, 200L))
      val otherWID = Base16.decode(Boxes.getRandomHexString()).get
      val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId).convertToInputWith(Boxes.getRandomHexString(), 1)
      val repoBox = Boxes.createRepo(ctx, 100000, 5801L, 100L, Seq(otherWID), Seq(5800L)).convertToInputWith(Boxes.getRandomHexString(), 0)
      val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 100L, Seq(otherWID, repoBox.getId.getBytes), Seq(5800L, 100L))
      val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
      val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 3L))
      val watcherCollateral = Boxes.createFakeWatcherCollateralBox(ctx, 1e9.toLong, 100, repoBox.getId.getBytes)
      val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
        .fee(Configs.fee)
        .addDataInputs(repoConfig)
        .addOutputs(repoOut, permitBox, WID, watcherCollateral)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        val signedTx = prover.sign(tx)
        println(signedTx.toJson(false))
      }
    })
  }

  /**
   * @target get permit transaction signing should throw error when stealing AWC and creating fake collateral
   * @dependencies
   * @scenario
   * - mock user input
   * - mock input repo box with 100 AWC
   * - mock valid repoConfig box as data input
   * - mock output repo box with 99 AWC and collateral box without AWC
   * - mock output wid box with stolen AWC
   * - mocked valid permit box
   * - build and sign the get permit transaction
   * @expected
   * - sign error for stealing AWC and creating fake collateral
   */
  property("get permit transaction signing should throw error when stealing AWC and creating fake collateral") {
    networkConfig._1.ergoClient.execute(ctx => {
      val prover = getProver()
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 2e9.toLong, new ErgoToken(networkConfig._3.RSN, 200L))
      val otherWID = Base16.decode(Boxes.getRandomHexString()).get
      val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId).convertToInputWith(Boxes.getRandomHexString(), 1)
      val repoBox = Boxes.createRepo(ctx, 100000, 5801L, 100L, Seq(otherWID), Seq(5800L)).convertToInputWith(Boxes.getRandomHexString(), 0)
      val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 99L, Seq(otherWID, repoBox.getId.getBytes), Seq(5800L, 100L))
      val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
      val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 3L), new ErgoToken(networkConfig._2.tokens.AwcNFT, 1L))
      val watcherCollateral = Boxes.createFakeWatcherCollateralBox(ctx, 1e9.toLong, 100, repoBox.getId.getBytes)
      val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
        .fee(Configs.fee)
        .addDataInputs(repoConfig)
        .addOutputs(repoOut, permitBox, WID, watcherCollateral)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        val signedTx = prover.sign(tx)
        println(signedTx.toJson(false))
      }
    })
  }

  property("test extend permit when there is just one permit on network") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._3.RSN, 100L))
        val repoBox = Boxes.createRepo(ctx, 100000, 5801L, 100L, Seq(WID), Seq(5800L)).convertToInputWith(Boxes.getRandomHexString(), 0)
        val repoOut = Boxes.createRepoWithR6(ctx, 99900, 5901L, 100L, Seq(WID), Seq(5900L), 1)
        val permitBox = Boxes.createPermitBox(ctx, 100L, WID)
        val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 2L))
        val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
          .fee(Configs.fee)
          .addOutputs(repoOut, permitBox, WIDBox)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("transaction not signed")
      }
    })
  }

  property("test extend first permit when there is other permits on network") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val otherWID = Base16.decode(Boxes.getRandomHexString()).get
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._3.RSN, 100L))
        val repoBox = Boxes.createRepo(ctx, 100000, 6459L, 100L, Seq(WID, otherWID), Seq(58L, 6400L)).convertToInputWith(Boxes.getRandomHexString(), 0)
        val repoOut = Boxes.createRepoWithR6(ctx, 99900, 6559L, 100L, Seq(WID, otherWID), Seq(158L, 6400L), 1)
        val permitBox = Boxes.createPermitBox(ctx, 100L, WID)
        val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 2L))
        val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
          .fee(Configs.fee)
          .addOutputs(repoOut, permitBox, WIDBox)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("transaction not signed")
      }
    })
  }

  property("test extend middle permit when there is other permits on network") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val otherWID = Base16.decode(Boxes.getRandomHexString()).get
        val otherWID2 = Base16.decode(Boxes.getRandomHexString()).get
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._3.RSN, 100L))
        val repoBox = Boxes.createRepo(ctx, 100000, 9659L, 100L, Seq(otherWID, WID, otherWID2), Seq(3200L, 58L, 6400L)).convertToInputWith(Boxes.getRandomHexString(), 0)
        val repoOut = Boxes.createRepoWithR6(ctx, 99900, 9759L, 100L, Seq(otherWID, WID, otherWID2), Seq(3200L, 158L, 6400L), 2)
        val permitBox = Boxes.createPermitBox(ctx, 100L, WID)
        val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 2L))
        val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
          .fee(Configs.fee)
          .addOutputs(repoOut, permitBox, WIDBox)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("transaction not signed")
      }
    })
  }

  property("test extend permit using one wid token") {
    networkConfig._1.ergoClient.execute(ctx => {
      val prover = getProver()
      val WID = Base16.decode(Boxes.getRandomHexString()).get
      val otherWID = Base16.decode(Boxes.getRandomHexString()).get
      val otherWID2 = Base16.decode(Boxes.getRandomHexString()).get
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 1L), new ErgoToken(networkConfig._3.RSN, 100L))
      val repoBox = Boxes.createRepo(ctx, 100000, 9659L, 100L, Seq(otherWID, WID, otherWID2), Seq(3200L, 58L, 6400L)).convertToInputWith(Boxes.getRandomHexString(), 0)
      val repoOut = Boxes.createRepoWithR6(ctx, 99900, 9759L, 100L, Seq(otherWID, WID, otherWID2), Seq(3200L, 158L, 6400L), 2)
      val permitBox = Boxes.createPermitBox(ctx, 100L, WID)
      val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 1L))
      val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
        .fee(Configs.fee)
        .addOutputs(repoOut, permitBox, WIDBox)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  property("test extend first permit while extended permit wid has changed") {
    networkConfig._1.ergoClient.execute(ctx => {
      val prover = getProver()
      val WID = Base16.decode(Boxes.getRandomHexString()).get
      val otherWID = Base16.decode(Boxes.getRandomHexString()).get
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._3.RSN, 100L))
      val repoBox = Boxes.createRepo(ctx, 100000, 6459L, 100L, Seq(WID, otherWID), Seq(58L, 6400L)).convertToInputWith(Boxes.getRandomHexString(), 0)
      val repoOut = Boxes.createRepoWithR6(ctx, 99900, 6559L, 100L, Seq(WID, otherWID), Seq(158L, 6400L), 1)
      val permitBox = Boxes.createPermitBox(ctx, 100L, otherWID)
      val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 2L))
      val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
        .fee(Configs.fee)
        .addOutputs(repoOut, permitBox, WIDBox)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        val signedTx = prover.sign(tx)
        println(signedTx.toJson(false))
      }
    })
  }

  property("test extend first permit while extended permit first token is not RWT") {
    networkConfig._1.ergoClient.execute(ctx => {
      val prover = getProver()
      val WID = Base16.decode(Boxes.getRandomHexString()).get
      val otherWID = Base16.decode(Boxes.getRandomHexString()).get
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._3.RSN, 200L))
      val repoBox = Boxes.createRepo(ctx, 100000, 6459L, 100L, Seq(WID, otherWID), Seq(58L, 6400L)).convertToInputWith(Boxes.getRandomHexString(), 0)
      val repoOut = Boxes.createRepoWithR6(ctx, 99900, 6559L, 100L, Seq(WID, otherWID), Seq(158L, 6400L), 1)
      val permitBox = Boxes.createInvalidPermitBox(ctx, 100L, WID)
      val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 2L))
      val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
        .fee(Configs.fee)
        .addOutputs(repoOut, permitBox, WIDBox)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        val signedTx = prover.sign(tx)
        println(signedTx.toJson(false))
      }
    })
  }

  property("test extend permit while mutating other permits amount on repo") {
    networkConfig._1.ergoClient.execute(ctx => {
      val prover = getProver()
      val WID = Base16.decode(Boxes.getRandomHexString()).get
      val otherWID = Base16.decode(Boxes.getRandomHexString()).get
      val otherWID2 = Base16.decode(Boxes.getRandomHexString()).get
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._3.RSN, 100L))
      val repoBox = Boxes.createRepo(ctx, 100000, 9659L, 100L, Seq(otherWID, WID, otherWID2), Seq(3200L, 58L, 6400L)).convertToInputWith(Boxes.getRandomHexString(), 0)
      val repoOut = Boxes.createRepoWithR6(ctx, 99900, 9759L, 100L, Seq(otherWID, WID, otherWID2), Seq(320L, 158L, 6400L), 2)
      val permitBox = Boxes.createPermitBox(ctx, 100L, WID)
      val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 2L))
      val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
        .fee(Configs.fee)
        .addOutputs(repoOut, permitBox, WIDBox)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  property("test extend permit while mutating other permits WID on repo") {
    networkConfig._1.ergoClient.execute(ctx => {
      val prover = getProver()
      val WID = Base16.decode(Boxes.getRandomHexString()).get
      val otherWID = Base16.decode(Boxes.getRandomHexString()).get
      val otherWID2 = Base16.decode(Boxes.getRandomHexString()).get
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._3.RSN, 100L))
      val repoBox = Boxes.createRepo(ctx, 100000, 9659L, 100L, Seq(otherWID, WID, otherWID2), Seq(3200L, 58L, 6400L)).convertToInputWith(Boxes.getRandomHexString(), 0)
      val repoOut = Boxes.createRepoWithR6(ctx, 99900, 9759L, 100L, Seq(otherWID, WID, otherWID), Seq(3200L, 158L, 6400L), 2)
      val permitBox = Boxes.createPermitBox(ctx, 100L, WID)
      val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 2L))
      val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
        .fee(Configs.fee)
        .addOutputs(repoOut, permitBox, WIDBox)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  property("test partially return permits") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val userWID = Base16.decode(Boxes.getRandomHexString()).get
        val WIDs = Seq(
          Base16.decode(Boxes.getRandomHexString()).get,
          Base16.decode(Boxes.getRandomHexString()).get,
          userWID,
          Base16.decode(Boxes.getRandomHexString()).get
        )
        val repoBox = Boxes.createRepo(ctx, 100000, 321L, 100L, WIDs, Seq(100L, 120L, 60L, 40L)).convertToInputWith(Boxes.getRandomHexString(), 0)
        val permitBox = Boxes.createPermitBox(ctx, 60L, userWID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(userWID, 2L))
        val repoOut = Boxes.createRepoWithR6(ctx, 100020, 301L, 100L, WIDs, Seq(100L, 120L, 40L, 40L), 3)
        val permitOut = Boxes.createPermitBox(ctx, 40L, userWID)
        val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(userWID, 2L), new ErgoToken(networkConfig._3.RSN, 20))
        val tx = ctx.newTxBuilder().addInputs(repoBox, permitBox, WIDBox)
          .fee(Configs.fee)
          .addOutputs(repoOut, permitOut, userOut)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          fail("transaction not signed")
      }
    })
  }

  property("test partially return permits using one wid token") {
    networkConfig._1.ergoClient.execute(ctx => {
      val prover = getProver()
      val userWID = Base16.decode(Boxes.getRandomHexString()).get
      val WIDs = Seq(
        Base16.decode(Boxes.getRandomHexString()).get,
        Base16.decode(Boxes.getRandomHexString()).get,
        userWID,
        Base16.decode(Boxes.getRandomHexString()).get
      )
      val repoBox = Boxes.createRepo(ctx, 100000, 321L, 100L, WIDs, Seq(100L, 120L, 60L, 40L)).convertToInputWith(Boxes.getRandomHexString(), 0)
      val permitBox = Boxes.createPermitBox(ctx, 60L, userWID).convertToInputWith(Boxes.getRandomHexString(), 0)
      val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(userWID, 1L))
      val repoOut = Boxes.createRepoWithR6(ctx, 100020, 301L, 100L, WIDs, Seq(100L, 120L, 40L, 40L), 3)
      val permitOut = Boxes.createPermitBox(ctx, 40L, userWID)
      val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(userWID, 1L), new ErgoToken(networkConfig._3.RSN, 20))
      val tx = ctx.newTxBuilder().addInputs(repoBox, permitBox, WIDBox)
        .fee(Configs.fee)
        .addOutputs(repoOut, permitOut, userOut)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  property("test partially return permits when output permit WID has changed") {
    networkConfig._1.ergoClient.execute(ctx => {
      val prover = getProver()
      val userWID = Base16.decode(Boxes.getRandomHexString()).get
      val otherWID = Base16.decode(Boxes.getRandomHexString()).get
      val WIDs = Seq(
        Base16.decode(Boxes.getRandomHexString()).get,
        otherWID,
        userWID,
        Base16.decode(Boxes.getRandomHexString()).get
      )
      val repoBox = Boxes.createRepo(ctx, 100000, 321L, 100L, WIDs, Seq(100L, 120L, 60L, 40L)).convertToInputWith(Boxes.getRandomHexString(), 0)
      val permitBox = Boxes.createPermitBox(ctx, 60L, userWID).convertToInputWith(Boxes.getRandomHexString(), 0)
      val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(userWID, 2L))
      val repoOut = Boxes.createRepoWithR6(ctx, 100020, 301L, 100L, WIDs, Seq(100L, 120L, 40L, 40L), 3)
      val permitOut = Boxes.createPermitBox(ctx, 40L, otherWID)
      val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(userWID, 2L), new ErgoToken(networkConfig._3.RSN, 20))
      val tx = ctx.newTxBuilder().addInputs(repoBox, permitBox, WIDBox)
        .fee(Configs.fee)
        .addOutputs(repoOut, permitOut, userOut)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        val signedTx = prover.sign(tx)
        println(signedTx.toJson(false))
      }
    })
  }

  property("test partially return permits when changing other wid permits in repo") {
    networkConfig._1.ergoClient.execute(ctx => {
      val prover = getProver()
      val userWID = Base16.decode(Boxes.getRandomHexString()).get
      val otherWID = Base16.decode(Boxes.getRandomHexString()).get
      val WIDs = Seq(
        Base16.decode(Boxes.getRandomHexString()).get,
        otherWID,
        userWID,
        Base16.decode(Boxes.getRandomHexString()).get
      )
      val repoBox = Boxes.createRepo(ctx, 100000, 321L, 100L, WIDs, Seq(100L, 120L, 60L, 40L)).convertToInputWith(Boxes.getRandomHexString(), 0)
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, otherWID)
      val permitBox = Boxes.createPermitBox(ctx, 60L, userWID).convertToInputWith(Boxes.getRandomHexString(), 0)
      val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(userWID, 2L))
      val repoOut = Boxes.createRepoWithR6(ctx, 100020, 301L, 100L, WIDs, Seq(100L, 100L, 60L, 40L), 2)
      val permitOut = Boxes.createPermitBox(ctx, 40L, userWID)
      val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(userWID, 2L), new ErgoToken(networkConfig._3.RSN, 20))
      val tx = ctx.newTxBuilder().addInputs(repoBox, userBox, WIDBox, permitBox)
        .fee(Configs.fee)
        .addOutputs(repoOut, permitOut, userOut)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        val signedTx = prover.sign(tx)
        println(signedTx.toJson(false))
      }
    })
  }

  property("test complete return permits") {
    networkConfig._1.ergoClient.execute(ctx => {
      var userIndex = 0
      try {
        val prover = getProver()
        val WIDs = generateRandomWIDList(6)
        val amounts = Seq(100L, 120L, 140L, 20L, 40L, 250L)
        for (userIndex <- 0 to 5) {
          val userWID = WIDs(userIndex)
          val totalPermitOut = amounts.sum
          val repoBox = Boxes.createRepo(ctx, 100000L, totalPermitOut + 1L, 99L, WIDs, amounts).convertToInputWith(Boxes.getRandomHexString(), 0)
          val permitBox = Boxes.createPermitBox(ctx, amounts(userIndex), userWID).convertToInputWith(Boxes.getRandomHexString(), 0)
          val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(userWID, 2L))
          val outputWIDs = WIDs.take(userIndex) ++ WIDs.drop(userIndex + 1)
          val outAmounts = amounts.take(userIndex) ++ amounts.drop(userIndex + 1)
          val repoOut = Boxes.createRepoWithR6(ctx, 100000L + amounts(userIndex), (totalPermitOut - amounts(userIndex)) + 1, 100L, outputWIDs, outAmounts, userIndex + 1) // 4 + first element in WID list is chain name
          val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(userWID, 2L), new ErgoToken(networkConfig._3.RSN, amounts(userIndex)))
          val watcherCollateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, userWID)
          val tx = ctx.newTxBuilder().addInputs(repoBox, permitBox, WIDBox, watcherCollateral)
            .fee(Configs.fee)
            .addOutputs(repoOut, userOut)
            .sendChangeTo(prover.getAddress)
            .build()
          prover.sign(tx)
        }
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail(s"transaction not signed for user index ${userIndex}")
      }
    })
  }

  property("test complete return permits using one wid token") {
    networkConfig._1.ergoClient.execute(ctx => {
      val prover = getProver()
      val WIDs = generateRandomWIDList(6)
      val amounts = Seq(100L, 120L, 140L, 20L, 40L, 250L)
      for (userIndex <- 0 to 5) {
        val userWID = WIDs(userIndex)
        val totalPermitOut = amounts.sum
        val repoBox = Boxes.createRepo(ctx, 100000L, totalPermitOut + 1L, 99L, WIDs, amounts).convertToInputWith(Boxes.getRandomHexString(), 0)
        val permitBox = Boxes.createPermitBox(ctx, amounts(userIndex), userWID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(userWID, 1L))
        val outputWIDs = WIDs.take(userIndex) ++ WIDs.drop(userIndex + 1)
        val outAmounts = amounts.take(userIndex) ++ amounts.drop(userIndex + 1)
        val repoOut = Boxes.createRepoWithR6(ctx, 100000L + amounts(userIndex), (totalPermitOut - amounts(userIndex)) + 1, 100L, outputWIDs, outAmounts, userIndex + 1) // 4 + first element in WID list is chain name
        val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(userWID, 1L), new ErgoToken(networkConfig._3.RSN, amounts(userIndex)))
        val watcherCollateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, userWID)
        val tx = ctx.newTxBuilder().addInputs(repoBox, permitBox, WIDBox, watcherCollateral)
          .fee(Configs.fee)
          .addOutputs(repoOut, userOut)
          .sendChangeTo(prover.getAddress)
          .build()
        assertThrows[AnyRef] {
          prover.sign(tx)
        }
      }
    })
  }

  property("test complete return permit while extending the wid list in repo") {
    networkConfig._1.ergoClient.execute(ctx => {
      val prover = getProver()
      val WIDs = generateRandomWIDList(6)
      val amounts = Seq(100L, 120L, 140L, 20L, 40L, 250L)
      val userIndex = 0
      val userWID = WIDs(userIndex)
      val totalPermitOut = amounts.sum
      val repoBox = Boxes.createRepo(ctx, 100000L, totalPermitOut + 1L, 99L, WIDs, amounts).convertToInputWith(Boxes.getRandomHexString(), 0)
      val permitBox = Boxes.createPermitBox(ctx, amounts(userIndex), userWID).convertToInputWith(Boxes.getRandomHexString(), 0)
      val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(userWID, 2L))
      val outputWIDs = WIDs.take(userIndex) ++ WIDs.drop(userIndex + 1) ++ Seq(WIDs(userIndex))
      val outAmounts = amounts.take(userIndex) ++ amounts.drop(userIndex + 1)
      val repoOut = Boxes.createRepoWithR6(ctx, 100000L + amounts(userIndex), (totalPermitOut - amounts(userIndex)) + 1, 100L, outputWIDs, outAmounts, userIndex + 1) // 4 + first element in WID list is chain name
      val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(userWID, 2), new ErgoToken(networkConfig._3.RSN, amounts(userIndex)))
      val watcherCollateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, userWID)
      val tx = ctx.newTxBuilder().addInputs(repoBox, permitBox, WIDBox, watcherCollateral)
        .fee(Configs.fee)
        .addOutputs(repoOut, userOut)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  property("test complete return permit while extending the rwt count list in repo") {
    networkConfig._1.ergoClient.execute(ctx => {
      val prover = getProver()
      val WIDs = generateRandomWIDList(6)
      val amounts = Seq(100L, 120L, 140L, 20L, 40L, 250L)
      val userIndex = 0
      val userWID = WIDs(userIndex)
      val totalPermitOut = amounts.sum
      val repoBox = Boxes.createRepo(ctx, 100000L, totalPermitOut + 1L, 99L, WIDs, amounts).convertToInputWith(Boxes.getRandomHexString(), 0)
      val permitBox = Boxes.createPermitBox(ctx, amounts(userIndex), userWID).convertToInputWith(Boxes.getRandomHexString(), 0)
      val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(userWID, 2L))
      val outputWIDs = WIDs.take(userIndex) ++ WIDs.drop(userIndex + 1)
      val outAmounts = amounts.take(userIndex) ++ amounts.drop(userIndex + 1) ++ Seq(100L)
      val repoOut = Boxes.createRepoWithR6(ctx, 100000L + amounts(userIndex), (totalPermitOut - amounts(userIndex)) + 1, 100L, outputWIDs, outAmounts, userIndex + 1) // 4 + first element in WID list is chain name
      val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(userWID, 2), new ErgoToken(networkConfig._3.RSN, amounts(userIndex)))
      val watcherCollateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, userWID)
      val tx = ctx.newTxBuilder().addInputs(repoBox, permitBox, WIDBox, watcherCollateral)
        .fee(Configs.fee)
        .addOutputs(repoOut, userOut)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  property("test complete return permit while extending both rwt count and wid list in repo") {
    networkConfig._1.ergoClient.execute(ctx => {
      val prover = getProver()
      val WIDs = generateRandomWIDList(6)
      val amounts = Seq(100L, 120L, 140L, 20L, 40L, 250L)
      val userIndex = 0
      val userWID = WIDs(userIndex)
      val totalPermitOut = amounts.sum
      val repoBox = Boxes.createRepo(ctx, 100000L, totalPermitOut + 1L, 99L, WIDs, amounts).convertToInputWith(Boxes.getRandomHexString(), 0)
      val permitBox = Boxes.createPermitBox(ctx, amounts(userIndex), userWID).convertToInputWith(Boxes.getRandomHexString(), 0)
      val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(userWID, 2L))
      val outputWIDs = WIDs.take(userIndex) ++ WIDs.drop(userIndex + 1) ++ Seq(WIDs(userIndex))
      val outAmounts = amounts.take(userIndex) ++ amounts.drop(userIndex + 1) ++ Seq(100L)
      val repoOut = Boxes.createRepoWithR6(ctx, 100000L + amounts(userIndex), (totalPermitOut - amounts(userIndex)) + 1, 100L, outputWIDs, outAmounts, userIndex + 1) // 4 + first element in WID list is chain name
      val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(userWID, 2), new ErgoToken(networkConfig._3.RSN, amounts(userIndex)))
      val watcherCollateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, userWID)
      val tx = ctx.newTxBuilder().addInputs(repoBox, permitBox, WIDBox, watcherCollateral)
        .fee(Configs.fee)
        .addOutputs(repoOut, userOut)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  /**
   * @target return permit transaction signing should throw error by returning a fake collateral not containing AWC
   * @dependencies
   * @scenario
   * - mock valid user WID box and permit box
   * - mock fake collateral without AWC
   * - mock input repo box with 99 AWC
   * - mock output repo box with 99 AWC
   * - mock valid user output with collateral tokens
   * - build and sign the complete return permit transaction
   * @expected
   * - sign error for returning a fake collateral not containing AWC
   */
  property("return permit transaction signing should throw error by returning a fake collateral not containing AWC") {
    networkConfig._1.ergoClient.execute(ctx => {
      val prover = getProver()
      val WIDs = generateRandomWIDList(6)
      val amounts = Seq(100L, 120L, 140L, 20L, 40L, 250L)
      val userIndex = 0
      val userWID = WIDs(userIndex)
      val totalPermitOut = amounts.sum
      val repoBox = Boxes.createRepo(ctx, 100000L, totalPermitOut + 1L, 99L, WIDs, amounts).convertToInputWith(Boxes.getRandomHexString(), 0)
      val permitBox = Boxes.createPermitBox(ctx, amounts(userIndex), userWID).convertToInputWith(Boxes.getRandomHexString(), 0)
      val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(userWID, 2L))
      val outputWIDs = WIDs.take(userIndex) ++ WIDs.drop(userIndex + 1)
      val outAmounts = amounts.take(userIndex) ++ amounts.drop(userIndex + 1)
      val repoOut = Boxes.createRepoWithR6(ctx, 100000L + amounts(userIndex), (totalPermitOut - amounts(userIndex)) + 1, 99L, outputWIDs, outAmounts, userIndex + 1) // 4 + first element in WID list is chain name
      val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(userWID, 2), new ErgoToken(networkConfig._3.RSN, amounts(userIndex)))
      val watcherCollateral = Boxes.createFakeWatcherCollateralBox(ctx, 1e9.toLong, 100, userWID).convertToInputWith(Boxes.getRandomHexString(), 3)
      val tx = ctx.newTxBuilder().addInputs(repoBox, permitBox, WIDBox, watcherCollateral)
        .fee(Configs.fee)
        .addOutputs(repoOut, userOut)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  property("test complete return of last permit") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val userWID = Base16.decode(Boxes.getRandomHexString()).get
        val repoBox = Boxes.createRepo(ctx, 100000, 41L, 99L, Seq(userWID), Seq(40L)).convertToInputWith(Boxes.getRandomHexString(), 0)
        val permitBox = Boxes.createPermitBox(ctx, 40L, userWID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(userWID, 2L))
        val repoOut = Boxes.createRepoWithR6(ctx, 100040, 1L, 100L, Seq(), Seq(), 1)
        val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(userWID, 2L), new ErgoToken(networkConfig._3.RSN, 40))
        val watcherCollateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, userWID)
        val tx = ctx.newTxBuilder().addInputs(repoBox, permitBox, WIDBox, watcherCollateral)
          .fee(Configs.fee)
          .addOutputs(repoOut, userOut)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          fail("transaction not signed")
      }
    })
  }

  property("test redeem repo") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val WIDs = generateRandomWIDList(7)
        val repoBox = Boxes.createRepo(ctx, 100000, 32001L, 100L, WIDs, Seq(100L, 120L, 140L, 20L, 40L, 250L, 123L)).convertToInputWith(Boxes.getRandomHexString(), 0)
        val guardBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(networkConfig._3.GuardNFT, 1L))
        val inputs = Seq(repoBox, guardBox)
        val boxBuilder = ctx.newTxBuilder().outBoxBuilder()
          .contract(ctx.newContract(prover.getAddress.asP2PK().script))
          .registers(
            repoBox.getRegisters.get(0),
            repoBox.getRegisters.get(1),
          )
          .tokens(new ErgoToken(networkConfig._3.GuardNFT, 1L))
        boxBuilder.value(inputs.map(item => item.getValue).sum - Configs.fee)
        repoBox.getTokens.forEach(token => boxBuilder.tokens(token))
        val tx = ctx.newTxBuilder().addInputs(repoBox, guardBox)
          .fee(Configs.fee)
          .addOutputs(boxBuilder.build())
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("transaction not signed")
      }
    })
  }

  /**
   * @target redeem repoConfig transaction should sign successfully
   * @dependencies
   * @scenario
   * - mock repo config
   * - mock commitments for an event for each wid
   * - mock guards secrets, public keys and box with the guard NFT
   * - build and sign the create trigger transaction
   * @expected
   * - successful sign for repo config redeem transaction
   */
  property("redeem repoConfig transaction should sign successfully") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId, 2).convertToInputWith(Boxes.getRandomHexString(), 1)
        val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
        val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
        val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
        val guardBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 0)
        val lockBox = Boxes.createLockBox(ctx, Configs.minBoxValue*10).convertToInputWith(Boxes.getRandomHexString(), 0)
        val inputs = Seq(repoConfig, guardBox)
        val boxBuilder = ctx.newTxBuilder().outBoxBuilder()
          .contract(ctx.newContract(prover.getAddress.asP2PK().script))
          .registers(
            repoConfig.getRegisters.get(0),
          )
          .tokens(new ErgoToken(networkConfig._3.GuardNFT, 1L))
        boxBuilder.value(inputs.map(item => item.getValue).sum - Configs.fee)
        val tx = ctx.newTxBuilder().addInputs(guardBox, repoConfig, lockBox)
          .fee(Configs.fee)
          .addDataInputs(guardBox)
          .addOutputs(boxBuilder.build())
          .sendChangeTo(prover.getAddress)
          .build()
        val multiSigProverBuilder = ctx.newProverBuilder()
        secrets.map(item => multiSigProverBuilder.withDLogSecret(item))
        val multiSigProver = multiSigProverBuilder.build()
          multiSigProver.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("transaction not signed")
      }
    })
  }

  property("test create new commitment") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val box1 = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 1))
        val permit = Boxes.createPermitBox(ctx, 10L, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val permitOut = Boxes.createPermitBox(ctx, 9L, WID)
        val commitmentBox = Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 1l)
        val WIDOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(WID, 1))
        val tx = ctx.newTxBuilder().addInputs(permit, box1)
          .fee(Configs.fee)
          .sendChangeTo(prover.getAddress)
          .addOutputs(permitOut, commitmentBox, WIDOut)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("transaction not signed")
      }
    })
  }

  property("test create new commitment with extra fee box") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val box1 = Boxes.createBoxForUser(ctx, prover.getAddress, 1e5.toLong, new ErgoToken(WID, 1))
        val box2 = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
        val permit = Boxes.createPermitBox(ctx, 10L, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val permitOut = Boxes.createPermitBox(ctx, 9L, WID)
        val commitmentBox = Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 1l)
        val WIDOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(WID, 1))
        val tx = ctx.newTxBuilder().addInputs(permit, box1, box2)
          .fee(Configs.fee)
          .sendChangeTo(prover.getAddress)
          .addOutputs(permitOut, commitmentBox, WIDOut)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("transaction not signed")
      }
    })
  }

  property("test create new commitment with RWT second place of permit tokens") {
    networkConfig._1.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WID = Base16.decode(Boxes.getRandomHexString()).get
      val box1 = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 1), new ErgoToken(networkConfig._3.RSN, 9))
      val permit = Boxes.createPermitBox(ctx, 10L, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
      val permitOut = Boxes.createInvalidMixedPermitBox(ctx, 9L, WID)
      val commitmentBox = Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 1l)
      val WIDOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(WID, 1))
      val tx = ctx.newTxBuilder().addInputs(permit, box1)
        .fee(Configs.fee)
        .sendChangeTo(prover.getAddress)
        .addOutputs(permitOut, commitmentBox, WIDOut)
        .build()
      assertThrows[AnyRef] {
        val signedTx = prover.sign(tx)
        println(signedTx.toJson(false))
      }
    })
  }

  property("test create new commitment with more than one RWT") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val box1 = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 1))
        val permit = Boxes.createPermitBox(ctx, 10L, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val permitOut = Boxes.createPermitBox(ctx, 8L, WID)
        val commitmentBox = Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 2l)
        val WIDOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(WID, 1))
        val tx = ctx.newTxBuilder().addInputs(permit, box1)
          .fee(Configs.fee)
          .sendChangeTo(prover.getAddress)
          .addOutputs(permitOut, commitmentBox, WIDOut)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("transaction not signed")
      }
    })
  }

  property("test create new commitment with more than one permit") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val box1 = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 1))
        val permit = Boxes.createPermitBox(ctx, 10L, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val permit2 = Boxes.createPermitBox(ctx, 20L, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val permitOut = Boxes.createPermitBox(ctx, 25L, WID)
        val commitmentBox = Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 5l)
        val WIDOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(WID, 1))
        val tx = ctx.newTxBuilder().addInputs(permit, permit2, box1)
          .fee(Configs.fee)
          .sendChangeTo(prover.getAddress)
          .addOutputs(permitOut, commitmentBox, WIDOut)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("transaction not signed")
      }
    })
  }

  property("test redeem commitment") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val commitmentBox = Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 1l).convertToInputWith(Boxes.getRandomHexString(), 1)
        val box = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 1L))
        val newPermit = Boxes.createPermitBox(ctx, 1, WID)
        val inputs = Seq(commitmentBox, box)
        val redeemUnsigned = ctx.newTxBuilder().addInputs(inputs: _*)
          .fee(Configs.fee)
          .addOutputs(newPermit)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(redeemUnsigned)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("transaction not signed")
      }
    })
  }

  property("test create event trigger for all watcher") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WIDs = generateRandomWIDList(5)
        val repo = Boxes.createRepo(ctx, 1000L, 10001L, 100L, WIDs, Seq(10L, 30L, 20L, 35L, 5L)).convertToInputWith(Boxes.getRandomHexString(), 1)
        val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId).convertToInputWith(Boxes.getRandomHexString(), 1)
        val commitments = WIDs.map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 10L).convertToInputWith(Boxes.getRandomHexString(), 1))
        val trigger = Boxes.createTriggerEventBox(ctx, WIDs, commitment, 50L)
        val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
        val tx = ctx.newTxBuilder().addInputs(commitments ++ Seq(feeBox): _*)
          .fee(Configs.fee)
          .addOutputs(trigger)
          .addDataInputs(repoConfig)
          .addDataInputs(repo)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.printStackTrace())
          fail("transaction not signed")
      }
    })
  }

  property("test create event trigger for minimum required watcher") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WIDs = generateRandomWIDList(7)
        val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId).convertToInputWith(Boxes.getRandomHexString(), 1)
        val repo = Boxes.createRepo(ctx, 1000L, 10001L, 100L, WIDs, Seq(10L, 30L, 20L, 25L, 5L, 4L, 6L)).convertToInputWith(Boxes.getRandomHexString(), 1)
        val commitments = WIDs.slice(0, 4).map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 10L).convertToInputWith(Boxes.getRandomHexString(), 1))
        val trigger = Boxes.createTriggerEventBox(ctx, WIDs.slice(0, 4), commitment, 40L)
        val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
        val tx = ctx.newTxBuilder().addInputs(commitments ++ Seq(feeBox): _*)
          .fee(Configs.fee)
          .addOutputs(trigger)
          .addDataInputs(repoConfig)
          .addDataInputs(repo)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("transaction not signed")
      }
    })
  }

  /**
   * @target create trigger transaction should sign successfully using multiple repo boxes, contributed by all watchers
   * @dependencies
   * @scenario
   * - mock two commitment wid list
   * - mock repo config and two repo boxes with the wid list as data input
   * - mock commitments for an event for each wid
   * - mock valid trigger
   * - build and sign the create trigger transaction
   * @expected
   * - successful sign for valid trigger transaction contributed by all watchers
   */
  property("create trigger transaction should sign successfully using multiple repo boxes, contributed by all watchers") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WIDs = generateRandomWIDList(5)
        val WIDs2 = generateRandomWIDList(5)
        val allWIDs = WIDs ++ WIDs2
        val repo = Boxes.createRepo(ctx, 1000L, 10001L, 100L, WIDs, Seq(10L, 30L, 20L, 35L, 5L)).convertToInputWith(Boxes.getRandomHexString(), 1)
        val repo2 = Boxes.createRepo(ctx, 1000L, 10001L, 100L, WIDs2, Seq(10L, 30L, 20L, 35L, 5L), 1).convertToInputWith(Boxes.getRandomHexString(), 1)
        val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId, 2).convertToInputWith(Boxes.getRandomHexString(), 1)
        val commitments = allWIDs.map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 10L).convertToInputWith(Boxes.getRandomHexString(), 1))
        val trigger = Boxes.createTriggerEventBox(ctx, allWIDs, commitment, 100L)
        val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
        val tx = ctx.newTxBuilder().addInputs(commitments ++ Seq(feeBox): _*)
          .fee(Configs.fee)
          .addOutputs(trigger)
          .addDataInputs(repoConfig)
          .addDataInputs(repo)
          .addDataInputs(repo2)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.printStackTrace())
          fail("transaction not signed")
      }
    })
  }

  /**
   * @target create trigger transaction should sign successfully using multiple repo boxes, contributed by minimum watchers
   * @dependencies
   * @scenario
   * - mock two commitment wid list
   * - mock repo config and two repo boxes with the wid list as data input
   * - mock commitments for an event for minimum number of watchers
   * - mock valid trigger
   * - build and sign the create trigger transaction
   * @expected
   * - successful sign for valid trigger transaction contributed by minimum watchers
   */
  property("create trigger transaction should sign successfully using multiple repo boxes, contributed by minimum watchers") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WIDs = generateRandomWIDList(5)
        val WIDs2 = generateRandomWIDList(5)
        val allWIDs = (WIDs ++ WIDs2).slice(0, 6)
        val repo = Boxes.createRepo(ctx, 1000L, 10001L, 100L, WIDs, Seq(10L, 30L, 20L, 35L, 5L)).convertToInputWith(Boxes.getRandomHexString(), 1)
        val repo2 = Boxes.createRepo(ctx, 1000L, 10001L, 100L, WIDs2, Seq(10L, 30L, 20L, 35L, 5L), 1).convertToInputWith(Boxes.getRandomHexString(), 1)
        val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId, 2).convertToInputWith(Boxes.getRandomHexString(), 1)
        val commitments = allWIDs.map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 10L).convertToInputWith(Boxes.getRandomHexString(), 1))
        val trigger = Boxes.createTriggerEventBox(ctx, allWIDs, commitment, 60L)
        val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
        val tx = ctx.newTxBuilder().addInputs(commitments ++ Seq(feeBox): _*)
          .fee(Configs.fee)
          .addOutputs(trigger)
          .addDataInputs(repoConfig)
          .addDataInputs(repo)
          .addDataInputs(repo2)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.printStackTrace())
          fail("transaction not signed")
      }
    })
  }

  property("test cant create event trigger for lower than minimum required watcher") {
    networkConfig._1.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(7)
      val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId).convertToInputWith(Boxes.getRandomHexString(), 1)
      val repo = Boxes.createRepo(ctx, 1000L, 10001L, 100L, WIDs, Seq(10L, 30L, 20L, 25L, 5L, 4L, 6L)).convertToInputWith(Boxes.getRandomHexString(), 1)
      val commitments = WIDs.slice(0, 3).map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 10L).convertToInputWith(Boxes.getRandomHexString(), 1))
      val trigger = Boxes.createTriggerEventBox(ctx, WIDs.slice(0, 3), commitment, 30L)
      val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
      val tx = ctx.newTxBuilder().addInputs(commitments ++ Seq(feeBox): _*)
        .fee(Configs.fee)
        .addOutputs(trigger)
        .addDataInputs(repoConfig)
        .addDataInputs(repo)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  /**
   * @target create trigger transaction signing should throw error when contributed by less than minimum watchers using multiple repo boxes
   * @dependencies
   * @scenario
   * - mock two commitment wid list
   * - mock repo config and two repo boxes with the wid list as data input
   * - mock commitments for an event for less than minimum number of watchers
   * - mock valid trigger
   * - build and sign the create trigger transaction
   * @expected
   * - sign error for trigger creation transaction contributed by less than minimum watchers
   */
  property("create trigger transaction signing should throw error when contributed by less than minimum watchers using multiple repo boxes") {
    networkConfig._1.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(5)
      val WIDs2 = generateRandomWIDList(5)
      val repo = Boxes.createRepo(ctx, 1000L, 10001L, 100L, WIDs, Seq(10L, 30L, 20L, 35L, 5L)).convertToInputWith(Boxes.getRandomHexString(), 1)
      val repo2 = Boxes.createRepo(ctx, 1000L, 10001L, 100L, WIDs2, Seq(10L, 30L, 20L, 35L, 5L), 1).convertToInputWith(Boxes.getRandomHexString(), 1)
      val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId, 2).convertToInputWith(Boxes.getRandomHexString(), 1)
      val commitments = WIDs.map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 10L).convertToInputWith(Boxes.getRandomHexString(), 1))
      val trigger = Boxes.createTriggerEventBox(ctx, WIDs, commitment, 50L)
      val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
      val tx = ctx.newTxBuilder().addInputs(commitments ++ Seq(feeBox): _*)
        .fee(Configs.fee)
        .addOutputs(trigger)
        .addDataInputs(repoConfig)
        .addDataInputs(repo)
        .addDataInputs(repo2)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  /**
   * @target create trigger transaction signing should throw error when the commitment count is not valid in trigger box
   * @dependencies
   * @scenario
   * - mock commitment wid list
   * - mock repo box with the wid list as data input
   * - mock commitments for an event for each wid
   * - mock invalid trigger with wrong commitment count
   * - build and sign the create trigger transaction
   * @expected
   * - sign error for invalid commitment count in trigger box
   */
  property("create trigger transaction signing should throw error when the commitment count is not valid in trigger box") {
    networkConfig._1.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(5)
      val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId).convertToInputWith(Boxes.getRandomHexString(), 1)
      val repo = Boxes.createRepo(ctx, 1000L, 10001L, 100L, WIDs, Seq(10L, 30L, 20L, 35L, 5L)).convertToInputWith(Boxes.getRandomHexString(), 1)
      val commitments = WIDs.map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 10L).convertToInputWith(Boxes.getRandomHexString(), 1))
      val trigger = Boxes.createTriggerEventBox(ctx, WIDs, commitment, 50L, Some(4))
      val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
      val tx = ctx.newTxBuilder().addInputs(commitments ++ Seq(feeBox): _*)
        .fee(Configs.fee)
        .addOutputs(trigger)
        .addDataInputs(repoConfig)
        .addDataInputs(repo)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  /**
   * @target create trigger transaction signing should throw error when the wid list digest is invalid in trigger box
   * @dependencies
   * @scenario
   * - mock commitment wid list
   * - mock repo box with the wid list as data input
   * - mock commitments for an event for each wid
   * - mock invalid trigger with wrong wid list digest
   * - build and sign the create trigger transaction
   * @expected
   * - sign error for invalid wid list digest in trigger box
   */
  property("create trigger transaction signing should throw error when the wid list digest is invalid in trigger box") {
    networkConfig._1.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(5)
      val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId).convertToInputWith(Boxes.getRandomHexString(), 1)
      val repo = Boxes.createRepo(ctx, 1000L, 10001L, 100L, WIDs, Seq(10L, 30L, 20L, 35L, 5L)).convertToInputWith(Boxes.getRandomHexString(), 1)
      val commitments = WIDs.map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 10L).convertToInputWith(Boxes.getRandomHexString(), 1))
      val trigger = Boxes.createFakeTriggerEventBox(ctx, WIDs, commitment, 50L)
      val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
      val tx = ctx.newTxBuilder().addInputs(commitments ++ Seq(feeBox): _*)
        .fee(Configs.fee)
        .addOutputs(trigger)
        .addDataInputs(repoConfig)
        .addDataInputs(repo)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  /**
   * @target create trigger transaction signing should throw error when using multiple repo boxes with invalid repoConfig from another chain
   * @dependencies
   * @scenario
   * - mock two commitment wid list
   * - mock repo config and one valid repo box with the wid list as data input
   * - mock an invalid repo box from another chain as data input
   * - mock commitments for an event for less than minimum number of watchers
   * - mock valid trigger
   * - build and sign the create trigger transaction
   * @expected
   * - sign error for trigger creation transaction with invalid repoConfig as data input
   */
  property("create trigger transaction signing should throw error when using multiple repo boxes with invalid repoConfig from another chain") {
    networkConfig._1.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(5)
      val repo = Boxes.createRepo(ctx, 1000L, 10001L, 100L, WIDs, Seq(10L, 30L, 20L, 35L, 5L)).convertToInputWith(Boxes.getRandomHexString(), 1)
      val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, Boxes.getRandomHexString()).convertToInputWith(Boxes.getRandomHexString(), 1)
      val commitments = WIDs.map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 10L).convertToInputWith(Boxes.getRandomHexString(), 1))
      val trigger = Boxes.createTriggerEventBox(ctx, WIDs, commitment, 50L)
      val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
      val tx = ctx.newTxBuilder().addInputs(commitments ++ Seq(feeBox): _*)
        .fee(Configs.fee)
        .addOutputs(trigger)
        .addDataInputs(repoConfig)
        .addDataInputs(repo)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  property("test create event trigger with repo box of different chain") {
    networkConfig._1.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(7)
      val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId).convertToInputWith(Boxes.getRandomHexString(), 1)
      val repo = Boxes.createRepoWithTokens(
        ctx,
        1000L,
        10001L,
        100L,
        WIDs,
        Seq(10L, 30L, 20L, 25L, 5L, 4L, 6L),
        networkConfig._3.RepoNFT,
        Boxes.getRandomHexString(),
        networkConfig._2.tokens.AwcNFT,
        0
      ).convertToInputWith(Boxes.getRandomHexString(), 1)
      val commitments = WIDs.slice(0, 3).map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 10L).convertToInputWith(Boxes.getRandomHexString(), 1))
      val trigger = Boxes.createTriggerEventBox(ctx, WIDs.slice(0, 3), commitment, 30L)
      val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
      val tx = ctx.newTxBuilder().addInputs(commitments ++ Seq(feeBox): _*)
        .fee(Configs.fee)
        .addOutputs(trigger)
        .addDataInputs(repoConfig)
        .addDataInputs(repo)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  /**
   * @target create trigger transaction signing should throw error when using multiple repo boxes with one invalid repo from another chain
   * @dependencies
   * @scenario
   * - mock two commitment wid list
   * - mock repo config and one valid repo box with the wid list as data input
   * - mock an invalid repo box from another chain as data input
   * - mock commitments for an event for less than minimum number of watchers
   * - mock valid trigger
   * - build and sign the create trigger transaction
   * @expected
   * - sign error for trigger creation transaction with one invalid repo as data input
   */
  property("create trigger transaction signing should throw error when using multiple repo boxes with one invalid repo from another chain") {
    networkConfig._1.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(5)
      val WIDs2 = generateRandomWIDList(5)
      val allWIDs = WIDs ++ WIDs2
      val repo = Boxes.createRepo(ctx, 1000L, 10001L, 100L, WIDs, Seq(10L, 30L, 20L, 35L, 5L))
        .convertToInputWith(Boxes.getRandomHexString(), 1)
      val repo2 = Boxes.createRepoWithTokens(
        ctx,
        1000L,
        10001L,
        100L,
        WIDs2,
        Seq(10L, 30L, 20L, 25L, 5L, 4L, 6L),
        networkConfig._3.RepoNFT,
        Boxes.getRandomHexString(),
        networkConfig._2.tokens.AwcNFT,
        1
      ).convertToInputWith(Boxes.getRandomHexString(), 1)
      val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId, 2)
        .convertToInputWith(Boxes.getRandomHexString(), 1)
      val commitments = allWIDs.map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 10L)
        .convertToInputWith(Boxes.getRandomHexString(), 1))
      val trigger = Boxes.createTriggerEventBox(ctx, allWIDs, commitment, 100L)
      val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
      val tx = ctx.newTxBuilder().addInputs(commitments ++ Seq(feeBox): _*)
        .fee(Configs.fee)
        .addOutputs(trigger)
        .addDataInputs(repoConfig)
        .addDataInputs(repo)
        .addDataInputs(repo2)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  property("test create event trigger with invalid repo box (invalid nft and valid rwt)") {
    networkConfig._1.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(7)
      val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId).convertToInputWith(Boxes.getRandomHexString(), 1)
      val repo = Boxes.createRepoWithTokens(
        ctx,
        1000L,
        10001L,
        100L,
        WIDs,
        Seq(10L, 30L, 20L, 25L, 5L, 4L, 6L),
        Boxes.getRandomHexString(),
        networkConfig._2.tokens.RWTId,
        networkConfig._2.tokens.AwcNFT,
        0
      ).convertToInputWith(Boxes.getRandomHexString(), 1)
      val commitments = WIDs.slice(0, 3).map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 10L).convertToInputWith(Boxes.getRandomHexString(), 1))
      val trigger = Boxes.createTriggerEventBox(ctx, WIDs.slice(0, 3), commitment, 30L)
      val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
      val tx = ctx.newTxBuilder().addInputs(commitments ++ Seq(feeBox): _*)
        .fee(Configs.fee)
        .addOutputs(trigger)
        .addDataInputs(repoConfig)
        .addDataInputs(repo)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  property("test create event trigger for all watchers with wrong trigger RWT sum") {
    networkConfig._1.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(5)
      val repoConfig = Boxes.createRepoConfigs(ctx, networkConfig._3.RepoNFT, networkConfig._2.tokens.RWTId).convertToInputWith(Boxes.getRandomHexString(), 1)
      val repo = Boxes.createRepo(ctx, 1000L, 10001L, 100L, WIDs, Seq(10L, 30L, 20L, 35L, 5L)).convertToInputWith(Boxes.getRandomHexString(), 1)
      val commitments = WIDs.map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 10L).convertToInputWith(Boxes.getRandomHexString(), 1))
      val trigger = Boxes.createTriggerEventBox(ctx, WIDs, commitment, 48L)
      val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
      val tx = ctx.newTxBuilder().addInputs(commitments ++ Seq(feeBox): _*)
        .fee(Configs.fee)
        .addOutputs(trigger)
        .addDataInputs(repoConfig)
        .addDataInputs(repo)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  property("test guard payment without not merged commitment") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WIDs = generateRandomWIDList(7)
        val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
        val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
        val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
        val userFee: Long = math.floor((commitment.fee * 0.6) / WIDs.length).toLong
        val guardBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 0)
        val lockBox = Boxes.createLockBox(
          ctx,
          Configs.minBoxValue,
          new ErgoToken(commitment.targetChainTokenId, userFee * WIDs.length)
        ).convertToInputWith(Boxes.getRandomHexString(), 0)
        val eventTrigger = Boxes.createTriggerEventBox(ctx, WIDs, commitment, 70L).convertToInputWith(Boxes.getRandomHexString(), 1)
        val newPermits = WIDs.map(item => {
          Boxes.createPermitBox(ctx, 10, item, new ErgoToken(commitment.targetChainTokenId, userFee))
        })
        val inputs = Seq(eventTrigger, lockBox)
        val unsignedTx = ctx.newTxBuilder().addInputs(inputs: _*)
          .addDataInputs(guardBox)
          .fee(Configs.fee)
          .sendChangeTo(prover.getAddress)
          .addOutputs(newPermits: _*)
          .build()
        val multiSigProverBuilder = ctx.newProverBuilder()
        secrets.map(item => multiSigProverBuilder.withDLogSecret(item))
        val multiSigProver = multiSigProverBuilder.build()
        multiSigProver.sign(unsignedTx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("transaction not signed")
      }
    })
  }

  /**
   * @target reward transaction signing should throw error when number of watcher rewards are less than the trigger commitment count
   * @dependencies
   * @scenario
   * - mock commitment wid list
   * - mock guards secrets, public keys and box with the guard NFT
   * - mock lock box with required tokens
   * - mock trigger box with wid list
   * - mock output permits for all wids except one
   * - build and sign the reward transaction
   * @expected
   * - sign error for fewer reward boxes than expected
   */
  property("reward transaction signing should throw error when number of watcher rewards are less than the trigger commitment count") {
    networkConfig._1.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(7)
      val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
      val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
      val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
      val userFee: Long = math.floor((commitment.fee * 0.6) / WIDs.length).toLong
      val guardBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 0)
      val lockBox = Boxes.createLockBox(
        ctx,
        Configs.minBoxValue,
        new ErgoToken(commitment.targetChainTokenId, userFee * WIDs.length)
      ).convertToInputWith(Boxes.getRandomHexString(), 0)
      val eventTrigger = Boxes.createTriggerEventBox(ctx, WIDs, commitment, 70L).convertToInputWith(Boxes.getRandomHexString(), 1)
      val newPermits = WIDs.slice(0, WIDs.length - 1).map(item => {
        Boxes.createPermitBox(ctx, 10, item, new ErgoToken(commitment.targetChainTokenId, userFee))
      })
      val inputs = Seq(eventTrigger, lockBox)
      val unsignedTx = ctx.newTxBuilder().addInputs(inputs: _*)
        .addDataInputs(guardBox)
        .fee(Configs.fee)
        .sendChangeTo(prover.getAddress)
        .addOutputs(newPermits: _*)
        .build()
      val multiSigProverBuilder = ctx.newProverBuilder()
      secrets.map(item => multiSigProverBuilder.withDLogSecret(item))
      val multiSigProver = multiSigProverBuilder.build()
      assertThrows[AnyRef] {
        multiSigProver.sign(unsignedTx)
      }
    })
  }

  /**
   * @target reward transaction signing should throw error when all reporting watchers are not rewarded
   * @dependencies
   * @scenario
   * - mock commitment wid list
   * - mock guards secrets, public keys and box with the guard NFT
   * - mock lock box with required tokens
   * - mock trigger box with wid list
   * - mock output permits for all wids except one + an extra permit for a new wid
   * - build and sign the reward transaction
   * @expected
   * - sign error for rewarding a wrong wid
   */
  property("reward transaction signing should throw error when all reporting watchers are not rewarded") {
    networkConfig._1.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(7)
      val newWID = Base16.decode(Boxes.getRandomHexString()).get
      val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
      val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
      val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
      val userFee: Long = math.floor((commitment.fee * 0.6) / WIDs.length).toLong
      val guardBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 0)
      val lockBox = Boxes.createLockBox(
        ctx,
        Configs.minBoxValue,
        new ErgoToken(commitment.targetChainTokenId, userFee * WIDs.length)
      ).convertToInputWith(Boxes.getRandomHexString(), 0)
      val eventTrigger = Boxes.createTriggerEventBox(ctx, WIDs, commitment, 70L).convertToInputWith(Boxes.getRandomHexString(), 1)
      val rewardingWIDs = WIDs.slice(0, WIDs.length - 1) ++ Seq(newWID)
      val newPermits = rewardingWIDs.map(item => {
        Boxes.createPermitBox(ctx, 10, item, new ErgoToken(commitment.targetChainTokenId, userFee))
      })
      val inputs = Seq(eventTrigger, lockBox)
      val unsignedTx = ctx.newTxBuilder().addInputs(inputs: _*)
        .addDataInputs(guardBox)
        .fee(Configs.fee)
        .sendChangeTo(prover.getAddress)
        .addOutputs(newPermits: _*)
        .build()
      val multiSigProverBuilder = ctx.newProverBuilder()
      secrets.map(item => multiSigProverBuilder.withDLogSecret(item))
      val multiSigProver = multiSigProverBuilder.build()
      assertThrows[AnyRef] {
        multiSigProver.sign(unsignedTx)
      }
    })
  }

  property("test guard payment with wrong permit RWT count") {
    networkConfig._1.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(7)
      val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
      val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
      val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
      val userFee: Long = math.floor((commitment.fee * 0.6) / WIDs.length).toLong
      val guardBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 0)
      val lockBox = Boxes.createLockBox(
        ctx,
        1e9.toLong,
        new ErgoToken(commitment.targetChainTokenId, userFee * WIDs.length)
      ).convertToInputWith(Boxes.getRandomHexString(), 0)
      val eventTrigger = Boxes.createTriggerEventBox(ctx, WIDs, commitment, 70L).convertToInputWith(Boxes.getRandomHexString(), 1)
      val newPermits = WIDs.map(item => {
        Boxes.createPermitBox(ctx, 9, item, new ErgoToken(commitment.targetChainTokenId, userFee))
      })
      val inputs = Seq(eventTrigger, lockBox)
      val unsignedTx = ctx.newTxBuilder().addInputs(inputs: _*)
        .addDataInputs(guardBox)
        .fee(Configs.fee)
        .sendChangeTo(prover.getAddress)
        .addOutputs(newPermits: _*)
        .build()
      val multiSigProverBuilder = ctx.newProverBuilder()
      secrets.map(item => multiSigProverBuilder.withDLogSecret(item))
      val multiSigProver = multiSigProverBuilder.build()
      assertThrows[AnyRef] {
        multiSigProver.sign(unsignedTx)
      }
    })
  }

  property("test guard payment with not merged commitment") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WIDs = generateRandomWIDList(7)
        val notMergedWIDs = generateRandomWIDList(3)
        val allWIDs = WIDs ++ notMergedWIDs
        val notMergedCommitments = notMergedWIDs.map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 10L).convertToInputWith(Boxes.getRandomHexString(), 1))
        val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
        val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
        val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
        val eventTrigger = Boxes.createTriggerEventBox(ctx, WIDs, commitment, 70L).convertToInputWith(Boxes.getRandomHexString(), 1)
        val userFee: Long = math.floor((commitment.fee * 0.6) / allWIDs.length).toLong
        val guardBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 0)
        val lockBox = Boxes.createLockBox(
          ctx,
          Configs.minBoxValue,
          new ErgoToken(commitment.targetChainTokenId, userFee * (WIDs ++ notMergedWIDs).length)
        ).convertToInputWith(Boxes.getRandomHexString(), 0)
        val newPermits = (WIDs ++ notMergedWIDs).map(item => {
          Boxes.createPermitBox(ctx, 10, item, new ErgoToken(commitment.targetChainTokenId, userFee))
        })
        val inputs = Seq(eventTrigger) ++ notMergedCommitments ++ Seq(lockBox)
        val unsignedTx = ctx.newTxBuilder().addInputs(inputs: _*)
          .fee(Configs.fee)
          .addDataInputs(guardBox)
          .sendChangeTo(prover.getAddress)
          .addOutputs(newPermits: _*)
          .build()
        val multiSigProverBuilder = ctx.newProverBuilder()
        secrets.map(item => multiSigProverBuilder.withDLogSecret(item))
        val multiSigProver = multiSigProverBuilder.build()
        multiSigProver.sign(unsignedTx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("transaction not signed")
      }
    })
  }

  property("test guard payment with not merged wrong commitment") {
    networkConfig._1.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(7)
      val notMergedWIDs = generateRandomWIDList(1)
      val allWIDs = WIDs ++ notMergedWIDs
      val notMergedCommitments = notMergedWIDs.map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 11L).convertToInputWith(Boxes.getRandomHexString(), 1))
      val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
      val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
      val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
      val eventTrigger = Boxes.createTriggerEventBox(ctx, WIDs, commitment, 70L).convertToInputWith(Boxes.getRandomHexString(), 1)
      val userFee: Long = math.floor((commitment.fee * 0.6) / allWIDs.length).toLong
      val guardBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 0)
      val lockBox = Boxes.createLockBox(
        ctx,
        1e9.toLong,
        new ErgoToken(commitment.targetChainTokenId, userFee * (WIDs ++ notMergedWIDs).length)
      ).convertToInputWith(Boxes.getRandomHexString(), 0)
      val newPermits = (WIDs ++ notMergedWIDs).map(item => {
        Boxes.createPermitBox(ctx, 10, item, new ErgoToken(commitment.targetChainTokenId, userFee))
      })
      val inputs = Seq(eventTrigger) ++ notMergedCommitments ++ Seq(lockBox)
      val unsignedTx = ctx.newTxBuilder().addInputs(inputs: _*)
        .fee(Configs.fee)
        .addDataInputs(guardBox)
        .sendChangeTo(prover.getAddress)
        .addOutputs(newPermits: _*)
        .build()
      val multiSigProverBuilder = ctx.newProverBuilder()
      secrets.map(item => multiSigProverBuilder.withDLogSecret(item))
      val multiSigProver = multiSigProverBuilder.build()
      assertThrows[AnyRef] {
        multiSigProver.sign(unsignedTx)
      }
    })
  }

  property("test create fraud from event trigger") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val commitment = new Commitment()
        val WIDs = generateRandomWIDList(7)
        val triggerEvent = Boxes.createTriggerEventBox(ctx, WIDs, commitment, 70L).convertToInputWith(Boxes.getRandomHexString(), 0)
        val box1 = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(networkConfig._2.tokens.CleanupNFT, 1L))
        val newFraud = WIDs.indices.map(index => {
          Boxes.createFraudBox(ctx, WIDs(index), 10L)
        })
        val unsignedTx = ctx.newTxBuilder().addInputs(triggerEvent, box1)
          .fee(Configs.fee)
          .sendChangeTo(prover.getAddress)
          .addOutputs(newFraud: _*)
          .build()
        prover.sign(unsignedTx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("transaction not signed")
      }
    })
  }

  property("test redeem fraud to repo") {
    networkConfig._1.ergoClient.execute(ctx => {
      var userIndex = 0
      try {
        val prover = getProver()
        val globalWIDs = generateRandomWIDList(3)
        val globalAmounts = Seq(100L, 11L, 20L)
        val repo = Boxes.createRepo(ctx, 1000L, globalAmounts.sum + 1, 100L, globalWIDs, globalAmounts).convertToInputWith(Boxes.getRandomHexString(), 1)
        for (userIndex <- 0 until globalWIDs.length) {
          var amounts = globalAmounts.map(item => item).toArray
          var WIDs = globalWIDs.map(item => item).toArray
          val WID = WIDs(userIndex)
          val box2 = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(networkConfig._2.tokens.CleanupNFT, 1L))
          val fraud = Boxes.createFraudBox(ctx, WID, 10L).convertToInputWith(Boxes.getRandomHexString(), 1)
          val RWTCount = repo.getTokens.get(1).getValue.toLong + 10
          val RSNCount = repo.getTokens.get(2).getValue.toLong - 10
          amounts(userIndex) -= 10
          val repoCandidate = Boxes.createRepoWithR6(ctx, RWTCount, RSNCount, 100L, WIDs, amounts, userIndex + 1)
          val unsigned = ctx.newTxBuilder().addInputs(repo, fraud, box2)
            .fee(Configs.fee)
            .addOutputs(repoCandidate)
            .sendChangeTo(prover.getAddress)
            .build()
          val signed = prover.sign(unsigned)
        }
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail(s"transaction not signed on index ${userIndex}")
      }
    })
  }

  property("test spent lock script when guard token is in data input") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
        val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
        val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
        val prover = getProver()
        val box = Boxes.createCustomBox(ctx, contracts.Lock._1, 1e9.toLong)
        val boxNft = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 32)
        val outBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 5e8.toLong)
        val multiSigProverBuilder = ctx.newProverBuilder()
        secrets.slice(0, 5).foreach(item => multiSigProverBuilder.withDLogSecret(item))
        val multiSigProver = multiSigProverBuilder.build()
        val tx = ctx.newTxBuilder().addInputs(box)
          .fee(Configs.fee)
          .addOutputs(outBox)
          .addDataInputs(boxNft)
          .sendChangeTo(prover.getAddress)
          .build()
        multiSigProver.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail(s"transaction not signed")
      }
    })
  }

  property("test guard nft box spend with update with 6 sign.") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
        val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
        val prover = guards(0)
        val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
        val signBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 1)
        val box2 = Boxes.createBoxForUser(ctx, guards(0).getAddress, 1e9.toLong)
        val outSignBox = Boxes.createGuardNftBox(ctx, guardsPks, 4, 6)
        val outBox = Boxes.createBoxCandidateForUser(ctx, guards(1).getAddress, 1e8.toLong)
        val tx = ctx.newTxBuilder().addInputs(signBox, box2)
          .fee(Configs.fee)
          .addOutputs(outSignBox, outBox)
          .sendChangeTo(prover.getAddress)
          .build()
        val proverBuilder = ctx.newProverBuilder()
        secrets.slice(0, 6).map(item => proverBuilder.withDLogSecret(item))
        proverBuilder.build().sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail(s"transaction not signed")
      }
    })
  }

  property("test guard nft box cant spend with update with 5 sign") {
    networkConfig._1.ergoClient.execute(ctx => {
      val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
      val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
      val prover = guards(0)
      val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
      val signBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 1)
      val box2 = Boxes.createBoxForUser(ctx, guards(0).getAddress, 1e9.toLong)
      val outSignBox = Boxes.createGuardNftBox(ctx, guardsPks, 4, 6)
      val outBox = Boxes.createBoxCandidateForUser(ctx, guards(1).getAddress, 1e8.toLong)
      val tx = ctx.newTxBuilder().addInputs(signBox, box2)
        .fee(Configs.fee)
        .addOutputs(outSignBox, outBox)
        .sendChangeTo(prover.getAddress)
        .build()
      val proverBuilder = ctx.newProverBuilder()
      secrets.slice(0, 5).map(item => proverBuilder.withDLogSecret(item))
      assertThrows[AnyRef] {
        proverBuilder.build().sign(tx)
      }
    })
  }
}
