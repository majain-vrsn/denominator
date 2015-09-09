package denominator.verisign;

import static denominator.CredentialsConfiguration.credentials;

import java.util.Iterator;

import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
import denominator.Denominator;
import denominator.model.ResourceRecordSet;

public class DenominatorTestRunner {

  public static void main(String[] args) {

    // DNSApiManager manager = Denominator.create("verisignmdns", credentials("vrsniotteam", ""));
    DNSApiManager manager =
        Denominator.create("verisignmdns", credentials("vrsniotteam", "end-points.com"));

    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        manager.api().recordSetsInZone("end-points.com");

    Iterator<ResourceRecordSet<?>> rrsIterator = recordSetsInZoneApi.iterator();

    while (rrsIterator.hasNext()) {
      ResourceRecordSet<?> rrs = rrsIterator.next();
      System.out.printf("\n\t%s", rrs.toString());// .println(rrs.name());
    }



  }

}
