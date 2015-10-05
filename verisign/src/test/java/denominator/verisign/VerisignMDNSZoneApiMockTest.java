package denominator.verisign;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import com.squareup.okhttp.mockwebserver.MockResponse;

import denominator.ZoneApi;
import denominator.model.Zone;

public class VerisignMDNSZoneApiMockTest {

  @Rule
  public final MockVerisignMDNSServer server = new MockVerisignMDNSServer();

  @Test
  public void iteratorWhenPresent() throws Exception {

    server.enqueue(getZoneListRes);
    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterator()).containsExactly(
        Zone.create("denominator.io", "denominator.io", 86400, "nil@denominator.io"));
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody("<api1:getZoneList></api1:getZoneList>"));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterator()).isEmpty();
  }

  @Test
  public void iterateByNameWhenPresent() throws Exception {

    server.enqueue(getZoneInfoRes);
    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io")).containsExactly(
        Zone.create("denominator.io", "denominator.io", 86400, "nil@denominator.io"));
  }

  @Test
  public void iterateByNameWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody("<ns4:getZoneInfoRes></ns4:getZoneInfoRes>"));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterateByName("denominator.io.")).isEmpty();
  }

  @Test
  public void putWhenPresent() throws Exception {
    server.enqueueError("ERROR_OPERATION_FAILURE",
        "Domain already exists. Please verify your domain name.");

    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create("denominator.io", "denominator.io", 86400, "nil@denominator.io");
    api.put(zone);
  }

  @Test
  public void putWhenAbsent() throws Exception {

    server.enqueue(new MockResponse());
    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create("denominator.io", "denominator.io", 86400, "nil@denominator.io");
    assertThat(api.put(zone)).isEqualTo(zone.name());
  }

  @Test
  public void deleteWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(
        "<ns4:dnsaWSRes xmlns=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns4=\"urn:com:verisign:dnsa:api:schema:1\">"
            + "   <ns4:callSuccess>true</ns4:callSuccess>"
            + "</ns4:dnsaWSRes>"
       ));

    ZoneApi api = server.connect().api().zones();    
    api.delete("denominator.io.");    
  }

  @Test
  public void deleteWhenAbsent() throws Exception {
    server.enqueueError("ERROR_OPERATION_FAILURE", "The domain name could not be found.");

    ZoneApi api = server.connect().api().zones();
    api.delete("test.io");
  }

  private MockResponse getZoneListRes = new MockResponse().setBody(
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
  
  private MockResponse getZoneInfoRes = new MockResponse().setBody(
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
       + "     <ns4:ttl>23456</ns4:ttl>"
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
}
