package denominator.verisign;

import static denominator.CredentialsConfiguration.credentials;

import java.util.Iterator;

import org.junit.Test;

import denominator.DNSApiManager;
import denominator.Denominator;
import denominator.ZoneApi;
import denominator.model.Zone;

public class VerisignMdnsTest {

  @Test
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
}
