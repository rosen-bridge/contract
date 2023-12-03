{
  // ----------------- REGISTERS
  // R4: Coll[Coll[Byte]] [WID[]]
  // R5: Coll[Coll[Byte]] Event data
  // R6: Coll[Byte] Permit contract script digest
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
  val WIDs: Coll[Coll[Byte]] = SELF.R4[Coll[Coll[Byte]]].get
  val mergeBoxes = OUTPUTS.slice(0, WIDs.size)
  val checkAllWIDs = WIDs.zip(mergeBoxes).forall {
    (data: (Coll[Byte], Box)) => {
      Coll(data._1) == data._2.R4[Coll[Coll[Byte]]].get && 
      data._2.propositionBytes == OUTPUTS(0).propositionBytes &&
      data._2.tokens(0)._1 == SELF.tokens(0)._1 &&
      data._2.tokens(0)._2 == SELF.tokens(0)._2 / WIDs.size
    }
  }
  sigmaProp(
    allOf(
      Coll(
        WIDs.size == mergeBoxes.size,
        checkAllWIDs,
        fraudScriptCheck,
      )
    )
  )
}
