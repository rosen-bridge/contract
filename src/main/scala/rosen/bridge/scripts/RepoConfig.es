{
  // ----------------- REGISTERS
  // R6: Coll[Long] = [Commitment RWT count, Watcher quorum percentage, minimum needed approval, maximum needed approval, Collateral Erg amount, Collateral Rsn Amount, Total repo count]
  // (Minimum number of commitments needed for an event is: min(R6[3], R6[1] * (len(R4) - 1) / 100 + R6[2]) )
  // ----------------- TOKENS
  // 0: X-RWT Repo NFT
  // 1: X-RWT
  
  val GuardNFT = fromBase64("GUARD_NFT");
  sigmaProp(INPUTS(0).tokens(0)._1 == GuardNFT)
}