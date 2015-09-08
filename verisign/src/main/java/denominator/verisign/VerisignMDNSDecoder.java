package denominator.verisign;

import java.io.IOException;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;

import feign.FeignException;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;

class VerisignMDNSDecoder implements Decoder {

  @Inject
  public VerisignMDNSDecoder(JAXBHelper jaxbHelper) {
    this.jaxbHelper = jaxbHelper;
  }

  private final JAXBHelper jaxbHelper;

  @Override
  public Object decode(Response response, Type type) throws IOException, FeignException {

    try {

      JAXBContext jaxbContext = jaxbHelper.getJAXBContext((Class<?>) type);

      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

      JAXBElement<?> object =
          (JAXBElement<?>) unmarshaller.unmarshal(MessageFactory
              .newInstance(SOAPConstants.SOAP_1_2_PROTOCOL)
              .createMessage(null, response.body().asInputStream()).getSOAPBody()
              .extractContentAsDocument());

      if (object != null) {
        return object.getValue();
      }

      return object;

    } catch (Exception e) {
      throw new DecodeException(e.getMessage(), e);
    }
  }

}
