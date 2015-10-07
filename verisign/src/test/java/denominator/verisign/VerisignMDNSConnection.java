package denominator.verisign;

import static denominator.CredentialsConfiguration.credentials;
import static feign.Util.emptyToNull;
import static java.lang.System.getProperty;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.Denominator;
import feign.Logger;

public class VerisignMDNSConnection {

  final DNSApiManager manager;
  final String mutableZone;

  VerisignMDNSConnection() {
    String username = emptyToNull(getProperty("verisignmdns.username"));
    String password = emptyToNull(getProperty("verisignmdns.password"));
    if (username != null && password != null) {
      manager = create(username, password);
    } else {
      manager = null;
    }
    mutableZone = emptyToNull(getProperty("verisignmdns.zone"));
  }

  static DNSApiManager create(String username, String password) {
    VerisignMDNSProvider provider =
        new VerisignMDNSProvider(emptyToNull(getProperty("verisignmdns.url")));
    return Denominator.create(provider, credentials(username, password), new Overrides());
  }

  @Module(overrides = true, library = true)
  static class Overrides {

    @Provides
    @Singleton
    Logger.Level provideLevel() {
      return Logger.Level.FULL;
    }

    @Provides
    @Singleton
    Logger provideLogger() {
      return new Logger.JavaLogger().appendToFile("build/http-wire.log");
    }
  }
}
