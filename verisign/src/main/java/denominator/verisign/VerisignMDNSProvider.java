package denominator.verisign;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import dagger.Provides;
import denominator.AllProfileResourceRecordSetApi;
import denominator.BasicProvider;
import denominator.CheckConnection;
import denominator.DNSApiManager;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.GeoUnsupported;
import denominator.config.NothingToClose;
import denominator.config.WeightedUnsupported;
import denominator.verisign.VerisignMDNSContentHandlers.RRHandler;
import denominator.verisign.VerisignMDNSContentHandlers.ZoneHandler;
import denominator.verisign.VerisignMDNSContentHandlers.ZoneListHandler;
import denominator.verisign.VerisignMDNSErrorDecoder.VerisignMDNSError;
import feign.Feign;
import feign.Logger;
import feign.Request.Options;
import feign.codec.Decoder;
import feign.sax.SAXDecoder;

public class VerisignMDNSProvider extends BasicProvider {

  private final String url;

  public VerisignMDNSProvider() {
    this(null);
  }

  /**
   * @param url if empty or null use default
   */
  public VerisignMDNSProvider(String url) {
    this.url =
        url == null || url.isEmpty() ? "https://ote-api.verisigndns.com/dnsa-ws/V2.0/dnsaapi?wsdl=1"
            : url;
  }

  @Override
  public String url() {
    return url;
  }

  @Override
  public Set<String> basicRecordTypes() {
    Set<String> types = new LinkedHashSet<String>();
    types.addAll(Arrays.asList("A", "AAAA", "CNAME", "MX", "NAPTR", "NS", "PTR", "SRV", "TXT"));
    return types;
  }

  @Override
  public Map<String, Collection<String>> credentialTypeToParameterNames() {
    Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
    options.put("password", Arrays.asList("username", "password"));
    return options;
  }

  @dagger.Module(injects = {DNSApiManager.class}, complete = false, includes = {NothingToClose.class,
                                                                                WeightedUnsupported.class,
                                                                                GeoUnsupported.class,
                                                                                FeignModule.class})
  public static final class Module {

    @Provides
    CheckConnection checkConnection(HostedZonesReadable checkConnection) {
      return checkConnection;
    }
    
    @Provides
    @Singleton
    ZoneApi provideZoneApi(VerisignMDNSZoneApi api) {
      return api;
    }


    @Provides
    @Singleton
    ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(
        VerisignMDNSResourceRecordSetApi.Factory factory) {
      return factory;
    }

    @Provides
    @Singleton
    AllProfileResourceRecordSetApi.Factory provideAllProfileResourceRecordSetApiFactory(
        VerisignMDNSAllProfileResourceRecordSetApi.Factory factory) {
      return factory;
    }

  }

  @dagger.Module(injects = VerisignMDNSResourceRecordSetApi.Factory.class, complete = false)
  public static final class FeignModule {

    @Provides
    @Singleton
    VerisignMDNS verisignMDNS(Feign feign, VerisignMDNSTarget target) {
      return feign.newInstance(target);
    }

    @Provides
    Logger logger() {
      return new Logger.NoOpLogger();
    }

    @Provides
    Logger.Level logLevel() {
      return Logger.Level.NONE;
    }

    @Provides
    @Singleton
    Feign feign(Logger logger, Logger.Level logLevel) {

      Options options = new Options(10 * 1000, 10 * 60 * 1000);
      Decoder decoder = decoder();

      return Feign.builder()
          .logger(logger)
          .logLevel(logLevel)
          .options(options)
          .encoder(new VerisignMDNSEncoder())
          .decoder(decoder)
          .errorDecoder(new VerisignMDNSErrorDecoder(decoder))
          .build();
    }

    static Decoder decoder() {
      return SAXDecoder.builder()
          .registerContentHandler(RRHandler.class)
          .registerContentHandler(ZoneHandler.class)
          .registerContentHandler(ZoneListHandler.class)
          .registerContentHandler(VerisignMDNSError.class)
          .build();
    }
  }
}
