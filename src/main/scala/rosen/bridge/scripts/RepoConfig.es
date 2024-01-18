{
  // ----------------- REGISTERS
  // R4: Coll[Long] = [Commitment RWT count, Watcher quorum percentage, approval offset, maximum needed approval, 
  //                   Collateral Erg amount, Collateral Rsn Amount, Total repo count]
  // (Minimum number of commitments needed for an event is: 
  //  min(R4[3], R4[1] * (total number of watchers) / 100 + R4[2]))
  // ----------------- TOKENS
  // 0: X-RWT Repo NFT
  // 1: X-RWT
  
  val GuardNFT = fromBase64("GUARD_NFT");
  sigmaProp(INPUTS(0).tokens(0)._1 == GuardNFT)
}
