package denominator.verisign;

import static denominator.CredentialsConfiguration.credentials;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;

import com.squareup.okhttp.mockwebserver.MockResponse;

import dagger.Module;
import dagger.Provides;
import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.DNSApi;
import denominator.Denominator;

public class VerisignMDNSProviderDynamicUpdateMockTest {

  @Rule
  public MockVerisignMDNSServer server = new MockVerisignMDNSServer();

  @Test
  public void dynamicEndpointUpdates() throws Exception {
    final AtomicReference<String> url = new AtomicReference<String>(server.url());
    server.enqueue(new MockResponse().setBody("<api1:getZoneList></api1:getZoneList>"));

    DNSApi api = Denominator.create(new VerisignMDNSProvider() {
      @Override
      public String url() {
        return url.get();
      }
    }, credentials(server.credentials())).api();

    api.zones().iterator();
    server.assertRequest();

    MockVerisignMDNSServer server2 = new MockVerisignMDNSServer();
    url.set(server2.url());
    server2.enqueue(new MockResponse().setBody("<api1:getZoneList></api1:getZoneList>"));

    api.zones().iterator();
    server2.assertRequest();
    server2.shutdown();
  }

  @Test
  public void dynamicCredentialUpdates() throws Exception {
    server.enqueue(new MockResponse()
        .setBody("<ns4:getResourceRecordListRes></ns4:getResourceRecordListRes>"));

    AtomicReference<Credentials> dynamicCredentials =
        new AtomicReference<Credentials>(server.credentials());

    DNSApi api = Denominator.create(server, new OverrideCredentials(dynamicCredentials)).api();

    api.zones().iterator();
    server.assertRequest();

    dynamicCredentials.set(ListCredentials.from("bob", "comeon"));
    server.credentials("bob", "comeon");
    server.enqueue(new MockResponse()
        .setBody("<ns4:getResourceRecordListRes></ns4:getResourceRecordListRes>"));

    api.zones().iterator();
    server.assertRequest();
  }

  @Module(complete = false, library = true, overrides = true)
  static class OverrideCredentials {

    final AtomicReference<Credentials> dynamicCredentials;

    OverrideCredentials(AtomicReference<Credentials> dynamicCredentials) {
      this.dynamicCredentials = dynamicCredentials;
    }

    @Provides
    public Credentials get() {
      return dynamicCredentials.get();
    }
  }
}
