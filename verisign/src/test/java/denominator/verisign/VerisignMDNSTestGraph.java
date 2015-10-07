package denominator.verisign;

import static feign.Util.emptyToNull;
import static java.lang.System.getProperty;
import denominator.DNSApiManagerFactory;

public class VerisignMDNSTestGraph extends denominator.TestGraph {

  private static final String url = emptyToNull(getProperty("verisignmdns.url"));
  private static final String zone = emptyToNull(getProperty("verisignmdns.zone"));

  public VerisignMDNSTestGraph() {
    super(DNSApiManagerFactory.create(new VerisignMDNSProvider(url)), zone);
  }
}
