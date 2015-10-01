package denominator.verisign;

import static denominator.CredentialsConfiguration.credentials;

import java.util.Iterator;

import org.junit.Test;

import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
import denominator.Denominator;
import denominator.ZoneApi;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

public class VerisignMdnsTest {

 
  @Test
  public void zoneTest() {

    DNSApiManager manager =
        Denominator.create("verisignmdns", credentials("vrsniotteam", "end-points.com"));
    ZoneApi zoneApi = manager.api().zones();

    // Setup test data
    String zoneName = "testzone-" + System.currentTimeMillis() + ".com";
    int ttl = 86400;
    String email = "nil." + zoneName;

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

    System.out.println("\nDone.");
  }

  @Test
  public void rrSetTest() {

    DNSApiManager manager =
        Denominator.create("verisignmdns", credentials("vrsniotteam", "end-points.com"));

    // Setup test data
    String zoneName = "testzone-" + System.currentTimeMillis() + ".com";
    int ttl = 86400;
    String email = "nil." + zoneName;

    // createZone
    System.out.println("\nCreating zone...");
    ZoneApi zoneApi = manager.api().zones();
    String zoneId = zoneApi.put(Zone.create(null, zoneName, ttl, email));

    AllProfileResourceRecordSetApi recordSetsInZoneApi = manager.api().recordSetsInZone(zoneId);

    // Add ResourceRecord record
    System.out.println("\nAdding resource record...");

    // Add A record
    recordSetsInZoneApi.put(ResourceRecordSet.builder().name("www").type("A")
        .add(Util.toMap("A", "127.0.0.1")).build());
    
//    Map<String, Object> x = new HashMap();
//    x.put("CUSTOM", "CUSTOM-VAL");
//    recordSetsInZoneApi.put(ResourceRecordSet.builder().name("www").type("CUSTOM")
//        .add(x).build());

    // Add TLSA record
    recordSetsInZoneApi.put(ResourceRecordSet
        .builder()
        .name("_443._tcp.www")
        .type("TLSA")
        .add(
            Util.toMap("CERT",
                "3 1 1 b760c12119c388736da724df1224d21dfd23bf03366c286de1a4125369ef7de0")).build());

    // Add SMIMEA record
    //    MDNS currently does not support SMIMEA per latest dane-smime draft
    //    https://tools.ietf.org/html/draft-ietf-dane-smime-09
    //    
    // recordSetsInZoneApi
    // .put(ResourceRecordSet
    // .builder()
    // .name("c93f1e400f26708f98cb19d936620da35eec8f72e57f9eec01c1afd6._smimecert")
    // .type("SMIMEA")
    // .add(
    // Util.toMap(
    // "CERT",
    // "3 1 1 b760c12119c388736da724df1224d21dfd23bf03366c286de1a4125369ef7de0"))
    // .build());


    // getResourceRecords
    System.out.println("\nQuerying resource records...");
    Iterator<ResourceRecordSet<?>> rrsIterator = recordSetsInZoneApi.iterator();
    while (rrsIterator.hasNext()) {
      ResourceRecordSet<?> rrs = rrsIterator.next();
      System.out.printf("\t%s", rrs.toString());
      System.out.println();
    }

    // getResourceRecordByName
    System.out.println("\nQuerying A resource record by name...");
    rrsIterator = recordSetsInZoneApi.iterateByName("www");
    while (rrsIterator.hasNext()) {
      ResourceRecordSet<?> rrs = rrsIterator.next();
      System.out.printf("\t%s", rrs.toString());
      System.out.println();
    }

    // getResourceRecordByNameAndType
    System.out.println("\nQuerying A resource record by name and rrType...");
    rrsIterator = recordSetsInZoneApi.iterateByNameAndType("www", "A");
    while (rrsIterator.hasNext()) {
      ResourceRecordSet<?> rrs = rrsIterator.next();
      System.out.printf("\t%s", rrs.toString());
      System.out.println();
    }

    // delete Resource Record
    System.out.println("\nDeleting A resource record...");
    recordSetsInZoneApi.deleteByNameAndType("www", "A");

    // deleteZone
    System.out.println("Deleting zone...");
    zoneApi.delete(zoneName);

    System.out.println("\nDone.");
  }

}
