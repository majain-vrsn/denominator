package denominator.verisign;

import static denominator.CredentialsConfiguration.credentials;

import java.util.Iterator;

import org.junit.Ignore;
import org.junit.Test;

import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
import denominator.Denominator;
import denominator.ZoneApi;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

public class VerisignMdnsTest {

  @Ignore
  public void zoneTest() {

    DNSApiManager manager =
        Denominator.create("verisignmdns", credentials("vrsniotteam", "end-points.com"));
    ZoneApi zoneApi = manager.api().zones();

    // Setup test data
    String zoneName = "testzone-" + System.currentTimeMillis() + ".com";
    int ttl = 0;
    String email = "nil." + zoneName;

    // createZone
    String zoneId = zoneApi.put(Zone.create(null, zoneName, ttl, email));
    System.out.println(zoneId);

    // getZoneInfo
    Iterator<Zone> zoneIterator = zoneApi.iterateByName(zoneName);
    while (zoneIterator.hasNext()) {
      System.out.println(zoneIterator.next());
    }

    // getZoneList
    zoneIterator = zoneApi.iterator();
    int count = 0;
    while (zoneIterator.hasNext()) {
      zoneIterator.next();
      count++;
    }
    System.out.println("Zone Size:" + count);


    // deleteZone
    zoneApi.delete(zoneName);
  }

  @Test
  public void rrSetTest() {

    DNSApiManager manager =
        Denominator.create("verisignmdns", credentials("vrsniotteam", "end-points.com"));

    // Setup test data
    String zoneName = "testzone-" + System.currentTimeMillis() + ".com";
    int ttl = 0;
    String email = "nil." + zoneName;

    // createZone
    System.out.println("\nCreating zone: " + zoneName);
    ZoneApi zoneApi = manager.api().zones();
    String zoneId = zoneApi.put(Zone.create(null, zoneName, ttl, email));
    System.out.println(zoneId);

    AllProfileResourceRecordSetApi recordSetsInZoneApi = manager.api().recordSetsInZone(zoneId);

    // Add A record
    System.out.println("\nAdding A resource record to zone: " + zoneName);
    String owner = "test";
    String rrType = "A";
    recordSetsInZoneApi.put(ResourceRecordSet.builder().name(owner).type(rrType)
        .add(Util.toMap("A", "127.0.0.1")).build());

    // getResourceRecords
    System.out.println("\nQuerying resource records for zone: " + zoneName);
    Iterator<ResourceRecordSet<?>> rrsIterator = recordSetsInZoneApi.iterator();
    while (rrsIterator.hasNext()) {
      ResourceRecordSet<?> rrs = rrsIterator.next();
      System.out.printf("\n\t%s", rrs.toString());
    }

    // getResourceRecordByName
    System.out.println("\nQuerying resource record by name: " + owner);
    rrsIterator = recordSetsInZoneApi.iterateByName(owner);
    while (rrsIterator.hasNext()) {
      ResourceRecordSet<?> rrs = rrsIterator.next();
      System.out.printf("\n\t%s", rrs.toString());
    }
    
    // getResourceRecordByNameAndType
    System.out.println("\nQuerying resource record by name and rrType");
    rrsIterator = recordSetsInZoneApi.iterateByNameAndType(owner, rrType);
    while (rrsIterator.hasNext()) {
      ResourceRecordSet<?> rrs = rrsIterator.next();
      System.out.printf("\n\t%s", rrs.toString());
    }

    // delete Resource Record
    System.out.println("\nDeleting resource record");
    recordSetsInZoneApi.deleteByNameAndType(owner, "A");

    // deleteZone
    System.out.println("\nDeleting zone");
    zoneApi.delete(zoneName);

  }

}
