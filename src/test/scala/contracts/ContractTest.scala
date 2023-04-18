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
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(networkConfig._3.RSN, 100L))
        val repoBox = Boxes.createRepo(ctx, 100000, 1L, Seq(), Seq()).convertToInputWith(Boxes.getRandomHexString(), 0)
        val repoOut = Boxes.createRepo(ctx, 99900, 101L, Seq(repoBox.getId.getBytes), Seq(100L))
        val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
        val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 1L))
        val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
          .fee(Configs.fee)
          .addOutputs(repoOut, permitBox, WID)
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
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(networkConfig._3.RSN, 100L))
        val otherWID = Base16.decode(Boxes.getRandomHexString()).get
        val repoBox = Boxes.createRepo(ctx, 100000, 5801L, Seq(otherWID), Seq(5800L)).convertToInputWith(Boxes.getRandomHexString(), 0)
        val repoOut = Boxes.createRepo(ctx, 99900, 5901L, Seq(otherWID, repoBox.getId.getBytes), Seq(5800L, 100L))
        val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
        val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 1L))
        val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
          .fee(Configs.fee)
          .addOutputs(repoOut, permitBox, WID)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          fail("transaction not signed")
      }
    })
  }

  property("test extend permit when there is just one permit on network") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 1L), new ErgoToken(networkConfig._3.RSN, 100L))
        val repoBox = Boxes.createRepo(ctx, 100000, 5801L, Seq(WID), Seq(5800L)).convertToInputWith(Boxes.getRandomHexString(), 0)
        val repoOut = Boxes.createRepoWithR7(ctx, 99900, 5901L, Seq(WID), Seq(5900L), 1)
        val permitBox = Boxes.createPermitBox(ctx, 100L, WID)
        val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 1L))
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
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 1L), new ErgoToken(networkConfig._3.RSN, 100L))
        val repoBox = Boxes.createRepo(ctx, 100000, 6459L, Seq(WID, otherWID), Seq(58L, 6400L)).convertToInputWith(Boxes.getRandomHexString(), 0)
        val repoOut = Boxes.createRepoWithR7(ctx, 99900, 6559L, Seq(WID, otherWID), Seq(158L, 6400L), 1)
        val permitBox = Boxes.createPermitBox(ctx, 100L, WID)
        val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 1L))
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
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 1L), new ErgoToken(networkConfig._3.RSN, 100L))
        val repoBox = Boxes.createRepo(ctx, 100000, 9659L, Seq(otherWID, WID, otherWID2), Seq(3200L, 58L, 6400L)).convertToInputWith(Boxes.getRandomHexString(), 0)
        val repoOut = Boxes.createRepoWithR7(ctx, 99900, 9759L, Seq(otherWID, WID, otherWID2), Seq(3200L, 158L, 6400L), 2)
        val permitBox = Boxes.createPermitBox(ctx, 100L, WID)
        val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 1L))
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

  property("test extend permit while mutating other permits amount on repo") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val otherWID = Base16.decode(Boxes.getRandomHexString()).get
        val otherWID2 = Base16.decode(Boxes.getRandomHexString()).get
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 1L), new ErgoToken(networkConfig._3.RSN, 100L))
        val repoBox = Boxes.createRepo(ctx, 100000, 9659L, Seq(otherWID, WID, otherWID2), Seq(3200L, 58L, 6400L)).convertToInputWith(Boxes.getRandomHexString(), 0)
        val repoOut = Boxes.createRepoWithR7(ctx, 99900, 9759L, Seq(otherWID, WID, otherWID2), Seq(320L, 158L, 6400L), 2)
        val permitBox = Boxes.createPermitBox(ctx, 100L, WID)
        val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 1L))
        val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
          .fee(Configs.fee)
          .addOutputs(repoOut, permitBox, WIDBox)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
        fail("transaction should not sign, the permit amounts have changed")
      } catch {
        case exp: Throwable =>
          println(exp.toString)
      }
    })
  }

  property("test extend permit while mutating other permits WID on repo") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val otherWID = Base16.decode(Boxes.getRandomHexString()).get
        val otherWID2 = Base16.decode(Boxes.getRandomHexString()).get
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 1L), new ErgoToken(networkConfig._3.RSN, 100L))
        val repoBox = Boxes.createRepo(ctx, 100000, 9659L, Seq(otherWID, WID, otherWID2), Seq(3200L, 58L, 6400L)).convertToInputWith(Boxes.getRandomHexString(), 0)
        val repoOut = Boxes.createRepoWithR7(ctx, 99900, 9759L, Seq(otherWID, WID, otherWID), Seq(3200L, 158L, 6400L), 2)
        val permitBox = Boxes.createPermitBox(ctx, 100L, WID)
        val WIDBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(WID, 1L))
        val tx = ctx.newTxBuilder().addInputs(repoBox, userBox)
          .fee(Configs.fee)
          .addOutputs(repoOut, permitBox, WIDBox)
          .sendChangeTo(prover.getAddress)
          .build()
        prover.sign(tx)
        fail("transaction should not sign, the permit WID have changed")
      } catch {
        case exp: Throwable =>
          println(exp.toString)
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
        val repoBox = Boxes.createRepo(ctx, 100000, 321L, WIDs, Seq(100L, 120L, 60L, 40L)).convertToInputWith(Boxes.getRandomHexString(), 0)
        val permitBox = Boxes.createPermitBox(ctx, 60L, userWID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(userWID, 1L))
        val repoOut = Boxes.createRepoWithR7(ctx, 100020, 301L, WIDs, Seq(100L, 120L, 40L, 40L), 3)
        val permitOut = Boxes.createPermitBox(ctx, 40L, userWID)
        val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(userWID, 1), new ErgoToken(networkConfig._3.RSN, 20))
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
          val repoBox = Boxes.createRepo(ctx, 100000L, totalPermitOut + 1L, WIDs, amounts).convertToInputWith(Boxes.getRandomHexString(), 0)
          val permitBox = Boxes.createPermitBox(ctx, amounts(userIndex), userWID).convertToInputWith(Boxes.getRandomHexString(), 0)
          val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(userWID, 1L))
          val outputWIDs = WIDs.take(userIndex) ++ WIDs.drop(userIndex + 1)
          val outAmounts = amounts.take(userIndex) ++ amounts.drop(userIndex + 1)
          val repoOut = Boxes.createRepoWithR7(ctx, 100000L + amounts(userIndex), (totalPermitOut - amounts(userIndex)) + 1, outputWIDs, outAmounts, userIndex + 1) // 4 + first element in WID list is chain name
          val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(userWID, 1), new ErgoToken(networkConfig._3.RSN, amounts(userIndex)))
          val tx = ctx.newTxBuilder().addInputs(repoBox, permitBox, WIDBox)
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

  property("test complete return of last permit") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val userWID = Base16.decode(Boxes.getRandomHexString()).get
        val repoBox = Boxes.createRepo(ctx, 100000, 41L, Seq(userWID), Seq(40L)).convertToInputWith(Boxes.getRandomHexString(), 0)
        val permitBox = Boxes.createPermitBox(ctx, 40L, userWID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(userWID, 1L))
        val repoOut = Boxes.createRepoWithR7(ctx, 100040, 1L, Seq(), Seq(), 1)
        val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(userWID, 1), new ErgoToken(networkConfig._3.RSN, 40))
        val tx = ctx.newTxBuilder().addInputs(repoBox, permitBox, WIDBox)
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
        val repoBox = Boxes.createRepo(ctx, 100000, 32001L, WIDs, Seq(100L, 120L, 140L, 20L, 40L, 250L, 123L)).convertToInputWith(Boxes.getRandomHexString(), 0)
        val guardBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(networkConfig._3.GuardNFT, 1L))
        val inputs = Seq(repoBox, guardBox)
        val boxBuilder = ctx.newTxBuilder().outBoxBuilder()
          .contract(ctx.newContract(prover.getAddress.asP2PK().script))
          .registers(
            repoBox.getRegisters.get(0),
            repoBox.getRegisters.get(1),
          )
        boxBuilder.value(inputs.map(item => item.getValue).sum - Configs.fee)
        inputs.foreach(box => box.getTokens.forEach(token => boxBuilder.tokens(token)))
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
        val tx = ctx.newTxBuilder().addInputs(permit, box1)
          .fee(Configs.fee)
          .sendChangeTo(prover.getAddress)
          .addOutputs(permitOut, commitmentBox)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("transaction not signed")
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
        val tx = ctx.newTxBuilder().addInputs(permit, box1)
          .fee(Configs.fee)
          .sendChangeTo(prover.getAddress)
          .addOutputs(permitOut, commitmentBox)
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
        val repo = Boxes.createRepo(ctx, 1000L, 10001L, WIDs, Seq(10L, 30L, 20L, 35L, 5L)).convertToInputWith(Boxes.getRandomHexString(), 1)
        val commitments = WIDs.map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 1l).convertToInputWith(Boxes.getRandomHexString(), 1))
        val trigger = Boxes.createTriggerEventBox(ctx, WIDs, commitment)
        val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
        val tx = ctx.newTxBuilder().addInputs(commitments ++ Seq(feeBox): _*)
          .fee(Configs.fee)
          .addOutputs(trigger)
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
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WIDs = generateRandomWIDList(7)
        val repo = Boxes.createRepo(ctx, 1000L, 10001L, WIDs, Seq(10L, 30L, 20L, 25L, 5L, 4L, 6L)).convertToInputWith(Boxes.getRandomHexString(), 1)
        val commitments = WIDs.slice(0, 4).map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 1l).convertToInputWith(Boxes.getRandomHexString(), 1))
        val trigger = Boxes.createTriggerEventBox(ctx, WIDs.slice(0, 4), commitment)
        val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
        val tx = ctx.newTxBuilder().addInputs(commitments ++ Seq(feeBox): _*)
          .fee(Configs.fee)
          .addOutputs(trigger)
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
    networkConfig._1.ergoClient.execute(ctx => {
      assertThrows[AnyRef] {
        val commitment = new Commitment()
        val prover = getProver()
        val WIDs = generateRandomWIDList(7)
        val repo = Boxes.createRepo(ctx, 1000L, 10001L, WIDs, Seq(10L, 30L, 20L, 25L, 5L, 4L, 6L)).convertToInputWith(Boxes.getRandomHexString(), 1)
        val commitments = WIDs.slice(0, 3).map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 1l).convertToInputWith(Boxes.getRandomHexString(), 1))
        val trigger = Boxes.createTriggerEventBox(ctx, WIDs.slice(0, 3), commitment)
        val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
        val tx = ctx.newTxBuilder().addInputs(commitments ++ Seq(feeBox): _*)
          .fee(Configs.fee)
          .addOutputs(trigger)
          .addDataInputs(repo)
          .sendChangeTo(prover.getAddress)
          .build()
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
        val eventTrigger = Boxes.createTriggerEventBox(ctx, WIDs, commitment).convertToInputWith(Boxes.getRandomHexString(), 1)
        val newPermits = WIDs.map(item => {
          Boxes.createPermitBox(ctx, 1, item, new ErgoToken(commitment.targetChainTokenId, userFee))
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

  property("test guard payment with not merged commitment") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WIDs = generateRandomWIDList(7)
        val notMergedWIDs = generateRandomWIDList(3)
        val allWIDs = WIDs ++ notMergedWIDs
        val notMergedCommitments = notMergedWIDs.map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID), 1l).convertToInputWith(Boxes.getRandomHexString(), 1))
        val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
        val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
        val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
        val eventTrigger = Boxes.createTriggerEventBox(ctx, WIDs, commitment).convertToInputWith(Boxes.getRandomHexString(), 1)
        val userFee: Long = math.floor((commitment.fee * 0.6) / allWIDs.length).toLong
        val guardBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 0)
        val lockBox = Boxes.createLockBox(
          ctx,
          Configs.minBoxValue,
          new ErgoToken(commitment.targetChainTokenId, userFee * (WIDs ++ notMergedWIDs).length)
        ).convertToInputWith(Boxes.getRandomHexString(), 0)
        val newPermits = (WIDs ++ notMergedWIDs).map(item => {
          Boxes.createPermitBox(ctx, 1, item, new ErgoToken(commitment.targetChainTokenId, userFee))
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

  property("test create fraud from event trigger") {
    networkConfig._1.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val commitment = new Commitment()
        val WIDs = generateRandomWIDList(7)
        val triggerEvent = Boxes.createTriggerEventBox(ctx, WIDs, commitment).convertToInputWith(Boxes.getRandomHexString(), 0)
        val box1 = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(networkConfig._2.tokens.CleanupNFT, 1L))
        val newFraud = WIDs.indices.map(index => {
          Boxes.createFraudBox(ctx, WIDs(index))
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
        val globalAmounts = Seq(10L, 1L, 2L)
        val repo = Boxes.createRepo(ctx, 1000L, globalAmounts.sum + 1, globalWIDs, globalAmounts).convertToInputWith(Boxes.getRandomHexString(), 1)
        for (userIndex <- 0 until globalWIDs.length) {
          var amounts = globalAmounts.map(item => item).toArray
          var WIDs = globalWIDs.map(item => item).toArray
          val WID = WIDs(userIndex)
          val box2 = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(networkConfig._2.tokens.CleanupNFT, 1L))
          val fraud = Boxes.createFraudBox(ctx, WID).convertToInputWith(Boxes.getRandomHexString(), 1)
          val RWTCount = repo.getTokens.get(1).getValue.toLong + 1
          val RSNCount = repo.getTokens.get(2).getValue.toLong - 1
          if (amounts(userIndex) > 1) {
            amounts(userIndex) -= 1
          } else {
            amounts = amounts.patch(userIndex, Nil, 1)
            WIDs = WIDs.patch(userIndex, Nil, 1)
          }
          val repoCandidate = Boxes.createRepoWithR7(ctx, RWTCount, RSNCount, WIDs, amounts, userIndex + 1)
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
        val outSignBox = Boxes.createGuardNftBox(ctx, guardsPks, 4,6)
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
      assertThrows[AnyRef] {
        val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
        val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
        val prover = guards(0)
        val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
        val signBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 1)
        val box2 = Boxes.createBoxForUser(ctx, guards(0).getAddress, 1e9.toLong)
        val outSignBox = Boxes.createGuardNftBox(ctx, guardsPks, 4,6)
        val outBox = Boxes.createBoxCandidateForUser(ctx, guards(1).getAddress, 1e8.toLong)
        val tx = ctx.newTxBuilder().addInputs(signBox, box2)
          .fee(Configs.fee)
          .addOutputs(outSignBox, outBox)
          .sendChangeTo(prover.getAddress)
          .build()
        val proverBuilder = ctx.newProverBuilder()
        secrets.slice(0, 5).map(item => proverBuilder.withDLogSecret(item))
        proverBuilder.build().sign(tx)
      }
    })
  }
}
