{
  // ----------------- REGISTERS
  // R4: Coll[Long] = [Commitment RWT count, Watcher quorum percentage, approval offset, maximum needed approval, 
  //                   Collateral Erg amount, Collateral Rsn Amount, Total repo count]
  // (Minimum number of commitments needed for an event is: 
  //  min(R4[3], R4[1] * (total number of watchers) / 100 + R4[2]))
  // ----------------- TOKENS
  // 0: X-Repo Config NFT
  
  val GuardNFT = fromBase64("GUARD_NFT");
  val GuardBox = CONTEXT.dataInputs(0);
  val paymentSignCount = GuardBox.R5[Coll[Int]].get(1);
  val signedColl = GuardBox.R4[Coll[Coll[Byte]]].get.map { (row: Coll[Byte]) => proveDlog(decodePoint(row)) };
  val verifyGuard = GuardBox.tokens.exists { (token: (Coll[Byte], Long)) => token._1 == GuardNFT };
  sigmaProp(
    allOf(
      Coll(
        verifyGuard,
        atLeast(paymentSignCount, signedColl)
      )
    )
  )
}
