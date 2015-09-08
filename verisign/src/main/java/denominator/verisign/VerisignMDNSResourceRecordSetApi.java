package denominator.verisign;

import static denominator.common.Util.nextOrNull;

import java.util.Iterator;

import javax.inject.Inject;

import mdns.wsdl.GetResourceRecordListType;
import mdns.wsdl.ResourceRecordType;
import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

final class VerisignMDNSResourceRecordSetApi implements denominator.ResourceRecordSetApi {

  private final VerisignMDNS api;
  private final VerisignMDNSAllProfileResourceRecordSetApi allApi;
  private final String zoneName;

  public VerisignMDNSResourceRecordSetApi(VerisignMDNSAllProfileResourceRecordSetApi allApi,
      VerisignMDNS api, String zoneName) {
    this.allApi = allApi;
    this.api = api;
    this.zoneName = zoneName;
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    return allApi.iterator();
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    return allApi.iterateByName(name);
  }

  @Override
  public ResourceRecordSet<?> getByNameAndType(String name, String type) {

    GetResourceRecordListType rrListType = new GetResourceRecordListType();
    rrListType.setDomainName(zoneName);
    rrListType.setOwner(name);
    rrListType.setResourceRecordType(ResourceRecordType.fromValue(type));

    return nextOrNull(new ResourceRecordByNameAndTypeIterator(api, rrListType));
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    allApi.put(rrset);
  }

  @Override
  public void deleteByNameAndType(String name, String type) {
    allApi.deleteByNameAndType(name, type);
  }

  static final class Factory implements denominator.ResourceRecordSetApi.Factory {

    private final VerisignMDNS api;
    private final VerisignMDNSAllProfileResourceRecordSetApi.Factory allApi;

    @Inject
    Factory(VerisignMDNSAllProfileResourceRecordSetApi.Factory allApi, VerisignMDNS api) {
      this.allApi = allApi;
      this.api = api;
    }

    @Override
    public ResourceRecordSetApi create(String idOrName) {
      return new VerisignMDNSResourceRecordSetApi(allApi.create(idOrName), api, idOrName);
    }
  }

}
