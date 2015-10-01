package denominator.verisign;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import com.squareup.okhttp.mockwebserver.MockResponse;

import denominator.AllProfileResourceRecordSetApi;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;

public class VerisignMDNSResourceRecordSetApiMockTest {

  @Rule
  public final MockVerisignMDNSServer server = new MockVerisignMDNSServer();

  @Test
  public void iteratorWhenPresent() throws Exception {
    
    server.enqueue(getResourceRecordListRes);
    AllProfileResourceRecordSetApi recordSetsInZoneApi = server.connect().api().recordSetsInZone("denominator.io");

    assertThat(recordSetsInZoneApi.iterator()).containsExactly(
        ResourceRecordSet.builder().name("www.denominator.io.").type("A").ttl(86400)
        .add(Util.toMap("A", "127.0.0.1")).build());
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody("<ns4:getResourceRecordListRes></ns4:getResourceRecordListRes>"));

    AllProfileResourceRecordSetApi recordSetsInZoneApi = server.connect().api().recordSetsInZone("denominator.io");
    assertThat(recordSetsInZoneApi.iterator()).isEmpty();
  }

  @Test
  public void iterateByNameWhenPresent() throws Exception {

    server.enqueue(getResourceRecordListRes);
    
    AllProfileResourceRecordSetApi recordSetsInZoneApi = server.connect().api().recordSetsInZone("denominator.io");

    assertThat(recordSetsInZoneApi.iterateByName("www.denominator.io")).containsExactly(
        ResourceRecordSet.builder().name("www.denominator.io.").type("A").ttl(86400)
        .add(Util.toMap("A", "127.0.0.1")).build());
  }

  @Test
  public void iterateByNameWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody("<ns4:getResourceRecordListRes></ns4:getResourceRecordListRes>"));

    AllProfileResourceRecordSetApi recordSetsInZoneApi = server.connect().api().recordSetsInZone("denominator.io");
    assertThat(recordSetsInZoneApi.iterator()).isEmpty();
  }
  
  @Test
  public void putFirstRecordCreatesNewRRSet() throws Exception {
    server.enqueue(getResourceRecordListRes);
    
    AllProfileResourceRecordSetApi recordSetsInZoneApi = server.connect().api().recordSetsInZone("denominator.io");

    recordSetsInZoneApi.put(ResourceRecordSet.builder().name("www").type("A").ttl(86400)
        .add(Util.toMap("A", "127.0.0.1")).build());    
  }
  
  @Test
  public void putSameRecordNoOp() throws Exception {
    server.enqueue(getResourceRecordListRes);

    AllProfileResourceRecordSetApi recordSetsInZoneApi = server.connect().api().recordSetsInZone("denominator.io");

    recordSetsInZoneApi.put(ResourceRecordSet.builder().name("www").type("A").ttl(86400)
        .add(Util.toMap("A", "127.0.0.1")).build());
  }  
  
  @Test
  public void putOneRecordReplacesRRSet() throws Exception {
    server.enqueue(twoResourceRecordRes);
    server.enqueue(getResourceRecordListRes);

    AllProfileResourceRecordSetApi recordSetsInZoneApi = server.connect().api().recordSetsInZone("denominator.io");

    recordSetsInZoneApi.put(ResourceRecordSet.builder().name("www").type("A").ttl(86400)
        .add(Util.toMap("A", "127.0.0.1")).build());
  }
  
  @Test
  public void deleteWhenPresent() throws Exception {
    server.enqueue(getResourceRecordListRes);
    server.enqueue(new MockResponse().setBody(
        "<ns4:dnsaWSRes xmlns=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns4=\"urn:com:verisign:dnsa:api:schema:1\">"
            + "   <ns4:callSuccess>true</ns4:callSuccess>"
            + "</ns4:dnsaWSRes>"
       ));

    AllProfileResourceRecordSetApi recordSetsInZoneApi = server.connect().api().recordSetsInZone("denominator.io");
    recordSetsInZoneApi.deleteByNameAndType("www.denominator.io.", "A");
  }

  @Test
  public void deleteWhenAbsent() throws Exception {
    server.enqueueError("ERROR_OPERATION_FAILURE", "The domain name could not be found.");

    AllProfileResourceRecordSetApi recordSetsInZoneApi = server.connect().api().recordSetsInZone("denominator.io");
    recordSetsInZoneApi.deleteByNameAndType("www", "A");
  }

  private MockResponse getResourceRecordListRes = new MockResponse().setBody(
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
  
  private MockResponse twoResourceRecordRes = new MockResponse().setBody(
    "<ns4:getResourceRecordListRes xmlns=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns4=\"urn:com:verisign:dnsa:api:schema:1\">"
        + "   <ns4:callSuccess>true</ns4:callSuccess>"
        + "   <ns4:totalCount>2</ns4:totalCount>"
        + "   <ns4:resourceRecord>"
        + "       <ns4:resourceRecordId>3194802</ns4:resourceRecordId>"
        + "       <ns4:owner>test.denominator.io.</ns4:owner>"
        + "       <ns4:type>A</ns4:type>"
        + "       <ns4:ttl>86400</ns4:ttl>"
        + "       <ns4:rData>127.0.0.11</ns4:rData>"
        + "   </ns4:resourceRecord>"
        + "   <ns4:resourceRecord>"
        + "       <ns4:resourceRecordId>3194811</ns4:resourceRecordId>"
        + "       <ns4:owner>test.denominator.io.</ns4:owner>"
        + "       <ns4:type>A</ns4:type>"
        + "       <ns4:ttl>86400</ns4:ttl>"
        + "       <ns4:rData>127.0.0.12</ns4:rData>"
        + "   </ns4:resourceRecord>"
        + "</ns4:getResourceRecordListRes>"
  );
      
}
