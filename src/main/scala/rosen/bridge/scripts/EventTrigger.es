{
  // ----------------- REGISTERS
  // R4: Coll[Coll[Byte]] Event data
  // R5: Coll[Byte] WID list digest
  // R6: Coll[Byte] Permit contract script digest
  // R7: Int Watchers Count
  // ----------------- TOKENS
  // 0: RWT

  // In case of fraud: [TriggerEvent, CleanupToken] => [Fraud1, Fraud2, ...]
  // In case of payment: [TriggerEvent, Commitments(if exists), LockBox](dataInput: GuardNFTBox) => [Permit1, ..., changeBox]
  val cleanupNFT = fromBase64("CLEANUP_NFT");
  val cleanupConfirmation = CLEANUP_CONFIRMATION;
  val LockScriptHash = fromBase64("LOCK_SCRIPT_HASH");
  val FraudScriptHash = fromBase64("FRAUD_SCRIPT_HASH");
  val GuardLockExists = INPUTS.exists { (box: Box) => blake2b256(box.propositionBytes) == LockScriptHash}
  val fraudScriptCheck = if(blake2b256(OUTPUTS(0).propositionBytes) == FraudScriptHash) {
    allOf(
      Coll(
        INPUTS(1).tokens(0)._1 == cleanupNFT,
        HEIGHT - cleanupConfirmation >= SELF.creationInfo._1
      )
    )
  } else {
    allOf(
      Coll(
        GuardLockExists,
        blake2b256(OUTPUTS(0).propositionBytes) == SELF.R6[Coll[Byte]].get
      )
    )
  }
  val watcherCount = SELF.R7[Int].get
  val rewards = OUTPUTS.filter{(box:Box) 
      => box.tokens.size > 0 && box.tokens(0)._1 == SELF.tokens(0)._1
    }
    .slice(0, watcherCount)
  val WIDs = rewards.map{(box:Box) => box.R4[Coll[Coll[Byte]]].get(0)}
  val widListDigest = blake2b256(WIDs.fold(Coll[Byte](), {(a: Coll[Byte], b: Coll[Byte]) => a++b}))
  val checkAllWIDs = rewards.forall {
    (data: Box) => {
      data.propositionBytes == OUTPUTS(0).propositionBytes &&
      data.tokens(0)._1 == SELF.tokens(0)._1 &&
      data.tokens(0)._2 == SELF.tokens(0)._2 / watcherCount
    }
  }
  sigmaProp(
    allOf(
      Coll(
        SELF.R5[Coll[Byte]].get == widListDigest,
        checkAllWIDs,
        fraudScriptCheck,
      )
    )
  )
}
