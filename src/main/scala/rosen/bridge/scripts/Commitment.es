{
  // ----------------- REGISTERS
  // R4: Coll[Coll[Byte]] = [WID]
  // R5: Coll[Coll[Byte]] = [Request ID (Hash(TxId))]
  // R6: Coll[Byte] = Event Data Digest
  // R7: Coll[Byte] = Permit Script Digest
  // ----------------- TOKENS
  // 0: X-RWT

  val eventTriggerHash = fromBase64("EVENT_TRIGGER_SCRIPT_HASH");
  val repoNFT = fromBase64("REPO_NFT");
  val trigger = if (blake2b256(INPUTS(0).propositionBytes) == eventTriggerHash) INPUTS(0) else OUTPUTS(0)
  val myWID = SELF.R4[Coll[Coll[Byte]]].get
  val eventData = trigger.R5[Coll[Coll[Byte]]].get.fold(Coll[Byte](), {(a: Coll[Byte], b: Coll[Byte]) => a ++ b })
  if(blake2b256(INPUTS(0).propositionBytes) == eventTriggerHash){
    // Reward Distribution (for missed commitments)
    // [EventTrigger, Commitments[], BridgeWallet] => [WatcherPermits[], BridgeWallet]
    val WIDs = OUTPUTS.filter{(box:Box) 
        => box.tokens.size > 0 && box.tokens(0)._1 == SELF.tokens(0)._1
      }
      .slice(0, trigger.R7[Int].get)
      .map{(box:Box) => box.R4[Coll[Coll[Byte]]].get(0)}
    val permitBox = OUTPUTS.filter {(box:Box) =>
      box.R4[Coll[Coll[Byte]]].isDefined &&
      box.R4[Coll[Coll[Byte]]].get == myWID
    }(0)
    val WIDExists =  WIDs.exists {(WID: Coll[Byte]) => myWID == Coll(WID)}
    sigmaProp(
      allOf(
        Coll(
          blake2b256(permitBox.propositionBytes) == SELF.R7[Coll[Byte]].get,
          permitBox.tokens(0)._1 == SELF.tokens(0)._1,
          permitBox.tokens(0)._2 == SELF.tokens(0)._2,
          // check for duplicates
          WIDExists == false,
          // validate commitment
          blake2b256(eventData ++ myWID(0)) == SELF.R6[Coll[Byte]].get
        )
      )
    )

  } else if (blake2b256(OUTPUTS(0).propositionBytes) == eventTriggerHash){
    // Event Trigger Creation
    // [Commitments[]] + [Repo(DataInput)] => [EventTrigger]
    val commitmentBoxes = INPUTS.filter{ 
      (box: Box) => 
        SELF.propositionBytes == box.propositionBytes && 
        box.tokens.size > 0 && 
        box.tokens(0)._1 == SELF.tokens(0)._1 
      }
    val WIDs = commitmentBoxes.map{(box:Box) => box.R4[Coll[Coll[Byte]]].get(0)}
    val widListDigest = blake2b256(WIDs.fold(Coll[Byte](), {(a: Coll[Byte], b: Coll[Byte]) => a++b}))
    val myWIDCommitments = commitmentBoxes.filter{ (box: Box) => box.R4[Coll[Coll[Byte]]].get == myWID }
    val EventBoxErgs = commitmentBoxes.map { (box: Box) => box.value }.fold(0L, { (a: Long, b: Long) => a + b })
    val repos = CONTEXT.dataInputs
    val repoValidation = allOf(Coll(
      repos.size == repos(0).R4[Coll[Long]].get(6) + 1,
      repos.forall {
        (repo: Box) => {
          repo.tokens(0)._1 == repoNFT &&
          repo.tokens(1)._1 == SELF.tokens(0)._1
        }
      },
      repos.slice(1, repos.size).zip(repos.indices).forall {
        (data: (Box, Int)) => {
          data._1.R5[Coll[Long]].get(0) == data._2
        }
      }
    ))
    val watcherCount = repos.slice(1, repos.size).fold(0L, {(total: Long, b: Box) => b.R5[Coll[Long]].get.size - 1L + total})
    val eventId = blake2b256(trigger.R5[Coll[Coll[Byte]]].get(0))
    val repoR6 = repos(0).R4[Coll[Long]].get
    val maxCommitment = repoR6(3)
    val requiredCommitmentFromFormula: Long = repoR6(2) + repoR6(1) * watcherCount / 100L
    val requiredCommitment = if(maxCommitment < requiredCommitmentFromFormula) {
      maxCommitment
    } else {
      requiredCommitmentFromFormula
    }
    sigmaProp(
      allOf(
        Coll(
          //check repo
          repoValidation,
          // prevent duplicate commitments
          myWIDCommitments.size == 1,
          // verify trigger params
          trigger.value >= EventBoxErgs,
          trigger.R6[Coll[Byte]].get == SELF.R7[Coll[Byte]].get,
          trigger.R7[Int].get == commitmentBoxes.size,
          trigger.R4[Coll[Coll[Byte]]].get(0) == widListDigest,
          // verify commitment to be correct
          blake2b256(eventData ++ myWID(0)) == SELF.R6[Coll[Byte]].get,
          // check event id
          SELF.R5[Coll[Coll[Byte]]].get == Coll(eventId),
          // check commitment count
          commitmentBoxes.size > requiredCommitment,
          // Check required RWT
          SELF.tokens(0)._2 == repoR6(0),
          trigger.tokens(0)._2 == repoR6(0) * commitmentBoxes.size,
          trigger.tokens(0)._1 == SELF.tokens(0)._1
        )
      )
    )
  } else {
    // Commitment Redeem
    // [Commitment, WID] => [Permit, WID]
    sigmaProp(
      allOf(
        Coll(
          SELF.id == INPUTS(0).id,
          OUTPUTS(0).tokens(0)._1 == SELF.tokens(0)._1,
          OUTPUTS(0).tokens(0)._2 == SELF.tokens(0)._2,
          // check WID copied
          OUTPUTS(0).R4[Coll[Coll[Byte]]].get == myWID,
          // check user WID
          INPUTS(1).tokens(0)._1 == myWID(0),
          // check permit contract address
          blake2b256(OUTPUTS(0).propositionBytes) == SELF.R7[Coll[Byte]].get
        )
      )
    )
  }
}
