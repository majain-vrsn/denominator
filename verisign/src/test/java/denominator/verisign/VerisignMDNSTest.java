package denominator.verisign;

import java.util.Iterator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;

import com.squareup.okhttp.mockwebserver.MockResponse;

import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
import denominator.Live;
import denominator.Live.UseTestGraph;
import denominator.ZoneApi;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

@RunWith(Live.class)
@UseTestGraph(VerisignMDNSTestGraph.class)
public class VerisignMDNSTest {

  @Parameter
  public DNSApiManager manager;

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
  
  static MockResponse getZoneListRes = new MockResponse().setBody(
      "<ns4:getZoneListRes xmlns=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns4=\"urn:com:verisign:dnsa:api:schema:1\">"
          + "   <ns4:callSuccess>true</ns4:callSuccess>"
          + "   <ns4:totalCount>1</ns4:totalCount>"
          + "     <ns4:zoneInfo>"
          + "       <ns4:domainName>denominator.io</ns4:domainName>"
          + "       <ns4:type>DNS Hosting</ns4:type>"
          + "       <ns4:status>ACTIVE</ns4:status>"
          + "       <ns4:createTimestamp>2015-09-29T01:55:39.000Z</ns4:createTimestamp>"
          + "       <ns4:updateTimestamp>2015-09-30T00:25:53.000Z</ns4:updateTimestamp>"
          + "       <ns4:geoLocationEnabled>No</ns4:geoLocationEnabled>"
          + "   </ns4:zoneInfo>"
          + "</ns4:getZoneListRes>"
    );
    
  static MockResponse getZoneInfoRes = new MockResponse().setBody(
      " <ns4:getZoneInfoRes xmlns=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns4=\"urn:com:verisign:dnsa:api:schema:1\">"
         + "    <ns4:callSuccess>true</ns4:callSuccess>"
         + "    <ns4:primaryZoneInfo>"
         + "  <ns4:domainName>denominator.io</ns4:domainName>"
         + "  <ns4:type>DNS Hosting</ns4:type>"
         + "  <ns4:status>ACTIVE</ns4:status>"
         + "  <ns4:createTimestamp>2015-09-29T13:58:53.000Z</ns4:createTimestamp>"
         + "  <ns4:updateTimestamp>2015-09-29T14:41:11.000Z</ns4:updateTimestamp>"
         + "  <ns4:zoneSOAInfo>"
         + "     <ns4:email>nil@denominator.io</ns4:email>"
         + "     <ns4:retry>7400</ns4:retry>"
         + "     <ns4:ttl>86400</ns4:ttl>"
         + "     <ns4:refresh>30000</ns4:refresh>"
         + "     <ns4:expire>1234567</ns4:expire>"
         + "     <ns4:serial>1443535137</ns4:serial>"
         + "  </ns4:zoneSOAInfo>"
         + "  <ns4:serviceLevel>COMPLETE</ns4:serviceLevel>"
         + "  <ns4:webParking>"
         + "     <ns4:parkingEnabled>false</ns4:parkingEnabled>"
         + "  </ns4:webParking>"
         + "  <ns4:verisignNSInfo>"
         + "     <ns4:virtualNameServerId>10</ns4:virtualNameServerId>"
         + "     <ns4:name>a1.verisigndns.com</ns4:name>"
         + "     <ns4:ipAddress>209.112.113.33</ns4:ipAddress>"
         + "     <ns4:ipv6Address>2001:500:7967::2:33</ns4:ipv6Address>"
         + "     <ns4:location>Anycast Global</ns4:location>"
         + "  </ns4:verisignNSInfo>"
         + "  <ns4:verisignNSInfo>"
         + "     <ns4:virtualNameServerId>11</ns4:virtualNameServerId>"
         + "     <ns4:name>a2.verisigndns.com</ns4:name>"
         + "     <ns4:ipAddress>209.112.114.33</ns4:ipAddress>"
         + "     <ns4:ipv6Address>2620:74:19::33</ns4:ipv6Address>"
         + "     <ns4:location>Anycast 1</ns4:location>"
         + "  </ns4:verisignNSInfo>"
         + "  <ns4:verisignNSInfo>"
         + "     <ns4:virtualNameServerId>12</ns4:virtualNameServerId>"
         + "     <ns4:name>a3.verisigndns.com</ns4:name>"
         + "     <ns4:ipAddress>69.36.145.33</ns4:ipAddress>"
         + "     <ns4:ipv6Address>2001:502:cbe4::33</ns4:ipv6Address>"
         + "     <ns4:location>Anycast 2</ns4:location>"
         + "  </ns4:verisignNSInfo>"
         + "    </ns4:primaryZoneInfo>"
         + " </ns4:getZoneInfoRes>"
    );  
  
  static MockResponse getResourceRecordListRes = new MockResponse().setBody(
      "<ns4:getResourceRecordListRes xmlns=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns4=\"urn:com:verisign:dnsa:api:schema:1\">"
          + "   <ns4:callSuccess>true</ns4:callSuccess>"
          + "   <ns4:totalCount>1</ns4:totalCount>"
          + "   <ns4:resourceRecord>"
          + "       <ns4:resourceRecordId>3194811</ns4:resourceRecordId>"
          + "       <ns4:owner>www.denominator.io.</ns4:owner>"
          + "       <ns4:type>A</ns4:type>"
          + "       <ns4:ttl>86400</ns4:ttl>"
          + "       <ns4:rData>127.0.0.1</ns4:rData>"
          + "   </ns4:resourceRecord>"
          + "</ns4:getResourceRecordListRes>"
    );
    
    static MockResponse twoResourceRecordRes = new MockResponse().setBody(
      "<ns4:getResourceRecordListRes xmlns=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns4=\"urn:com:verisign:dnsa:api:schema:1\">"
          + "   <ns4:callSuccess>true</ns4:callSuccess>"
          + "   <ns4:totalCount>2</ns4:totalCount>"
          + "   <ns4:resourceRecord>"
          + "       <ns4:resourceRecordId>3194802</ns4:resourceRecordId>"
          + "       <ns4:owner>www.denominator.io.</ns4:owner>"
          + "       <ns4:type>A</ns4:type>"
          + "       <ns4:ttl>86400</ns4:ttl>"
          + "       <ns4:rData>127.0.0.11</ns4:rData>"
          + "   </ns4:resourceRecord>"
          + "   <ns4:resourceRecord>"
          + "       <ns4:resourceRecordId>3194811</ns4:resourceRecordId>"
          + "       <ns4:owner>www1.denominator.io.</ns4:owner>"
          + "       <ns4:type>A</ns4:type>"
          + "       <ns4:ttl>86400</ns4:ttl>"
          + "       <ns4:rData>127.0.0.12</ns4:rData>"
          + "   </ns4:resourceRecord>"
          + "</ns4:getResourceRecordListRes>"
    );  
}
