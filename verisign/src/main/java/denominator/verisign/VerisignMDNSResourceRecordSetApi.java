package denominator.verisign;

import static denominator.common.Util.nextOrNull;

import java.util.Iterator;

import javax.inject.Inject;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.verisign.VerisignMDNSSaxEncoder.GetRRList;

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
    
    GetRRList getRRList = new GetRRList();
    getRRList.ownerName = name;
    getRRList.type = type;
    getRRList.zoneName = zoneName;

    return nextOrNull(new ResourceRecordByNameAndTypeIterator(api, getRRList));
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
