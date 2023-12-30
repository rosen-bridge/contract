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
  val eventData = trigger.R5[Coll[Coll[Byte]]].get.fold(Coll[Byte](), {(a: Coll[Byte], b: Coll[Byte]) => a++b})
  if(blake2b256(INPUTS(0).propositionBytes) == eventTriggerHash){
    val WIDs = OUTPUTS.filter{(box:Box) 
        => box.tokens.size > 0 && box.tokens(0)._1 == SELF.tokens(0)._1
      }
      .slice(0, trigger.R7[Int].get)
      .map{(box:Box) => box.R4[Coll[Coll[Byte]]].get(0)}
    // Reward Distribution (for missed commitments)
    // [EventTrigger, Commitments[], BridgeWallet] => [WatcherPermits[], BridgeWallet]
    val permitBoxes = OUTPUTS.filter {(box:Box) =>
      blake2b256(box.propositionBytes) == SELF.R7[Coll[Byte]].get &&
      box.R4[Coll[Coll[Byte]]].isDefined &&
      box.R4[Coll[Coll[Byte]]].get == myWID
    }
    val WIDExists =  WIDs.exists {(WID: Coll[Byte]) => myWID == Coll(WID)}
    sigmaProp(
      allOf(
        Coll(
          permitBoxes.size == 1,
          permitBoxes(0).tokens(0)._1 == SELF.tokens(0)._1,
          permitBoxes(0).tokens(0)._2 == SELF.tokens(0)._2,
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
    val WIDs = INPUTS.filter{
      (box:Box) => box.tokens.size > 0 && box.tokens(0)._1 == SELF.tokens(0)._1
      }
      .map{(box:Box) => box.R4[Coll[Coll[Byte]]].get(0)}
    val commitmentBoxes = INPUTS.filter { (box: Box) => SELF.propositionBytes == box.propositionBytes }
    val myWIDCommitments = commitmentBoxes.filter{ (box: Box) => box.R4[Coll[Coll[Byte]]].get == myWID }
    val EventBoxErgs = commitmentBoxes.map { (box: Box) => box.value }.fold(0L, { (a: Long, b: Long) => a + b })
    val myWIDExists = WIDs.exists{ (WID: Coll[Byte]) => Coll(WID) == myWID }
    val repo = CONTEXT.dataInputs(0)
    val requestId = blake2b256(trigger.R5[Coll[Coll[Byte]]].get(0))
    val repoR6 = repo.R6[Coll[Long]].get
    val maxCommitment = repoR6(3)
    val requiredCommitmentFromFormula: Long = repoR6(2) + repoR6(1) * (repo.R4[Coll[Coll[Byte]]].get.size - 1L) / 100L
    val requiredCommitment = if(maxCommitment < requiredCommitmentFromFormula) {
      maxCommitment
    } else {
      requiredCommitmentFromFormula
    }
    sigmaProp(
      allOf(
        Coll(
          //check repo
          repo.tokens(0)._1 == repoNFT,
          repo.tokens(1)._1 == SELF.tokens(0)._1,

          OUTPUTS(0).value >= EventBoxErgs,
          myWIDCommitments.size == 1,
          myWIDExists,
          trigger.R6[Coll[Byte]].get == SELF.R7[Coll[Byte]].get,
          WIDs.size == commitmentBoxes.size,
          // verify commitment to be correct
          blake2b256(eventData ++ myWID(0)) == SELF.R6[Coll[Byte]].get,
          // check event id
          SELF.R5[Coll[Coll[Byte]]].get == Coll(requestId),
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
