package denominator.verisign;

import javax.inject.Inject;

import denominator.CheckConnection;
import denominator.verisign.VerisignMDNSEncoder.Paging;

public class HostedZonesReadable implements CheckConnection {

  private final VerisignMDNS api;

  @Inject
  HostedZonesReadable(VerisignMDNS api) {
    this.api = api;
  }

  @Override
  public boolean ok() {
    try {
      Paging paging = new Paging();
      paging.pageSize = 1;
      paging.pageNumber = 1;

      api.getZones(paging);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  @Override
  public String toString() {
    return "HostedZonesReadable";
  }
}
