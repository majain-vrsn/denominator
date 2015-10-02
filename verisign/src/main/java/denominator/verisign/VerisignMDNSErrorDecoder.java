package denominator.verisign;

import static java.lang.String.format;

import java.io.IOException;

import javax.inject.Inject;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import feign.FeignException;
import feign.Response;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import feign.sax.SAXDecoder.ContentHandlerWithResult;

class VerisignMDNSErrorDecoder implements ErrorDecoder {

  private final Decoder decoder;

  VerisignMDNSErrorDecoder(Decoder decoder) {
    this.decoder = decoder;
  }

  @Override
  public Exception decode(String methodKey, Response response) {

    try {
      VerisignMDNSError error =
          VerisignMDNSError.class.cast(decoder.decode(response, VerisignMDNSError.class));
      if (error == null) {
        return FeignException.errorStatus(methodKey, response);
      }

      String message = format("%s failed", methodKey);
      if (error.code != null) {
        message = format("%s with error %s", message, error.code);
      }
      if (error.description != null) {
        message = format("%s: %s", message, error.description);
      }
      return new VerisignMDNSException(message, error.code);
    } catch (IOException ignored) {
      return FeignException.errorStatus(methodKey, response);
    } catch (Exception propagate) {
      return propagate;
    }

  }

  static class VerisignMDNSError extends DefaultHandler implements
      ContentHandlerWithResult<VerisignMDNSError> {

    private String description;
    private String code;

    @Inject
    VerisignMDNSError() {
      
    }

    @Override
    public VerisignMDNSError result() {
      return (code == null && description == null) ? null : this;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {

      if ("ns4:reason".equals(qName)) {
        String description = attributes.getValue("description");
        if (description != null) {
          this.description = description;
        }
        String code = attributes.getValue("code");
        if (code != null) {
          this.code = code;
        }
      }
    }
  }
}
