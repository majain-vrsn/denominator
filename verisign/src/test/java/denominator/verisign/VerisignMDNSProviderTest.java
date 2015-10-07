package denominator.verisign;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.Denominator.create;
import static denominator.Providers.list;
import static denominator.Providers.provide;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import dagger.ObjectGraph;
import denominator.Credentials.MapCredentials;
import denominator.DNSApiManager;
import denominator.Provider;

public class VerisignMDNSProviderTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  private static final Provider PROVIDER = new VerisignMDNSProvider();

  @Test
  public void testVerisignMDNSMetadata() {
    assertThat(PROVIDER.name()).isEqualTo("verisignmdns");
    assertThat(PROVIDER.supportsDuplicateZoneNames()).isFalse();
    assertThat(PROVIDER.credentialTypeToParameterNames()).containsEntry("password",
        Arrays.asList("username", "password"));
  }

  @Test
  public void testVerisignMDNSRegistered() {
    assertThat(list()).contains(PROVIDER);
  }

  @Test
  public void testProviderWiresVerisignMDNSZoneApi() {
    DNSApiManager manager = create(PROVIDER, credentials("username", "password"));
    assertThat(manager.api().zones()).isInstanceOf(VerisignMDNSZoneApi.class);
    manager = create("verisignmdns", credentials("username", "password"));
    assertThat(manager.api().zones()).isInstanceOf(VerisignMDNSZoneApi.class);

    Map<String, String> map = new LinkedHashMap<String, String>();
    map.put("username", "U");
    map.put("password", "P");
    manager = create("verisignmdns", credentials(MapCredentials.from(map)));
    assertThat(manager.api().zones()).isInstanceOf(VerisignMDNSZoneApi.class);
  }

  @Test
  public void testCredentialsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("no credentials supplied. verisignmdns requires username,password");

    create(PROVIDER).api().zones().iterator();
  }

  @Test
  public void testTwoPartCredentialsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("incorrect credentials supplied. verisignmdns requires username,password");

    create(PROVIDER, credentials("customer", "username", "password")).api().zones().iterator();
  }

  @Test
  public void testViaDagger() {
    DNSApiManager manager =
        ObjectGraph.create(provide(new VerisignMDNSProvider()), new VerisignMDNSProvider.Module(),
            credentials("username", "password")).get(DNSApiManager.class);
    assertThat(manager.api().zones()).isInstanceOf(VerisignMDNSZoneApi.class);
  }
}
