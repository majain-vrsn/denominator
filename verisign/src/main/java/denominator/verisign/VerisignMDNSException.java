package denominator.verisign;

import feign.FeignException;

final class VerisignMDNSException extends FeignException {

  private static final long serialVersionUID = 1L;
  private final String code;

  VerisignMDNSException(String message, String code) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }

}
