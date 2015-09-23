package denominator.verisign;

import static denominator.CredentialsConfiguration.credentials;

import java.util.Iterator;

import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
import denominator.Denominator;
import denominator.ZoneApi;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

public class DenominatorTestRunner {

  public static void main(String[] args) {

    DNSApiManager manager =
        Denominator.create("verisignmdns", credentials("vrsniotteam", "end-points.com"));
    
    ZoneApi zoneApi = manager.api().zones();
    
    //zoneApi.delete("hasixtest.com");
    
    zoneApi.put(Zone.create("hasixtest.com"));
    
    Iterator<Zone> zoneIterator = zoneApi.iterateByName("hasixtest.com");
    
    while(zoneIterator.hasNext()) {
      System.out.println(zoneIterator.next().name());
    }

    /*
    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        manager.api().recordSetsInZone("end-points.com");

    Iterator<ResourceRecordSet<?>> rrsIterator = recordSetsInZoneApi.iterator();

    while (rrsIterator.hasNext()) {
      ResourceRecordSet<?> rrs = rrsIterator.next();
      System.out.printf("\n\t%s", rrs.toString());// .println(rrs.name());
    }
     */


  }

}
