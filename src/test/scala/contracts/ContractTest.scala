package contracts

import helpers.{Configs, Network, NetworkGeneral, Utils}
import org.ergoplatform.appkit.{ErgoProver, ErgoToken}
import rosen.bridge.Contracts
import scorex.util.encode.Base16
import testUtils.{Boxes, Commitment, TestSuite}


class ContractTest extends TestSuite {
  val sk: BigInt = Utils.randBigInt

  val networkConfig: (NetworkGeneral, Network) = Utils.selectConfig("cardano", "mainnet")
  val contracts = new Contracts(networkConfig._1, networkConfig._2)

  def getProver(): ErgoProver = {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 2e9.toLong, new ErgoToken(networkConfig._1.mainTokens.RSN, 200L))
        val repoConfig = Boxes.createRepoConfigsInput(ctx)
        val repoBox = Boxes.createRepo(ctx, 100000, 1L, 100L, 0).convertToInputWith(Boxes.getRandomHexString(), 0)
        val repoOut = Boxes.createRepo(ctx, 99900, 101L, 99L, 1)
        val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
        val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 3L))
        val watcherCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, repoBox.getId.getBytes, 100)
        val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
          .fee(Configs.fee)
          .addOutputs(repoOut, watcherCollateral, permitBox, WID)
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 2e9.toLong, new ErgoToken(networkConfig._1.mainTokens.RSN, 200L))
        val repoConfig = Boxes.createRepoConfigsInput(ctx)
        val repoBox = Boxes.createRepoInput(ctx, 100000, 5801L, 100L, 1)
        val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 99L, 2)
        val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
        val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 3L))
        val watcherCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, repoBox.getId.getBytes, 100L)
        val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
          .fee(Configs.fee)
          .addDataInputs(repoConfig)
          .addOutputs(repoOut, watcherCollateral, permitBox, WID)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          fail("transaction not signed")
      }
    })
  }

  /**
   * @target get permit transaction should sign successfully with 0 rsn collateral
   * @dependencies
   * @scenario
   * - mock user input
   * - mock input repo box and repoConfig box as data input
   * - mock valid output repo box and collateral box without rsn
   * - mock valid output wid and permit box
   * - build and sign the get permit transaction
   * @expected
   * - successful sign for get permit transaction
   */
  property("get permit transaction should sign successfully with 0 rsn collateral") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 2e9.toLong, new ErgoToken(networkConfig._1.mainTokens.RSN, 200L))
        val repoConfig = Boxes.createRepoConfigsWithList(ctx, networkConfig._2.tokens.RepoConfigNFT, Array(10L, 51L, 0L, 9999L, 1e9.toLong, 0))
          .convertToInputWith(Boxes.getRandomHexString(), 1)
        val repoBox = Boxes.createRepoInput(ctx, 100000, 1L, 100L, 0)
        val repoOut = Boxes.createRepo(ctx, 99900, 101L, 99L, 1)
        val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
        val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 3L))
        val watcherCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 0, repoBox.getId.getBytes, 100)
        val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
          .fee(Configs.fee)
          .addOutputs(repoOut, watcherCollateral, permitBox, WID)
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

  property("test get permit while minting just one wid") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 2e9.toLong, new ErgoToken(networkConfig._1.mainTokens.RSN, 300L))
      val repoConfig = Boxes.createRepoConfigsInput(ctx)
      val repoBox = Boxes.createRepoInput(ctx, 100000, 5801L, 100L, 0)
      val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 99L, 2)
      val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
      val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 1L))
      val watcherCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, repoBox.getId.getBytes, 100L)
      val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
        .fee(Configs.fee)
        .addDataInputs(repoConfig)
        .addOutputs(repoOut, watcherCollateral, permitBox, WID)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        val signedTx = prover.sign(tx)
        println(signedTx.toJson(false))
      }
    })
  }

  property("test get permit while created permit first token is not RWT") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 2e9.toLong, new ErgoToken(networkConfig._1.mainTokens.RSN, 300L))
      val repoConfig = Boxes.createRepoConfigsInput(ctx)
      val repoBox = Boxes.createRepoInput(ctx, 100000, 5801L, 100L, 0)
      val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 99L, 2)
      val permitBox = Boxes.createInvalidPermitBox(ctx, 100L, repoBox.getId.getBytes)
      val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 3L))
      val watcherCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, repoBox.getId.getBytes, 100L)
      val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
        .fee(Configs.fee)
        .addDataInputs(repoConfig)
        .addOutputs(repoOut, watcherCollateral, permitBox, WID)
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
   * - mock invalid repoConfig box with invalid NFT as data input
   * - mock valid output repo box with 100 AWC and collateral box
   * - mock valid output wid and permit box
   * - build and sign the get permit transaction
   * @expected
   * - sign error for invalid repo config box
   */
  property("get permit transaction signing should throw error when repoConfig is invalid") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 2e9.toLong, new ErgoToken(networkConfig._1.mainTokens.RSN, 200L))
      val repoConfig = Boxes.createRepoConfigsWithList(ctx, Boxes.getRandomHexString(), Array(10L, 51L, 0L, 9999L, 1e9.toLong, 100))
        .convertToInputWith(Boxes.getRandomHexString(), 1)
      val repoBox = Boxes.createRepoInput(ctx, 100000, 5801L, 100L, 1)
      val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 99L, 2)
      val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
      val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 3L))
      val watcherCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, repoBox.getId.getBytes, 100)
      val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
        .fee(Configs.fee)
        .addDataInputs(repoConfig)
        .addOutputs(repoOut, watcherCollateral, permitBox, WID)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  /**
   * @target get permit transaction signing should throw error when repo value decreased in transaction
   * @dependencies
   * @scenario
   * - mock user input
   * - mock input repo box with 1e10 value
   * - mock valid repoConfig box as data input
   * - mock valid output repo box with 1e9 value
   * - mock valid output wid, collateral and permit box
   * - build and sign the get permit transaction
   * @expected
   * - sign error since 1e9 is less than 1e10
   */
  property("get permit transaction signing should throw error when repo value decreased in transaction") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 2e9.toLong, new ErgoToken(networkConfig._1.mainTokens.RSN, 200L))
      val repoConfig = Boxes.createRepoConfigsInput(ctx)
      val repoBox = Boxes.createRepoInput(ctx, 100000, 5801L, 100L, 1, 1e10.toLong)
      val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 99L, 2, 1e9.toLong)
      val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
      val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 3L))
      val watcherCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, repoBox.getId.getBytes, 100)
      val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
        .fee(Configs.fee)
        .addDataInputs(repoConfig)
        .addOutputs(repoOut, watcherCollateral, permitBox, WID)
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 2e9.toLong, new ErgoToken(networkConfig._1.mainTokens.RSN, 200L))
      val repoConfig = Boxes.createRepoConfigsInput(ctx)
      val repoBox = Boxes.createRepoInput(ctx, 100000, 5801L, 100L, 0)
      val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 100L, 2)
      val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
      val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 3L))
      val watcherCollateral = Boxes.createFakeWatcherCollateralBox(ctx, 1e9.toLong, 100, repoBox.getId.getBytes, 100L)
      val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
        .fee(Configs.fee)
        .addDataInputs(repoConfig)
        .addOutputs(repoOut, watcherCollateral, permitBox, WID)
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 2e9.toLong, new ErgoToken(networkConfig._1.mainTokens.RSN, 200L))
      val repoConfig = Boxes.createRepoConfigsInput(ctx)
      val repoBox = Boxes.createRepoInput(ctx, 100000, 5801L, 100L, 0)
      val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 99L, 2)
      val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
      val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 3L), new ErgoToken(networkConfig._2.tokens.AwcNFT, 1L))
      val watcherCollateral = Boxes.createFakeWatcherCollateralBox(ctx, 1e9.toLong, 100, repoBox.getId.getBytes, 100)
      val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
        .fee(Configs.fee)
        .addDataInputs(repoConfig)
        .addOutputs(repoOut, watcherCollateral, permitBox, WID)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        val signedTx = prover.sign(tx)
        println(signedTx.toJson(false))
      }
    })
  }

  /**
   * @target get permit transaction signing should throw error when rwt count is not set correctly in collateral
   * @dependencies
   * @scenario
   * - mock user input
   * - mock valid input repo box
   * - mock valid repoConfig box as data input
   * - mock valid output repo, permit and wid box
   * - mock invalid collateral with wrong rwt count
   * - build and sign the get permit transaction
   * @expected
   * - sign error for invalid rwt count in collateral (101 != 100)
   */
  property("get permit transaction signing should throw error when rwt count is not set correctly in collateral") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 2e9.toLong, new ErgoToken(networkConfig._1.mainTokens.RSN, 200L))
      val repoConfig = Boxes.createRepoConfigsInput(ctx)
      val repoBox = Boxes.createRepo(ctx, 100000, 1L, 100L, 0).convertToInputWith(Boxes.getRandomHexString(), 0)
      val repoOut = Boxes.createRepo(ctx, 99900, 101L, 99L, 1)
      val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
      val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 3L))
      val watcherCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, repoBox.getId.getBytes, 101)
      val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
        .fee(Configs.fee)
        .addOutputs(repoOut, watcherCollateral, permitBox, WID)
        .addDataInputs(repoConfig)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  property("test extend permit when there is just one permit on network") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._1.mainTokens.RSN, 100L))
        val repoBox = Boxes.createRepoInput(ctx, 100000, 5801L, 100L, 1)
        val collateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, WID, 100)
        val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 100L, 1)
        val outCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, WID, 200L)
        val permitBox = Boxes.createPermitBox(ctx, 100L, WID)
        val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 2L))
        val tx = ctx.newTxBuilder().addInputs(repoBox, collateral, userBox)
          .fee(Configs.fee)
          .addOutputs(repoOut, outCollateral, permitBox, WIDBox)
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
   * @target extend permit transaction should sign successfully with 0 rsn collateral
   * @dependencies
   * @scenario
   * - mock user input
   * - mock valid input repo and wid box
   * - mock valid collateral without rsn
   * - mock valid output repo, permit and wid box
   * - mock valid collateral without rsn
   * - build and sign the extend permit transaction
   * @expected
   * - successful sign for extend permit transaction
   */
  property("extend permit transaction should sign successfully with 0 rsn collateral") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._1.mainTokens.RSN, 100L))
        val repoBox = Boxes.createRepoInput(ctx, 100000, 5801L, 100L, 1)
        val collateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 0, WID, 100)
        val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 100L, 1)
        val outCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 0, WID, 200L)
        val permitBox = Boxes.createPermitBox(ctx, 100L, WID)
        val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 2L))
        val tx = ctx.newTxBuilder().addInputs(repoBox, collateral, userBox)
          .fee(Configs.fee)
          .addOutputs(repoOut, outCollateral, permitBox, WIDBox)
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val WID = Base16.decode(Boxes.getRandomHexString()).get
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 1L), new ErgoToken(networkConfig._1.mainTokens.RSN, 100L))
      val repoBox = Boxes.createRepoInput(ctx, 100000, 5801L, 100L, 0)
      val collateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, WID, 100)
      val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 100L, 1)
      val outCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, WID, 200L)
      val permitBox = Boxes.createPermitBox(ctx, 100L, WID)
      val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 1L))
      val tx = ctx.newTxBuilder().addInputs(repoBox, collateral, userBox)
        .fee(Configs.fee)
        .addOutputs(repoOut, outCollateral, permitBox, WIDBox)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  property("test extend first permit while extended permit wid has changed") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val WID = Base16.decode(Boxes.getRandomHexString()).get
      val otherWID = Base16.decode(Boxes.getRandomHexString()).get
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._1.mainTokens.RSN, 100L))
      val repoBox = Boxes.createRepoInput(ctx, 100000, 5801L, 100L, 0)
      val collateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, WID, 100)
      val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 100L, 1)
      val outCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, WID, 200L)
      val permitBox = Boxes.createPermitBox(ctx, 100L, otherWID)
      val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 2L))
      val tx = ctx.newTxBuilder().addInputs(repoBox, collateral, userBox)
        .fee(Configs.fee)
        .addOutputs(repoOut, outCollateral, permitBox, WIDBox)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        val signedTx = prover.sign(tx)
        println(signedTx.toJson(false))
      }
    })
  }

  property("test extend first permit while extended permit first token is not RWT") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val WID = Base16.decode(Boxes.getRandomHexString()).get
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._1.mainTokens.RSN, 200L))
      val repoBox = Boxes.createRepoInput(ctx, 100000, 5801L, 100L, 0)
      val collateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, WID, 100)
      val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 100L, 1)
      val outCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, WID, 200L)
      val permitBox = Boxes.createInvalidPermitBox(ctx, 100L, WID)
      val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 2L))
      val tx = ctx.newTxBuilder().addInputs(repoBox, collateral, userBox)
        .fee(Configs.fee)
        .addOutputs(repoOut, outCollateral, permitBox, WIDBox)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        val signedTx = prover.sign(tx)
        println(signedTx.toJson(false))
      }
    })
  }

  /**
   * @target extend permit transaction should throw error when using fake collateral in inputs altering rwt count
   * @dependencies
   * @scenario
   * - mock user input
   * - mock valid input repo and wid box
   * - mock fake collateral without AWC and different rwt count
   * - mock valid collateral
   * - mock valid output repo, permit and wid box
   * - mock invalid collateral with AWC and altered rwt count
   * - build and sign the extend permit transaction
   * @expected
   * - sign error for invalid rwt count in output collateral (110 != 200)
   */
  property("extend permit transaction should throw error when using fake collateral in inputs altering rwt count") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val WID = Base16.decode(Boxes.getRandomHexString()).get
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._1.mainTokens.RSN, 100L))
      val repoBox = Boxes.createRepoInput(ctx, 100000, 5801L, 100L, 1)
      val collateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 0, WID, 100)
      val fakeCollateral = Boxes.createFakeWatcherCollateralBox(ctx, 1e9.toLong, 0, WID, 10).convertToInputWith(Boxes.getRandomHexString(), 1)
      val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 100L, 1)
      val outCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 0, WID, 110L)
      val permitBox = Boxes.createPermitBox(ctx, 100L, WID)
      val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 2L))
      val tx = ctx.newTxBuilder().addInputs(repoBox, fakeCollateral, userBox, collateral)
        .fee(Configs.fee)
        .addOutputs(repoOut, outCollateral, permitBox, WIDBox)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  /**
   * @target extend permit transaction signing should throw error when rwt count is not updated correctly in collateral
   * @dependencies
   * @scenario
   * - mock user input
   * - mock valid input repo, collateral and wid box
   * - mock valid repoConfig box as data input
   * - mock valid output repo, permit and wid box
   * - mock invalid collateral with wrong rwt count
   * - build and sign the extend permit transaction
   * @expected
   * - sign error for invalid rwt count in output collateral (201 != 200)
   */
  property("extend permit transaction signing should throw error when rwt count is not updated correctly in collateral") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val WID = Base16.decode(Boxes.getRandomHexString()).get
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._1.mainTokens.RSN, 100L))
      val repoBox = Boxes.createRepoInput(ctx, 100000, 5801L, 100L, 0)
      val collateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, WID, 100)
      val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 100L, 1)
      val outCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, WID, 201L)
      val permitBox = Boxes.createPermitBox(ctx, 100L, WID)
      val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 2L))
      val tx = ctx.newTxBuilder().addInputs(repoBox, collateral, userBox)
        .fee(Configs.fee)
        .addOutputs(repoOut, outCollateral, permitBox, WIDBox)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  /**
   * @target extend permit transaction signing should throw error when collateral wid has changed in outputs
   * @dependencies
   * @scenario
   * - mock user input
   * - mock valid input repo, collateral and wid box
   * - mock valid repoConfig box as data input
   * - mock valid output repo, permit and wid box
   * - mock invalid collateral with wrong wid
   * - build and sign the extend permit transaction
   * @expected
   * - sign error for invalid wid in output collateral
   */
  property("extend permit transaction signing should throw error when collateral wid has changed in outputs") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val WID = Base16.decode(Boxes.getRandomHexString()).get
      val WID2 = Base16.decode(Boxes.getRandomHexString()).get
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._1.mainTokens.RSN, 100L))
      val repoBox = Boxes.createRepoInput(ctx, 100000, 5801L, 100L, 0)
      val collateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, WID, 100)
      val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 100L, 1)
      val outCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, WID2, 200L)
      val permitBox = Boxes.createPermitBox(ctx, 100L, WID)
      val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 2L))
      val tx = ctx.newTxBuilder().addInputs(repoBox, collateral, userBox)
        .fee(Configs.fee)
        .addOutputs(repoOut, outCollateral, permitBox, WIDBox)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  /**
   * @target extend permit transaction signing should throw error when stealing rsn collateral
   * @dependencies
   * @scenario
   * - mock user input
   * - mock valid input repo, collateral and wid box
   * - mock valid repoConfig box as data input
   * - mock valid output repo, permit and wid box
   * - mock invalid collateral with wrong rwt count
   * - build and sign the extend permit transaction
   * @expected
   * - sign error for stealing rsn collateral
   */
  property("extend permit transaction signing should throw error when stealing rsn collateral") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val WID = Base16.decode(Boxes.getRandomHexString()).get
      val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._1.mainTokens.RSN, 100L))
      val repoBox = Boxes.createRepoInput(ctx, 100000, 5801L, 100L, 1)
      val collateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, WID, 100)
      val repoOut = Boxes.createRepo(ctx, 99900, 5901L, 100L, 1)
      val outCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 0, WID, 200L)
      val permitBox = Boxes.createPermitBox(ctx, 100L, WID)
      val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 2L))
      val tx = ctx.newTxBuilder().addInputs(repoBox, collateral, userBox)
        .fee(Configs.fee)
        .addOutputs(repoOut, outCollateral, permitBox, WIDBox)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }


  property("test partially return permits") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val repoBox = Boxes.createRepo(ctx, 10000, 321, 100L, 1).convertToInputWith(Boxes.getRandomHexString(), 0)
        val collateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, WID, 100L)
        val repoOut = Boxes.createRepo(ctx, 10020, 301, 100L, 1)
        val outCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, WID, 80L)
        val permitBox = Boxes.createPermitBox(ctx, 60L, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L))
        val permitOut = Boxes.createPermitBox(ctx, 40L, WID)
        val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._1.mainTokens.RSN, 20))
        val tx = ctx.newTxBuilder().addInputs(repoBox, collateral, permitBox, WIDBox)
          .fee(Configs.fee)
          .addOutputs(repoOut, outCollateral, permitOut, userOut)
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
   * @target partial return permit transaction should sign successfully with 0 rsn collateral
   * @dependencies
   * @scenario
   * - mock user input
   * - mock valid input repo, permit and wid box
   * - mock valid collateral without rsn
   * - mock valid output repo, permit and wid box
   * - mock valid collateral without rsn
   * - build and sign the return permit transaction
   * @expected
   * - successful sign for return permit transaction
   */
  property("partial return permit transaction should sign successfully with 0 rsn collateral") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val repoBox = Boxes.createRepo(ctx, 10000, 321, 100L, 1).convertToInputWith(Boxes.getRandomHexString(), 0)
        val collateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 0, WID, 100L)
        val repoOut = Boxes.createRepo(ctx, 10020, 301, 100L, 1)
        val outCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 0, WID, 80L)
        val permitBox = Boxes.createPermitBox(ctx, 60L, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L))
        val permitOut = Boxes.createPermitBox(ctx, 40L, WID)
        val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._1.mainTokens.RSN, 20))
        val tx = ctx.newTxBuilder().addInputs(repoBox, collateral, permitBox, WIDBox)
          .fee(Configs.fee)
          .addOutputs(repoOut, outCollateral, permitOut, userOut)
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
   * @target partial return permit transaction should sign successfully with multiple permit boxes in input
   * @dependencies
   * @scenario
   * - mock user input
   * - mock valid input repo, collateral and wid box
   * - mock valid permi1 and permit2 for wathcer
   * - mock valid output repo, permit, collateral and wid box
   * - build and sign the return permit transaction
   * @expected
   * - successful sign for partial return permit transaction
   */
  property("partial return permit transaction should sign successfully with multiple permit boxes in input") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val repoBox = Boxes.createRepo(ctx, 10000, 351, 100L, 1).convertToInputWith(Boxes.getRandomHexString(), 0)
        val collateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 0, WID, 100L)
        val repoOut = Boxes.createRepo(ctx, 10050, 301, 100L, 1)
        val outCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 0, WID, 50L)
        val permitBox = Boxes.createPermitBox(ctx, 60L, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val permitBox2 = Boxes.createPermitBox(ctx, 40L, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L))
        val permitOut = Boxes.createPermitBox(ctx, 50L, WID)
        val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._1.mainTokens.RSN, 20))
        val tx = ctx.newTxBuilder().addInputs(repoBox, collateral, permitBox, WIDBox, permitBox2)
          .fee(Configs.fee)
          .addOutputs(repoOut, outCollateral, permitOut, userOut)
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
   * @target partial return permit transaction signing should throw error by changing another watcher permit count
   * @dependencies
   * @scenario
   * - mock valid input wid box and permit box for watcher with WID2
   * - mock a fake permit box with WID in registers
   * - mock valid input collateral for watcher with WID
   * - mock valid input and output repo box
   * - mock valid output wid box and permit box for watcher with WID2
   * - mock output collateral with updated permit count for watcher with WID
   * - build and sign the partial return permit transaction
   * @expected
   * - sign error for returning permit WID2 and changing watcher permit count for WID
   */
  property("partial return permit transaction signing should throw error by changing another watcher permit count") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val WID = Base16.decode(Boxes.getRandomHexString()).get
      val WID2 = Base16.decode(Boxes.getRandomHexString()).get
      val repoBox = Boxes.createRepo(ctx, 10000, 321, 100L, 1).convertToInputWith(Boxes.getRandomHexString(), 0)
      val collateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, WID, 100L)
      val repoOut = Boxes.createRepo(ctx, 10020, 301, 100L, 1)
      val outCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, WID, 80L)
      val fakePermit = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, WID, new ErgoToken(Boxes.getRandomHexString(), 60L))
      val permitBox = Boxes.createPermitBox(ctx, 60L, WID2).convertToInputWith(Boxes.getRandomHexString(), 0)
      val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID2, 2L))
      val permitOut = Boxes.createPermitBox(ctx, 40L, WID2)
      val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(WID2, 2L), new ErgoToken(networkConfig._1.mainTokens.RSN, 20))
      val tx = ctx.newTxBuilder().addInputs(repoBox, collateral, fakePermit, WIDBox, permitBox)
        .fee(Configs.fee)
        .addOutputs(repoOut, outCollateral, permitOut, userOut)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        val signedTx = prover.sign(tx)
        println(signedTx.toJson(false))
      }
    })
  }

  /**
   * @target partial return permit transaction signing should throw error by spending another watcher permit box
   * @dependencies
   * @scenario
   * - mock valid input wid box and permit box for watcher with WID
   * - mock valid input collateral for watcher with WID
   * - mock valid input and output repo box
   * - mock a valid permit box with WID2 in registers
   * - mock output collateral and permit for WID
   * - build and sign the partial return permit transaction
   * @expected
   * - sign error for trying to spend another watcher permits
   */
  property("partial return permit transaction signing should throw error by spending another watcher permit box") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val WID = Base16.decode(Boxes.getRandomHexString()).get
      val WID2 = Base16.decode(Boxes.getRandomHexString()).get
      val repoBox = Boxes.createRepo(ctx, 10000, 321, 100L, 1).convertToInputWith(Boxes.getRandomHexString(), 0)
      val collateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, WID, 100L)
      val repoOut = Boxes.createRepo(ctx, 10020, 301, 100L, 1)
      val outCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, WID, 80L)
      val permitBox = Boxes.createPermitBox(ctx, 60L, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
      val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L))
      val anotherPermit = Boxes.createPermitBox(ctx, 60L, WID2).convertToInputWith(Boxes.getRandomHexString(), 1)
      val permitOut = Boxes.createPermitBox(ctx, 100L, WID)
      val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._1.mainTokens.RSN, 20))
      val tx = ctx.newTxBuilder().addInputs(repoBox, collateral, permitBox, WIDBox, anotherPermit)
        .fee(Configs.fee)
        .addOutputs(repoOut, outCollateral, permitOut, userOut)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        val signedTx = prover.sign(tx)
        println(signedTx.toJson(false))
      }
    })
  }

  property("test partially return permits using one wid token") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val WID = Base16.decode(Boxes.getRandomHexString()).get
      val repoBox = Boxes.createRepo(ctx, 100000, 321L, 100L, 2).convertToInputWith(Boxes.getRandomHexString(), 0)
      val permitBox = Boxes.createPermitBox(ctx, 60L, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
      val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 1L))
      val collateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, WID, 100L)
      val repoOut = Boxes.createRepo(ctx, 10020, 301, 100L, 1)
      val outCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, WID, 80L)
      val permitOut = Boxes.createPermitBox(ctx, 40L, WID)
      val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(WID, 1L), new ErgoToken(networkConfig._1.mainTokens.RSN, 20))
      val tx = ctx.newTxBuilder().addInputs(repoBox, collateral, permitBox, WIDBox)
        .fee(Configs.fee)
        .addOutputs(repoOut, outCollateral, permitOut, userOut)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  property("test partially return permits when output permit WID has changed") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val WID = Base16.decode(Boxes.getRandomHexString()).get
      val otherWID = Base16.decode(Boxes.getRandomHexString()).get
      val repoBox = Boxes.createRepoInput(ctx, 100000, 321L, 100L, 1)
      val permitBox = Boxes.createPermitBox(ctx, 60L, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
      val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L))
      val collateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, WID, 100L)
      val repoOut = Boxes.createRepo(ctx, 10020, 301, 100L, 1)
      val outCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, WID, 80L)
      val permitOut = Boxes.createPermitBox(ctx, 40L, otherWID)
      val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(WID, 2L), new ErgoToken(networkConfig._1.mainTokens.RSN, 20))
      val tx = ctx.newTxBuilder().addInputs(repoBox, collateral, permitBox, WIDBox)
        .fee(Configs.fee)
        .addOutputs(repoOut, outCollateral, permitOut, userOut)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        val signedTx = prover.sign(tx)
        println(signedTx.toJson(false))
      }
    })
  }

  property("test complete return permits") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val WID = Boxes.getRandomHexString().getBytes()
        val rwtCount = 20
        val repoBox = Boxes.createRepoInput(ctx, 100000L, 200, 99L, 5)
        val permitBox = Boxes.createPermitBox(ctx, rwtCount, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L))
        val repoOut = Boxes.createRepo(ctx, 100000L + rwtCount, 200 - rwtCount, 100L, 4)
        val watcherCollateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, WID, rwtCount)
        val tx = ctx.newTxBuilder().addInputs(repoBox, watcherCollateral, permitBox, WIDBox)
          .fee(Configs.fee)
          .addOutputs(repoOut)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail(s"transaction not signed")
      }
    })
  }

  /**
   * @target complete return permit transaction should sign successfully with 0 rsn collateral
   * @dependencies
   * @scenario
   * - mock valid input repo, permit and wid box
   * - mock valid collateral without rsn
   * - mock valid output repo box
   * - build and sign the return permit transaction
   * @expected
   * - complete return permit transaction should sign successfully with 0 rsn collateral
   */
  property("complete return permit transaction should sign successfully with 0 rsn collateral") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val WID = Boxes.getRandomHexString().getBytes()
        val rwtCount = 20
        val repoBox = Boxes.createRepoInput(ctx, 100000L, 200, 99L, 5)
        val permitBox = Boxes.createPermitBox(ctx, rwtCount, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L))
        val repoOut = Boxes.createRepo(ctx, 100000L + rwtCount, 200 - rwtCount, 100L, 4)
        val watcherCollateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, WID, rwtCount)
        val tx = ctx.newTxBuilder().addInputs(repoBox, watcherCollateral, permitBox, WIDBox)
          .fee(Configs.fee)
          .addOutputs(repoOut)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail(s"transaction not signed")
      }
    })
  }

  /**
   * @target complete return permit transaction should sign successfully with multiple permit boxes in input
   * @dependencies
   * @scenario
   * - mock valid input repo, collateral and wid box
   * - mock valid permit1 and permit2 for WID
   * - mock valid output repo box
   * - build and sign the return permit transaction
   * @expected
   * - successful sign for complete return permit transaction
   */
  property("complete return permit transaction should sign successfully with multiple permit boxes in input") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val WID = Boxes.getRandomHexString().getBytes()
        val rwtCount = 20
        val repoBox = Boxes.createRepoInput(ctx, 100000L, 200, 99L, 5)
        val permitBox = Boxes.createPermitBox(ctx, rwtCount / 2, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val permitBox2 = Boxes.createPermitBox(ctx, rwtCount / 2, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L))
        val repoOut = Boxes.createRepo(ctx, 100000L + rwtCount, 200 - rwtCount, 100L, 4)
        val watcherCollateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, WID, rwtCount)
        val tx = ctx.newTxBuilder().addInputs(repoBox, watcherCollateral, permitBox, WIDBox, permitBox2)
          .fee(Configs.fee)
          .addOutputs(repoOut)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail(s"transaction not signed")
      }
    })
  }

  property("test complete return permits using one wid token") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val WID = Boxes.getRandomHexString().getBytes()
      val rwtCount = 20
      val repoBox = Boxes.createRepoInput(ctx, 100000L, 200, 99L, 5)
      val permitBox = Boxes.createPermitBox(ctx, rwtCount, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
      val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 1L))
      val repoOut = Boxes.createRepo(ctx, 100000L + rwtCount, 200 - rwtCount, 100L, 4)
      val watcherCollateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, WID, rwtCount)
      val tx = ctx.newTxBuilder().addInputs(repoBox, watcherCollateral, permitBox, WIDBox)
        .fee(Configs.fee)
        .addOutputs(repoOut)
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val WID = Boxes.getRandomHexString().getBytes()
      val rwtCount = 20
      val repoBox = Boxes.createRepoInput(ctx, 100000L, 200, 99L, 5)
      val permitBox = Boxes.createPermitBox(ctx, rwtCount, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
      val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 2L))
      val repoOut = Boxes.createRepo(ctx, 100000L + rwtCount, 200 - rwtCount, 99L, 4)
      val watcherCollateral = Boxes.createFakeWatcherCollateralBox(ctx, 1e9.toLong, 100L, WID, rwtCount).convertToInputWith(Boxes.getRandomHexString(), 1)
      val tx = ctx.newTxBuilder().addInputs(repoBox, watcherCollateral, permitBox, WIDBox)
        .fee(Configs.fee)
        .addOutputs(repoOut)
        .sendChangeTo(prover.getAddress)
        .build()
      assertThrows[AnyRef] {
        prover.sign(tx)
      }
    })
  }

  property("test redeem repo") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val repoConfig = Boxes.createRepoConfigsInput(ctx)
        val repoBox = Boxes.createRepoInput(ctx, 100000, 32001L, 100L, 7)
        val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
        val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
        val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
        val guardBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 0)
        val lockBox = Boxes.createLockBox(ctx, Configs.minBoxValue * 10).convertToInputWith(Boxes.getRandomHexString(), 0)
        val boxBuilder = ctx.newTxBuilder().outBoxBuilder()
          .value(Configs.minBoxValue)
          .contract(ctx.newContract(prover.getAddress.asP2PK().script))
          .registers(
            repoBox.getRegisters.get(0),
            repoBox.getRegisters.get(1),
          )
          .tokens(new ErgoToken(networkConfig._2.tokens.RepoConfigNFT, 1))
        val tx = ctx.newTxBuilder().addInputs(repoConfig, repoBox, lockBox)
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

  /**
   * @target redeem repoConfig transaction should sign successfully
   * @dependencies
   * @scenario
   * - mock repo config
   * - mock guards secrets, public keys and box with the guard NFT
   * - mock lock box to provide transaction fee
   * - build and sign the create trigger transaction
   * @expected
   * - successful sign for repo config redeem transaction
   */
  property("redeem repoConfig transaction should sign successfully") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val repoConfig = Boxes.createRepoConfigsInput(ctx)
        val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
        val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
        val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
        val guardBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 0)
        val lockBox = Boxes.createLockBox(ctx, Configs.minBoxValue * 10).convertToInputWith(Boxes.getRandomHexString(), 0)
        val inputs = Seq(repoConfig, guardBox)
        val boxBuilder = ctx.newTxBuilder().outBoxBuilder()
          .contract(ctx.newContract(prover.getAddress.asP2PK().script))
          .registers(
            repoConfig.getRegisters.get(0),
          )
          .tokens(new ErgoToken(networkConfig._2.tokens.RepoConfigNFT, 1))
        boxBuilder.value(inputs.map(item => item.getValue).sum - Configs.fee)
        val tx = ctx.newTxBuilder().addInputs(repoConfig, lockBox)
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


  /**
   * @target redeem repoConfig transaction signing should throw error when signed by less than required guards
   * @dependencies
   * @scenario
   * - mock repo config
   * - mock commitments for an event for each wid
   * - mock guards secrets, public keys and box with the guard NFT requiring at least 6 signature for update
   * - build and sign the create trigger transaction with only 5 secret
   * @expected
   * - sign error for repo config redeem transaction
   */
  property("redeem repoConfig transaction signing should throw error when signed by less than required guards") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val prover = getProver()
      val repoConfig = Boxes.createRepoConfigsInput(ctx)
      val secrets = (0 until 10).map(ind => Utils.randBigInt.bigInteger)
      val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
      val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
      val guardBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 0)
      val lockBox = Boxes.createLockBox(ctx, Configs.minBoxValue * 10).convertToInputWith(Boxes.getRandomHexString(), 0)
      val inputs = Seq(repoConfig, guardBox)
      val boxBuilder = ctx.newTxBuilder().outBoxBuilder()
        .contract(ctx.newContract(prover.getAddress.asP2PK().script))
        .registers(
          repoConfig.getRegisters.get(0),
        )
        .tokens(new ErgoToken(networkConfig._2.tokens.RepoConfigNFT, 1))
      boxBuilder.value(inputs.map(item => item.getValue).sum - Configs.fee)
      val tx = ctx.newTxBuilder().addInputs(repoConfig, lockBox)
        .fee(Configs.fee)
        .addDataInputs(guardBox)
        .addOutputs(boxBuilder.build())
        .sendChangeTo(prover.getAddress)
        .build()
      val multiSigProverBuilder = ctx.newProverBuilder()
      secrets.slice(0, 5).map(item => multiSigProverBuilder.withDLogSecret(item))
      val multiSigProver = multiSigProverBuilder.build()
      assertThrows[AnyRef] {
        multiSigProver.sign(tx)
      }
    })
  }

  property("test create new commitment") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WID = Base16.decode(Boxes.getRandomHexString()).get
      val box1 = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 1), new ErgoToken(networkConfig._1.mainTokens.RSN, 9))
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val commitmentBox = Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 1l).convertToInputWith(Boxes.getRandomHexString(), 1)
        val box = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 1L))
        val newPermit = Boxes.createPermitBox(ctx, 1, WID)
        val WIDOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(WID, 1))
        val inputs = Seq(commitmentBox, box)
        val redeemUnsigned = ctx.newTxBuilder().addInputs(inputs: _*)
          .fee(Configs.fee)
          .addOutputs(newPermit, WIDOut)
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WIDs = generateRandomWIDList(5)
        val repo = Boxes.createRepo(ctx, 1000L, 10001L, 100L, 5).convertToInputWith(Boxes.getRandomHexString(), 1)
        val repoConfig = Boxes.createRepoConfigsInput(ctx)
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
          println(exp.toString)
          fail("transaction not signed")
      }
    })
  }

  property("test create event trigger for minimum required watcher") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WIDs = generateRandomWIDList(7)
        val repoConfig = Boxes.createRepoConfigsInput(ctx)
        val repo = Boxes.createRepo(ctx, 1000L, 10001L, 100L, 7).convertToInputWith(Boxes.getRandomHexString(), 1)
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

  property("test cant create event trigger for lower than minimum required watcher") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(7)
      val repoConfig = Boxes.createRepoConfigsInput(ctx)
      val repo = Boxes.createRepo(ctx, 1000L, 10001L, 100L, 7).convertToInputWith(Boxes.getRandomHexString(), 1)
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
   * @target create trigger transaction signing should throw error when the commitment count is not valid in trigger box
   * @dependencies
   * @scenario
   * - mock commitment wid list (5 wid)
   * - mock repo config and repo box with the wid list as data input
   * - mock commitments for an event for each wid (for all 5 wid)
   * - mock invalid trigger with wrong commitment count (4 commitment)
   * - build and sign the create trigger transaction
   * @expected
   * - sign error for invalid commitment count in trigger box
   */
  property("create trigger transaction signing should throw error when the commitment count is not valid in trigger box") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(5)
      val repoConfig = Boxes.createRepoConfigsInput(ctx)
      val repo = Boxes.createRepo(ctx, 1000L, 10001L, 100L, 5).convertToInputWith(Boxes.getRandomHexString(), 1)
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
   * - mock commitment wid list (5 wid)
   * - mock repo config and repo box with the wid list as data input
   * - mock commitments for an event for each wid (for all 5 wid)
   * - mock invalid trigger with wrong wid list digest (not considering one wid in digest)
   * - build and sign the create trigger transaction
   * @expected
   * - sign error for invalid wid list digest in trigger box
   */
  property("create trigger transaction signing should throw error when the wid list digest is invalid in trigger box") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(5)
      val repoConfig = Boxes.createRepoConfigsInput(ctx)
      val repo = Boxes.createRepo(ctx, 1000L, 10001L, 100L, 5).convertToInputWith(Boxes.getRandomHexString(), 1)
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
   * @target create trigger transaction signing should throw error with invalid repoConfig from another chain
   * @dependencies
   * @scenario
   * - mock two commitment wid list (5 wid
   * - mock invalid repo config from another chain (different RWT)
   * - mock valid repo box with the wid list as data input
   * - mock commitments for an event for each wid (for all 5 wid)
   * - mock valid trigger
   * - build and sign the create trigger transaction
   * @expected
   * - sign error for trigger creation transaction with invalid repoConfig as data input
   */
  property("create trigger transaction signing should throw error with invalid repoConfig from another chain") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(5)
      val repo = Boxes.createRepo(ctx, 1000L, 10001L, 100L, 5).convertToInputWith(Boxes.getRandomHexString(), 1)
      val repoConfig = Boxes.createRepoConfigs(ctx, Boxes.getRandomHexString()).convertToInputWith(Boxes.getRandomHexString(), 1)
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(7)
      val repoConfig = Boxes.createRepoConfigsInput(ctx)
      val repo = Boxes.createRepoWithTokens(
        ctx,
        1000L,
        10001L,
        100L,
        7,
        networkConfig._1.mainTokens.RepoNFT,
        Boxes.getRandomHexString(),
        networkConfig._2.tokens.AwcNFT
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

  property("test create event trigger with invalid repo box (invalid nft and valid rwt)") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(7)
      val repoConfig = Boxes.createRepoConfigsInput(ctx)
      val repo = Boxes.createRepoWithTokens(
        ctx,
        1000L,
        10001L,
        100L,
        7,
        Boxes.getRandomHexString(),
        networkConfig._2.tokens.RWTId,
        networkConfig._2.tokens.AwcNFT
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(5)
      val repoConfig = Boxes.createRepoConfigsInput(ctx)
      val repo = Boxes.createRepo(ctx, 1000L, 10001L, 100L, 5).convertToInputWith(Boxes.getRandomHexString(), 1)
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
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

  /**
   * @target reward transaction signing should throw error when there is duplicated not-merged commitments in inputs
   * @dependencies
   * @scenario
   * - mock commitment wid list
   * - mock not-merged commitments wid list with a duplicate WID
   * - mock guards secrets, public keys and box with the guard NFT
   * - mock lock box with required tokens
   * - mock trigger box with wid list
   * - mock output permits for all wids except the duplicate one
   * - build and sign the reward transaction
   * @expected
   * - sign error for having duplicate commitment boxes in inputs, and stealing the RWTs by guards
   */
  property("reward transaction signing should throw error when there is duplicated not-merged commitments in inputs") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(7)
      var notMergedWIDs = generateRandomWIDList(3)
      val allWIDs = WIDs ++ notMergedWIDs
      notMergedWIDs = notMergedWIDs ++ Seq(notMergedWIDs(0))
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
      val newPermits = allWIDs.map(item => {
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

  /**
   * @target reward transaction signing should throw error when there is duplicated rewards for duplicated not merged commitments
   * @dependencies
   * @scenario
   * - mock commitment wid list
   * - mock not merged commitments wid list with a duplicate WID
   * - mock guards secrets, public keys and box with the guard NFT
   * - mock lock box with required tokens
   * - mock trigger box with wid list
   * - mock output permits for all wids including the duplicate WID
   * - build and sign the reward transaction
   * @expected
   * - sign error for having duplicate rewards boxes in outputs
   */
  property("reward transaction signing should throw error when there is duplicated rewards for duplicated not merged commitments") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val commitment = new Commitment()
      val prover = getProver()
      val WIDs = generateRandomWIDList(7)
      var notMergedWIDs = generateRandomWIDList(3)
      notMergedWIDs = notMergedWIDs ++ Seq(notMergedWIDs(0))
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
      val newPermits = allWIDs.map(item => {
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

  property("test guard payment with not merged wrong commitment") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      val userIndex = 0
      try {
        val prover = getProver()
        val WIDs = generateRandomWIDList(3)
        val amounts = Seq(100L, 11L, 20L)
        var repo = Boxes.createRepo(ctx, 1000L, amounts.sum + 1, 100L, 3).convertToInputWith(Boxes.getRandomHexString(), 1)
        for (userIndex <- 0 until WIDs.length) {
          val WID = WIDs(userIndex)
          val box2 = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(networkConfig._2.tokens.CleanupNFT, 1L))
          val fraud = Boxes.createFraudBox(ctx, WID, 10L).convertToInputWith(Boxes.getRandomHexString(), 1)
          val RWTCount = repo.getTokens.get(1).getValue + 10
          val RSNCount = repo.getTokens.get(2).getValue - 10
          val collateral = Boxes.createWatcherCollateralBoxInput(ctx, 1e9.toLong, 100, WID, amounts(userIndex))
          val repoCandidate = Boxes.createRepo(ctx, RWTCount, RSNCount, 100L, 3)
          val outCollateral = Boxes.createWatcherCollateralBox(ctx, 1e9.toLong, 100, WID, amounts(userIndex) - 10)
          val unsigned = ctx.newTxBuilder().addInputs(repo, collateral, fraud, box2)
            .fee(Configs.fee)
            .addOutputs(repoCandidate, outCollateral)
            .sendChangeTo(prover.getAddress)
            .build()
          val signed = prover.sign(unsigned)
          repo = signed.getOutputsToSpend.get(0)
        }
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail(s"transaction not signed on index ${userIndex}")
      }
    })
  }

  property("test spent lock script when guard token is in data input") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
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
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
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

  /**
   * @target redeem emission box transaction should sign successfully
   * @dependencies
   * @scenario
   * - mock emission box
   * - mock guards secrets, public keys and box with the guard NFT
   * - build and sign the emission redeem transaction
   * @expected
   * - successful sign for emission redeem transaction
   */
  property("redeem emission box transaction should sign successfully") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
        val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
        val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
        val guardBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 0)
        val ERSNId = Boxes.getRandomHexString()
        val emissionBox = Boxes.createEmissionBox(
          ctx,
          Configs.minBoxValue + Configs.fee,
          new ErgoToken(networkConfig._1.mainTokens.EmissionNFT, 1),
          new ErgoToken(networkConfig._1.mainTokens.RSN, 10000),
          new ErgoToken(ERSNId, 9000)
        ).convertToInputWith(Boxes.getRandomHexString(), 0)
        val inputs = Seq(emissionBox)
        val boxBuilder = ctx.newTxBuilder().outBoxBuilder()
          .contract(ctx.newContract(prover.getAddress.asP2PK().script))
          .tokens(
            new ErgoToken(networkConfig._1.mainTokens.EmissionNFT, 1),
            new ErgoToken(networkConfig._1.mainTokens.RSN, 10000),
            new ErgoToken(ERSNId, 9000)
          )
        boxBuilder.value(inputs.map(item => item.getValue).sum - Configs.fee)
        val tx = ctx.newTxBuilder().addInputs(emissionBox)
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

  /**
   * @target redeem emission box transaction should sign successfully
   * @dependencies
   * @scenario
   * - mock emission box
   * - mock guards secrets, public keys and box with the guard NFT
   * - build and sign the emission redeem transaction with only 5 secrets
   * @expected
   * - sign error for emission redeem transaction
   */
  property("redeem emission box transaction signing should throw error when signed by less than required guards") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
        val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
        val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
        val guardBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 0)
        val ERSNId = Boxes.getRandomHexString()
        val emissionBox = Boxes.createEmissionBox(
          ctx,
          Configs.minBoxValue + Configs.fee,
          new ErgoToken(networkConfig._1.mainTokens.EmissionNFT, 1),
          new ErgoToken(networkConfig._1.mainTokens.RSN, 10000),
          new ErgoToken(ERSNId, 9000)
        ).convertToInputWith(Boxes.getRandomHexString(), 0)
        val inputs = Seq(emissionBox)
        val boxBuilder = ctx.newTxBuilder().outBoxBuilder()
          .contract(ctx.newContract(prover.getAddress.asP2PK().script))
          .tokens(
            new ErgoToken(networkConfig._1.mainTokens.EmissionNFT, 1),
            new ErgoToken(networkConfig._1.mainTokens.RSN, 10000),
            new ErgoToken(ERSNId, 9000)
          )
        boxBuilder.value(inputs.map(item => item.getValue).sum - Configs.fee)
        val tx = ctx.newTxBuilder().addInputs(emissionBox)
          .fee(Configs.fee)
          .addDataInputs(guardBox)
          .addOutputs(boxBuilder.build())
          .sendChangeTo(prover.getAddress)
          .build()
        val multiSigProverBuilder = ctx.newProverBuilder()
        secrets.slice(0, 5).map(item => multiSigProverBuilder.withDLogSecret(item))
        val multiSigProver = multiSigProverBuilder.build()
        assertThrows[AnyRef] {
          multiSigProver.sign(tx)
        }
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("unexpected error")
      }
    })
  }

  /**
   * @target emission transaction should sign successfully
   * @dependencies
   * @scenario
   * - mock user input
   * - mock valid emission box
   * - mock valid output emission and user boxes
   * - build and sign the emission transaction
   * @expected
   * - successful sign for emission transaction
   */
  property("emission transaction should sign successfully") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val ERSNId = Boxes.getRandomHexString()
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(ERSNId, 100L))
        val emissionBox = Boxes.createEmissionBox(
          ctx,
          Configs.minBoxValue,
          new ErgoToken(networkConfig._1.mainTokens.EmissionNFT, 1),
          new ErgoToken(networkConfig._1.mainTokens.RSN, 10000),
          new ErgoToken(ERSNId, 9000)
        ).convertToInputWith(Boxes.getRandomHexString(), 0)

        val emissionOut = ctx.newTxBuilder().outBoxBuilder()
          .contract(contracts.Emission._1)
          .value(Configs.minBoxValue)
          .tokens(
            new ErgoToken(networkConfig._1.mainTokens.EmissionNFT, 1),
            new ErgoToken(networkConfig._1.mainTokens.RSN, 9900),
            new ErgoToken(ERSNId, 9100)
          ).build()
        val userOut = ctx.newTxBuilder().outBoxBuilder()
          .contract(ctx.newContract(prover.getAddress.asP2PK().script))
          .value(userBox.getValue() - Configs.fee)
          .tokens(
            new ErgoToken(networkConfig._1.mainTokens.RSN, 100),
          ).build()
        val tx = ctx.newTxBuilder().addInputs(emissionBox, userBox)
          .fee(Configs.fee)
          .addOutputs(emissionOut, userOut)
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
   * @target emission transaction signing should throw error when out RSN is more than in ERSN
   * @dependencies
   * @scenario
   * - mock user input
   * - mock valid emission box
   * - mock valid output emission and user boxes with more RSN
   * - build and sign the emission transaction
   * @expected
   * - sign error for emission transaction
   */
  property("emission transaction signing should throw error when out RSN is more than in ERSN") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val ERSNId = Boxes.getRandomHexString()
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(ERSNId, 100L))
        val emissionBox = Boxes.createEmissionBox(
          ctx,
          Configs.minBoxValue,
          new ErgoToken(networkConfig._1.mainTokens.EmissionNFT, 1),
          new ErgoToken(networkConfig._1.mainTokens.RSN, 10000),
          new ErgoToken(ERSNId, 9000)
        ).convertToInputWith(Boxes.getRandomHexString(), 0)

        val emissionOut = ctx.newTxBuilder().outBoxBuilder()
          .contract(contracts.Emission._1)
          .value(Configs.minBoxValue)
          .tokens(
            new ErgoToken(networkConfig._1.mainTokens.EmissionNFT, 1),
            new ErgoToken(networkConfig._1.mainTokens.RSN, 9800),
            new ErgoToken(ERSNId, 9100)
          ).build()
        val userOut = ctx.newTxBuilder().outBoxBuilder()
          .contract(ctx.newContract(prover.getAddress.asP2PK().script))
          .value(userBox.getValue() - Configs.fee)
          .tokens(
            new ErgoToken(networkConfig._1.mainTokens.RSN, 200),
          ).build()
        val tx = ctx.newTxBuilder().addInputs(emissionBox, userBox)
          .fee(Configs.fee)
          .addOutputs(emissionOut, userOut)
          .sendChangeTo(prover.getAddress)
          .build()
        assertThrows[AnyRef] {
          prover.sign(tx)
        }
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("unexpected error")
      }
    })
  }

  /**
   * @target emission transaction signing should throw error when RSN is swapped for ERSN
   * @dependencies
   * @scenario
   * - mock user input with RSN
   * - mock valid emission box
   * - mock valid output emission and user boxes
   * - build and sign the emission transaction
   * @expected
   * - sign error for emission transaction
   */
  property("emission transaction signing should throw error when RSN is swapped for ERSN") {
    networkConfig._1.ergoNetwork.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val ERSNId = Boxes.getRandomHexString()
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(networkConfig._1.mainTokens.RSN, 100L))
        val emissionBox = Boxes.createEmissionBox(
          ctx,
          Configs.minBoxValue,
          new ErgoToken(networkConfig._1.mainTokens.EmissionNFT, 1),
          new ErgoToken(networkConfig._1.mainTokens.RSN, 10000),
          new ErgoToken(ERSNId, 9000)
        ).convertToInputWith(Boxes.getRandomHexString(), 0)

        val emissionOut = ctx.newTxBuilder().outBoxBuilder()
          .contract(contracts.Emission._1)
          .value(Configs.minBoxValue)
          .tokens(
            new ErgoToken(networkConfig._1.mainTokens.EmissionNFT, 1),
            new ErgoToken(networkConfig._1.mainTokens.RSN, 10100),
            new ErgoToken(ERSNId, 8900)
          ).build()
        val userOut = ctx.newTxBuilder().outBoxBuilder()
          .contract(ctx.newContract(prover.getAddress.asP2PK().script))
          .value(userBox.getValue() - Configs.fee)
          .tokens(
            new ErgoToken(ERSNId, 100),
          ).build()
        val tx = ctx.newTxBuilder().addInputs(emissionBox, userBox)
          .fee(Configs.fee)
          .addOutputs(emissionOut, userOut)
          .sendChangeTo(prover.getAddress)
          .build()
        assertThrows[AnyRef] {
          prover.sign(tx)
        }
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("unexpected error")
      }
    })
  }

}
