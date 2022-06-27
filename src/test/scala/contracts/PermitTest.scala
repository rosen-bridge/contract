package contracts

import helpers.{Configs, Utils}
import org.ergoplatform.appkit.{ErgoProver, ErgoToken}
import rosen.bridge.{Contracts}
import scorex.util.encode.{Base16}
import testUtils.{Boxes, Commitment, TestSuite}

import scala.collection.JavaConverters._

class PermitTest extends TestSuite {
  val sk = Utils.randBigInt

  def getProver(): ErgoProver = {
    Configs.ergoClient.execute(ctx => {
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
    Configs.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(Configs.tokens.RSN, 10000L))
        val repoBox = Boxes.createRepo(ctx, 100000, 1L, Seq(), Seq(), 0).convertToInputWith(Boxes.getRandomHexString(), 0);
        val repoOut = Boxes.createRepo(ctx, 99900, 10001L, Seq(repoBox.getId.getBytes), Seq(100L), 0)
        val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
        val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 1L))
        val tx = ctx.newTxBuilder().boxesToSpend(Seq(repoBox, userBox).asJava)
          .fee(Configs.fee)
          .outputs(repoOut, permitBox, WID)
          .sendChangeTo(prover.getAddress.getErgoAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          fail("transaction not signed")
      }
    })
  }

  property("test get permit when there is another permit on network") {
    Configs.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val userBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(Configs.tokens.RSN, 10000L))
        val otherWID = Base16.decode(Boxes.getRandomHexString()).get
        val repoBox = Boxes.createRepo(ctx, 100000, 5801L, Seq(otherWID), Seq(58L), 0).convertToInputWith(Boxes.getRandomHexString(), 0);
        val repoOut = Boxes.createRepo(ctx, 99900, 15801L, Seq(otherWID, repoBox.getId.getBytes), Seq(58L, 100L), 0)
        val permitBox = Boxes.createPermitBox(ctx, 100L, repoBox.getId.getBytes)
        val WID = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(repoBox.getId.getBytes, 1L))
        val tx = ctx.newTxBuilder().boxesToSpend(Seq(repoBox, userBox).asJava)
          .fee(Configs.fee)
          .outputs(repoOut, permitBox, WID)
          .sendChangeTo(prover.getAddress.getErgoAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          fail("transaction not signed")
      }
    })
  }

  property("test partially return permits") {
    Configs.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val userWID = Base16.decode(Boxes.getRandomHexString()).get
        val WIDs = Seq(
          Base16.decode(Boxes.getRandomHexString()).get,
          Base16.decode(Boxes.getRandomHexString()).get,
          userWID,
          Base16.decode(Boxes.getRandomHexString()).get
        )
        val repoBox = Boxes.createRepo(ctx, 100000, 32001L, WIDs, Seq(100L, 120L, 60L, 40L), 0).convertToInputWith(Boxes.getRandomHexString(), 0);
        val permitBox = Boxes.createPermitBox(ctx, 60L, userWID).convertToInputWith(Boxes.getRandomHexString(), 0);
        val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(userWID, 1L))
        val repoOut = Boxes.createRepo(ctx, 100020, 30001L, WIDs, Seq(100L, 120L, 40L, 40L), 3)
        val permitOut = Boxes.createPermitBox(ctx, 40L, userWID)
        val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(userWID, 1), new ErgoToken(Configs.tokens.RSN, 2000))
        val tx = ctx.newTxBuilder().boxesToSpend(Seq(repoBox, permitBox, WIDBox).asJava)
          .fee(Configs.fee)
          .outputs(repoOut, permitOut, userOut)
          .sendChangeTo(prover.getAddress.getErgoAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          fail("transaction not signed")
      }
    })
  }

  property("test complete return permits") {
    Configs.ergoClient.execute(ctx => {
      var userIndex = 0
      try {
        val prover = getProver()
        val WIDs = generateRandomWIDList(6)
        val amounts = Seq(100L, 120L, 140L, 20L, 40L, 250L)
        for (userIndex <- 0 to 5) {
          val userWID = WIDs(userIndex)
          val totalPermitOut = amounts.sum
          val repoBox = Boxes.createRepo(ctx, 100000L, totalPermitOut * 100L + 1L, WIDs, amounts, 0).convertToInputWith(Boxes.getRandomHexString(), 0);
          val permitBox = Boxes.createPermitBox(ctx, amounts(userIndex), userWID).convertToInputWith(Boxes.getRandomHexString(), 0);
          val WIDBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(userWID, 1L))
          val outputWIDs = WIDs.take(userIndex) ++ WIDs.drop(userIndex + 1)
          val outAmounts = amounts.take(userIndex) ++ amounts.drop(userIndex + 1)
          val repoOut = Boxes.createRepo(ctx, 100000L + amounts(userIndex), (totalPermitOut - amounts(userIndex)) * 100 + 1, outputWIDs, outAmounts, userIndex + 1) // 4 + first element in WID list is chain name
          val userOut = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 1e8.toLong, new ErgoToken(userWID, 1), new ErgoToken(Configs.tokens.RSN, 2000))
          val tx = ctx.newTxBuilder().boxesToSpend(Seq(repoBox, permitBox, WIDBox).asJava)
            .fee(Configs.fee)
            .outputs(repoOut, userOut)
            .sendChangeTo(prover.getAddress.getErgoAddress)
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

  property("test redeem repo") {
    Configs.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val WIDs = generateRandomWIDList(7)
        val repoBox = Boxes.createRepo(ctx, 100000, 32001L, WIDs, Seq(100L, 120L, 140L, 20L, 40L, 250L, 123L), 0).convertToInputWith(Boxes.getRandomHexString(), 0);
        val guardBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(Configs.tokens.GuardNFT, 1L));
        val inputs = Seq(repoBox, guardBox)
        val boxBuilder = ctx.newTxBuilder().outBoxBuilder()
          .contract(ctx.newContract(prover.getAddress.asP2PK().script))
          .registers(
            repoBox.getRegisters.get(0),
            repoBox.getRegisters.get(1),
            repoBox.getRegisters.get(2),
          )
        boxBuilder.value(inputs.map(item => item.getValue).sum - Configs.fee)
        inputs.foreach(box => box.getTokens.forEach(token => boxBuilder.tokens(token)))
        val tx = ctx.newTxBuilder().boxesToSpend(Seq(repoBox, guardBox).asJava)
          .fee(Configs.fee)
          .outputs(boxBuilder.build())
          .sendChangeTo(prover.getAddress.getErgoAddress)
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
    Configs.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val box1 = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 1))
        val permit = Boxes.createPermitBox(ctx, 10L, WID).convertToInputWith(Boxes.getRandomHexString(), 0)
        val permitOut = Boxes.createPermitBox(ctx, 9L, WID)
        val commitmentBox = Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID))
        val tx = ctx.newTxBuilder().boxesToSpend(Seq(permit, box1).asJava)
          .fee(Configs.fee)
          .sendChangeTo(prover.getAddress.getErgoAddress)
          .outputs(permitOut, commitmentBox)
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
    Configs.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WID = Base16.decode(Boxes.getRandomHexString()).get
        val commitmentBox = Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID)).convertToInputWith(Boxes.getRandomHexString(), 1)
        val box = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(WID, 1L));
        val newPermit = Boxes.createPermitBox(ctx, 1, WID)
        val inputs = Seq(commitmentBox, box)
        val redeemUnsigned = ctx.newTxBuilder().boxesToSpend(inputs.asJava)
          .fee(Configs.fee)
          .outputs(newPermit)
          .sendChangeTo(prover.getAddress.getErgoAddress)
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
    Configs.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WIDs = generateRandomWIDList(5)
        val repo = Boxes.createRepo(ctx, 1000L, 10001L, WIDs, Seq(10L, 30L, 20L, 35L, 5L), 0).convertToInputWith(Boxes.getRandomHexString(), 1)
        val commitments = WIDs.map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID)).convertToInputWith(Boxes.getRandomHexString(), 1))
        val trigger = Boxes.createTriggerEventBox(ctx, WIDs, commitment)
        val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
        val tx = ctx.newTxBuilder().boxesToSpend((commitments ++ Seq(feeBox)).asJava)
          .fee(Configs.fee)
          .outputs(trigger)
          .withDataInputs(Seq(repo).asJava)
          .sendChangeTo(prover.getAddress.getErgoAddress)
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
    Configs.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WIDs = generateRandomWIDList(7)
        val repo = Boxes.createRepo(ctx, 1000L, 10001L, WIDs, Seq(10L, 30L, 20L, 25L, 5L, 4L, 6L), 0).convertToInputWith(Boxes.getRandomHexString(), 1)
        val commitments = WIDs.slice(0, 4).map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID)).convertToInputWith(Boxes.getRandomHexString(), 1))
        val trigger = Boxes.createTriggerEventBox(ctx, WIDs.slice(0, 4), commitment)
        val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
        val tx = ctx.newTxBuilder().boxesToSpend((commitments ++ Seq(feeBox)).asJava)
          .fee(Configs.fee)
          .outputs(trigger)
          .withDataInputs(Seq(repo).asJava)
          .sendChangeTo(prover.getAddress.getErgoAddress)
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
    Configs.ergoClient.execute(ctx => {
      assertThrows[AnyRef] {
        val commitment = new Commitment()
        val prover = getProver()
        val WIDs = generateRandomWIDList(7)
        val repo = Boxes.createRepo(ctx, 1000L, 10001L, WIDs, Seq(10L, 30L, 20L, 25L, 5L, 4L, 6L), 0).convertToInputWith(Boxes.getRandomHexString(), 1)
        val commitments = WIDs.slice(0, 3).map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID)).convertToInputWith(Boxes.getRandomHexString(), 1))
        val trigger = Boxes.createTriggerEventBox(ctx, WIDs.slice(0, 3), commitment)
        val feeBox = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong)
        val tx = ctx.newTxBuilder().boxesToSpend((commitments ++ Seq(feeBox)).asJava)
          .fee(Configs.fee)
          .outputs(trigger)
          .withDataInputs(Seq(repo).asJava)
          .sendChangeTo(prover.getAddress.getErgoAddress)
          .build()
        prover.sign(tx)
      }
    })
  }

  property("test guard payment without not merged commitment") {
    Configs.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WIDs = generateRandomWIDList(7)
        val wRepo = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(commitment.targetChainTokenId, commitment.fee))
        val guardBox = Boxes.createBoxForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(Configs.tokens.GuardNFT, 1))
        val eventTrigger = Boxes.createTriggerEventBox(ctx, WIDs, commitment).convertToInputWith(Boxes.getRandomHexString(), 1)
        val userFee: Long = math.floor((commitment.fee * 0.6) / WIDs.length).toLong
        val newPermits = WIDs.map(item => {
          Boxes.createPermitBox(ctx, 1, item, new ErgoToken(commitment.targetChainTokenId, userFee))
        })
        val inputs = Seq(eventTrigger, guardBox, wRepo)
        val unsignedTx = ctx.newTxBuilder().boxesToSpend(inputs.asJava)
          .fee(Configs.fee)
          .sendChangeTo(prover.getAddress.getErgoAddress)
          .outputs(newPermits: _*)
          .build()
        prover.sign(unsignedTx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("transaction not signed")
      }
    })
  }

  property("test guard payment with not merged commitment") {
    Configs.ergoClient.execute(ctx => {
      try {
        val commitment = new Commitment()
        val prover = getProver()
        val WIDs = generateRandomWIDList(7)
        val notMergedWIDs = generateRandomWIDList(3)
        val allWIDs = WIDs ++ notMergedWIDs
        val notMergedCommitments = notMergedWIDs.map(WID => Boxes.createCommitment(ctx, WID, commitment.requestId(), commitment.hash(WID)).convertToInputWith(Boxes.getRandomHexString(), 1))
        val wRepo = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(commitment.targetChainTokenId, commitment.fee))
        val guardBox = Boxes.createBoxForUser(ctx, prover.getAddress, Configs.minBoxValue, new ErgoToken(Configs.tokens.GuardNFT, 1))
        val eventTrigger = Boxes.createTriggerEventBox(ctx, WIDs, commitment).convertToInputWith(Boxes.getRandomHexString(), 1)
        val userFee: Long = math.floor((commitment.fee * 0.6) / allWIDs.length).toLong
        val newPermits = (WIDs ++ notMergedWIDs).map(item => {
          Boxes.createPermitBox(ctx, 1, item, new ErgoToken(commitment.targetChainTokenId, userFee))
        })
        val inputs = Seq(eventTrigger, guardBox, wRepo) ++ notMergedCommitments
        val unsignedTx = ctx.newTxBuilder().boxesToSpend(inputs.asJava)
          .fee(Configs.fee)
          .sendChangeTo(prover.getAddress.getErgoAddress)
          .outputs(newPermits: _*)
          .build()
        prover.sign(unsignedTx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail("transaction not signed")
      }
    })
  }

  property("test create fraud from event trigger") {
    Configs.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val commitment = new Commitment()
        val WIDs = generateRandomWIDList(7)
        val triggerEvent = Boxes.createTriggerEventBox(ctx, WIDs, commitment).convertToInputWith(Boxes.getRandomHexString(), 0)
        val box1 = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(Configs.tokens.CleanupNFT, 1L))
        val newFraud = WIDs.indices.map(index => {
          Boxes.createFraudBox(ctx, WIDs(index))
        })
        val unsignedTx = ctx.newTxBuilder().boxesToSpend(Seq(triggerEvent, box1).asJava)
          .fee(Configs.fee)
          .sendChangeTo(prover.getAddress.getErgoAddress)
          .outputs(newFraud: _*)
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
    Configs.ergoClient.execute(ctx => {
      var userIndex = 0;
      try {
        val prover = getProver()
        val globalWIDs = generateRandomWIDList(3)
        val globalAmounts = Seq(10L, 1L, 2L)
        val repo = Boxes.createRepo(ctx, 1000L, 100 * globalAmounts.sum + 1, globalWIDs, globalAmounts, 0).convertToInputWith(Boxes.getRandomHexString(), 1)
        for (userIndex <- 0 until globalWIDs.length) {
          var amounts = globalAmounts.map(item => item).toArray
          var WIDs = globalWIDs.map(item => item).toArray
          val WID = WIDs(userIndex)
          val box2 = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(Configs.tokens.CleanupNFT, 1L))
          val fraud = Boxes.createFraudBox(ctx, WID).convertToInputWith(Boxes.getRandomHexString(), 1)
          val RWTCount = repo.getTokens.get(1).getValue.toLong + 1
          val RSNCount = repo.getTokens.get(2).getValue.toLong - 100
          if (amounts(userIndex) > 1) {
            amounts(userIndex) -= 1;
          } else {
            amounts = amounts.patch(userIndex, Nil, 1)
            WIDs = WIDs.patch(userIndex, Nil, 1)
          }
          val repoCandidate = Boxes.createRepo(ctx, RWTCount, RSNCount, WIDs, amounts, userIndex + 1)
          val unsigned = ctx.newTxBuilder().boxesToSpend(Seq(repo, fraud, box2).asJava)
            .fee(Configs.fee)
            .outputs(repoCandidate)
            .sendChangeTo(prover.getAddress.getErgoAddress)
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

  property("test Lock Script when guard token is at first index") {
    Configs.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val box = Boxes.createCustomBox(ctx, Contracts.lock, 1e9.toLong)
        val boxNft = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(Configs.tokens.GuardNFT, 1L))
        val outBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 11e8.toLong, new ErgoToken(Configs.tokens.GuardNFT, 1L))
        val tx = ctx.newTxBuilder().boxesToSpend(Seq(boxNft, box).asJava)
          .fee(Configs.fee)
          .outputs(outBox)
          .sendChangeTo(prover.getAddress.getErgoAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail(s"transaction not signed")
      }
    })
  }

  property("test Lock Script when guard token is at second index") {
    Configs.ergoClient.execute(ctx => {
      try {
        val prover = getProver()
        val box = Boxes.createCustomBox(ctx, Contracts.lock, 1e9.toLong)
        val boxNft = Boxes.createBoxForUser(ctx, prover.getAddress, 1e9.toLong, new ErgoToken(Configs.tokens.GuardNFT, 1L))
        val outBox = Boxes.createBoxCandidateForUser(ctx, prover.getAddress, 11e8.toLong, new ErgoToken(Configs.tokens.GuardNFT, 1L))
        val tx = ctx.newTxBuilder().boxesToSpend(Seq(box, boxNft).asJava)
          .fee(Configs.fee)
          .outputs(outBox)
          .sendChangeTo(prover.getAddress.getErgoAddress)
          .build()
        prover.sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail(s"transaction not signed")
      }
    })
  }

  property("test guard nft box spend without update with 5 sign.") {
    Configs.ergoClient.execute(ctx => {
      try {
        val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
        val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
        val prover = guards(0)
        val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
        val signBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 1)
        val box2 = Boxes.createBoxForUser(ctx, guards(0).getAddress, 1e9.toLong)
        val outSignBox = Boxes.createGuardNftBox(ctx, guardsPks, 5,6)
        val outBox = Boxes.createBoxCandidateForUser(ctx, guards(1).getAddress, 1e8.toLong)
        val tx = ctx.newTxBuilder().boxesToSpend(Seq(signBox, box2).asJava)
          .fee(Configs.fee)
          .outputs(outSignBox, outBox)
          .sendChangeTo(prover.getAddress.getErgoAddress)
          .build()
        val proverBuilder = ctx.newProverBuilder()
        secrets.slice(0, 5).map(item => proverBuilder.withDLogSecret(item))
        proverBuilder.build().sign(tx)
      } catch {
        case exp: Throwable =>
          println(exp.toString)
          fail(s"transaction not signed")
      }
    })
  }

  property("test guard nft box cant spend without update with 4 sign") {
    Configs.ergoClient.execute(ctx => {
      assertThrows[AnyRef] {
        val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
        val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
        val prover = guards(0)
        val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
        val signBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 1)
        val box2 = Boxes.createBoxForUser(ctx, guards(0).getAddress, 1e9.toLong)
        val outSignBox = Boxes.createGuardNftBox(ctx, guardsPks, 5,6)
        val outBox = Boxes.createBoxCandidateForUser(ctx, guards(1).getAddress, 1e8.toLong)
        val tx = ctx.newTxBuilder().boxesToSpend(Seq(signBox, box2).asJava)
          .fee(Configs.fee)
          .outputs(outSignBox, outBox)
          .sendChangeTo(prover.getAddress.getErgoAddress)
          .build()
        val proverBuilder = ctx.newProverBuilder()
        secrets.slice(0, 4).map(item => proverBuilder.withDLogSecret(item))
        proverBuilder.build().sign(tx)
      }
    })
  }

  property("test guard nft box spend with update with 6 sign.") {
    Configs.ergoClient.execute(ctx => {
      try {
        val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
        val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
        val prover = guards(0)
        val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
        val signBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 1)
        val box2 = Boxes.createBoxForUser(ctx, guards(0).getAddress, 1e9.toLong)
        val outSignBox = Boxes.createGuardNftBox(ctx, guardsPks, 4,6)
        val outBox = Boxes.createBoxCandidateForUser(ctx, guards(1).getAddress, 1e8.toLong)
        val tx = ctx.newTxBuilder().boxesToSpend(Seq(signBox, box2).asJava)
          .fee(Configs.fee)
          .outputs(outSignBox, outBox)
          .sendChangeTo(prover.getAddress.getErgoAddress)
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
    Configs.ergoClient.execute(ctx => {
      assertThrows[AnyRef] {
        val secrets = (0 until 7).map(ind => Utils.randBigInt.bigInteger)
        val guards = secrets.map(item => ctx.newProverBuilder().withDLogSecret(item).build())
        val prover = guards(0)
        val guardsPks = guards.map(item => item.getAddress.getPublicKey.pkBytes).toArray
        val signBox = Boxes.createGuardNftBox(ctx, guardsPks, 5, 6).convertToInputWith(Boxes.getRandomHexString(), 1)
        val box2 = Boxes.createBoxForUser(ctx, guards(0).getAddress, 1e9.toLong)
        val outSignBox = Boxes.createGuardNftBox(ctx, guardsPks, 4,6)
        val outBox = Boxes.createBoxCandidateForUser(ctx, guards(1).getAddress, 1e8.toLong)
        val tx = ctx.newTxBuilder().boxesToSpend(Seq(signBox, box2).asJava)
          .fee(Configs.fee)
          .outputs(outSignBox, outBox)
          .sendChangeTo(prover.getAddress.getErgoAddress)
          .build()
        val proverBuilder = ctx.newProverBuilder()
        secrets.slice(0, 5).map(item => proverBuilder.withDLogSecret(item))
        proverBuilder.build().sign(tx)
      }
    })
  }
}
