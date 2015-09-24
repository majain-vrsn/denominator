package denominator.verisign;

import static feign.Util.UTF_8;
import static java.lang.String.format;

import java.net.URI;
import java.util.List;

import javax.inject.Inject;

import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.Provider;
import feign.Request;
import feign.RequestTemplate;
import feign.Target;

class VerisignMDNSTarget implements Target<VerisignMDNS> {

  //@formatter:off
  static final String SOAP_TEMPLATE =
      ""
          + //
          "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:urn=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:urn1=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:api1=\"urn:com:verisign:dnsa:api:schema:1\" xmlns:api2=\"urn:com:verisign:dnsa:api:schema:2\"> "
          + "   <soap:Header> " + "      <urn:reliableMessageReq> "
          + "         <urn:MessageId>%d</urn:MessageId> " 
          + "         <urn:DuplicateElimination/> "
          + "      </urn:reliableMessageReq> " 
          + "      <urn1:authInfo> "
          + "         <urn1:userToken> " 
          + "            <urn1:userName>%s</urn1:userName> "
          + "            <urn1:password>%s</urn1:password> " 
          + "         </urn1:userToken> "
          + "      </urn1:authInfo> " 
          + "   </soap:Header> " 
          + "   <soap:Body> " 
          + "      %s "
          + "   </soap:Body> " 
          + "</soap:Envelope> ";
  //@formatter:on

  private final Provider provider;
  private final javax.inject.Provider<Credentials> credentials;

  @Inject
  VerisignMDNSTarget(Provider provider, javax.inject.Provider<Credentials> credentials) {
    this.provider = provider;
    this.credentials = credentials;
  }

  @Override
  public Class<VerisignMDNS> type() {
    return VerisignMDNS.class;
  }

  @Override
  public String name() {
    return provider.name();
  }

  @Override
  public String url() {
    return provider.url();
  }

  @Override
  public Request apply(RequestTemplate in) {
    in.insert(0, url());
    List<Object> creds = ListCredentials.asList(credentials.get());
    String xml =
        format(SOAP_TEMPLATE, System.currentTimeMillis(), creds.get(0).toString(), creds.get(1)
            .toString(), new String(in.body(), UTF_8));
    in.body(xml);
    in.header("Host", URI.create(in.url()).getHost());
    in.header("Content-Type", "application/soap+xml");
    return in.request();
  }

}
