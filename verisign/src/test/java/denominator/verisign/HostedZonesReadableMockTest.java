package denominator.verisign;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.squareup.okhttp.mockwebserver.MockResponse;

import static denominator.verisign.VerisignMDNSTest.getZoneListRes;

import denominator.DNSApiManager;

public class HostedZonesReadableMockTest {

  @Rule
  public final MockVerisignMDNSServer server = new MockVerisignMDNSServer();

  @Test
  public void singleRequestOnSuccess() throws Exception {
    server.enqueue(getZoneListRes);

    DNSApiManager api = server.connect();
    assertTrue(api.checkConnection());

    server.assertRequest();
  }

  @Test
  public void singleRequestOnFailure() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(500));

    DNSApiManager api = server.connect();
    assertFalse(api.checkConnection());

    server.assertRequest();
  }
}
