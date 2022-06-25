package testUtils

import org.scalatest.propspec.AnyPropSpec
import org.scalatest.matchers.should.Matchers

trait TestSuite extends AnyPropSpec with Matchers {

//  protected val client: Client = TestBoxes.client

//  protected val blockchainHeight: Long = client.getHeight
  // In some test box creation height is 10k less than blockchain height. So it should be more than 10k (11k for assurance)
//  assert(blockchainHeight > 11000)

}
