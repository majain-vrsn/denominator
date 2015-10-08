package denominator.verisign;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import com.squareup.okhttp.mockwebserver.MockResponse;

import denominator.AllProfileResourceRecordSetApi;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;

import static denominator.verisign.VerisignMDNSTest.getResourceRecordListRes;
import static denominator.verisign.VerisignMDNSTest.twoResourceRecordRes;

public class VerisignMDNSResourceRecordSetApiMockTest {

  @Rule
  public final MockVerisignMDNSServer server = new MockVerisignMDNSServer();

  @Test
  public void iteratorWhenPresent() throws Exception {

    server.enqueue(getResourceRecordListRes);
    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        server.connect().api().recordSetsInZone("denominator.io");

    assertThat(recordSetsInZoneApi.iterator()).containsExactly(
        ResourceRecordSet.builder().name("www").type("A").ttl(86400)
            .add(Util.toMap("A", "127.0.0.1")).build());
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueue(new MockResponse()
        .setBody("<ns4:getResourceRecordListRes></ns4:getResourceRecordListRes>"));

    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        server.connect().api().recordSetsInZone("denominator.io");
    assertThat(recordSetsInZoneApi.iterator()).isEmpty();
  }

  @Test
  public void iterateByNameWhenPresent() throws Exception {

    server.enqueue(getResourceRecordListRes);

    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        server.connect().api().recordSetsInZone("denominator.io");

    assertThat(recordSetsInZoneApi.iterateByName("www")).containsExactly(
        ResourceRecordSet.builder().name("www").type("A").ttl(86400)
            .add(Util.toMap("A", "127.0.0.1")).build());
  }

  @Test
  public void iterateByNameWhenAbsent() throws Exception {
    server.enqueue(new MockResponse()
        .setBody("<ns4:getResourceRecordListRes></ns4:getResourceRecordListRes>"));

    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        server.connect().api().recordSetsInZone("denominator.io");
    assertThat(recordSetsInZoneApi.iterator()).isEmpty();
  }

  @Test
  public void putFirstRecordCreatesNewRRSet() throws Exception {
    server
        .enqueue(new MockResponse()
            .setBody("<ns4:getResourceRecordListRes xmlns=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns4=\"urn:com:verisign:dnsa:api:schema:1\">"
                + "   <ns4:callSuccess>true</ns4:callSuccess>"
                + "   <ns4:totalCount>0</ns4:totalCount>" + "</ns4:getResourceRecordListRes>"));
    server.enqueue(getResourceRecordListRes);

    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        server.connect().api().recordSetsInZone("denominator.io");
    assertThat(recordSetsInZoneApi.iterator()).isEmpty();

    recordSetsInZoneApi.put(ResourceRecordSet.builder().name("www").type("A").ttl(86400)
        .add(Util.toMap("A", "127.0.0.1")).build());
  }

  @Test
  public void putSameRecordNoOp() throws Exception {
    server.enqueue(getResourceRecordListRes);
    server.enqueue(getResourceRecordListRes);

    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        server.connect().api().recordSetsInZone("denominator.io");

    recordSetsInZoneApi.put(ResourceRecordSet.builder().name("www").type("A").ttl(86400)
        .add(Util.toMap("A", "127.0.0.1")).build());

    assertThat(recordSetsInZoneApi.iterator()).hasSize(1);
  }

  @Test
  public void putOneRecordReplacesRRSet() throws Exception {
    server.enqueue(twoResourceRecordRes);
    server.enqueue(getResourceRecordListRes);

    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        server.connect().api().recordSetsInZone("denominator.io");
    assertThat(recordSetsInZoneApi.iterator()).hasSize(2);

    recordSetsInZoneApi.put(ResourceRecordSet.builder().name("www").type("A").ttl(86400)
        .add(Util.toMap("A", "127.0.0.1")).build());
  }

  @Test
  public void deleteWhenPresent() throws Exception {
    server.enqueue(getResourceRecordListRes);
    server
        .enqueue(new MockResponse()
            .setBody("<ns4:dnsaWSRes xmlns=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns4=\"urn:com:verisign:dnsa:api:schema:1\">"
                + "   <ns4:callSuccess>true</ns4:callSuccess>" + "</ns4:dnsaWSRes>"));

    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        server.connect().api().recordSetsInZone("denominator.io");
    recordSetsInZoneApi.deleteByNameAndType("www.denominator.io.", "A");
  }

  @Test
  public void deleteWhenAbsent() throws Exception {
    server.enqueueError("ERROR_OPERATION_FAILURE", "The domain name could not be found.");

    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        server.connect().api().recordSetsInZone("denominator.io");
    recordSetsInZoneApi.deleteByNameAndType("www", "A");
  }
}
