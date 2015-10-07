package denominator.verisign;

import static denominator.CredentialsConfiguration.credentials;
import static feign.Util.emptyToNull;
import static java.lang.System.getProperty;

import java.util.Iterator;

import javax.inject.Singleton;

import org.junit.Test;

import dagger.Module;
import dagger.Provides;
import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
import denominator.Denominator;
import denominator.ZoneApi;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import feign.Logger;

public class VerisignMDNSTest {

  final DNSApiManager manager;

  public VerisignMDNSTest() throws Exception {
    String username = emptyToNull(getProperty("verisignmdns.username"));
    String password = emptyToNull(getProperty("verisignmdns.password"));
    if (username != null && password != null) {
      manager = create(username, password);
    } else {
      throw new Exception("Missing Authentication data.");
    }
  }

  @Test
  public void zoneTest() {
    ZoneApi zoneApi = manager.api().zones();

    // Setup test data
    String zoneName = "testzone-" + System.currentTimeMillis() + ".io";
    int ttl = 86400;
    String email = "user@" + zoneName;

    // createZone
    System.out.println("\nCreating zone...");
    zoneApi.put(Zone.create(null, zoneName, ttl, email));

    // getZoneInfo
    System.out.println("\nQuerying zone by name...");
    Iterator<Zone> zoneIterator = zoneApi.iterateByName(zoneName);
    while (zoneIterator.hasNext()) {
      System.out.printf("\t%s", zoneIterator.next());
      System.out.println();
    }

    // getZoneList
    System.out.println("\nQuerying zones for an account...");
    zoneIterator = zoneApi.iterator();
    int count = 0;
    while (zoneIterator.hasNext()) {
      zoneIterator.next();
      count++;
    }
    System.out.println("\tZone Size:" + count);

    // deleteZone
    System.out.println("Deleting zone...");
    zoneApi.delete(zoneName);
  }

  @Test
  public void rrSetTest() {

    // Setup test data
    String zoneName = "testzone-" + System.currentTimeMillis() + ".io";
    int ttl = 86400;
    String email = "user@" + zoneName;

    // createZone
    System.out.println("\nCreating zone...");
    ZoneApi zoneApi = manager.api().zones();
    String zoneId = zoneApi.put(Zone.create(null, zoneName, ttl, email));

    AllProfileResourceRecordSetApi recordSetsInZoneApi = manager.api().recordSetsInZone(zoneId);

    // Add ResourceRecord record
    System.out.println("\nAdding resource records...");

    // Add A record
    recordSetsInZoneApi.put(ResourceRecordSet.builder().name("www").type("A")
        .add(Util.toMap("A", "127.0.0.1")).build());

    // Add TLSA record
    recordSetsInZoneApi.put(ResourceRecordSet
        .builder()
        .name("_443._tcp.www")
        .type("TLSA")
        .add(
            Util.toMap("CERT",
                "3 1 1 b760c12119c388736da724df1224d21dfd23bf03366c286de1a4125369ef7de0")).build());

    // getResourceRecords
    System.out.println("\nQuerying resource records...");
    Iterator<ResourceRecordSet<?>> rrsIterator = recordSetsInZoneApi.iterator();
    while (rrsIterator.hasNext()) {
      ResourceRecordSet<?> rrs = rrsIterator.next();
      System.out.printf("\t%s", rrs.toString());
      System.out.println();
    }

    // getResourceRecordByName
    System.out.println("\nQuerying resource record by name...");
    rrsIterator = recordSetsInZoneApi.iterateByName("www");
    while (rrsIterator.hasNext()) {
      ResourceRecordSet<?> rrs = rrsIterator.next();
      System.out.printf("\t%s", rrs.toString());
      System.out.println();
    }

    // getResourceRecordByNameAndType
    System.out.println("\nQuerying resource record by name and rrType...");
    rrsIterator = recordSetsInZoneApi.iterateByNameAndType("www", "A");
    while (rrsIterator.hasNext()) {
      ResourceRecordSet<?> rrs = rrsIterator.next();
      System.out.printf("\t%s", rrs.toString());
      System.out.println();
    }

    // delete Resource Record
    System.out.println("\nDeleting resource record...");
    recordSetsInZoneApi.deleteByNameAndType("www", "A");

    // deleteZone
    System.out.println("Deleting zone...");
    zoneApi.delete(zoneName);
  }

  static DNSApiManager create(String username, String password) {
    VerisignMDNSProvider provider =
        new VerisignMDNSProvider(emptyToNull(getProperty("verisignmdns.url")));
    return Denominator.create(provider, credentials(username, password), new Overrides());
  }

  @Module(overrides = true, library = true)
  static class Overrides {

    @Provides
    @Singleton
    Logger.Level provideLevel() {
      return Logger.Level.FULL;
    }

    @Provides
    @Singleton
    Logger provideLogger() {
      return new Logger.JavaLogger().appendToFile("build/http-wire.log");
    }
  }
}
