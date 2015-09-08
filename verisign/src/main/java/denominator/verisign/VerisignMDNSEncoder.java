package denominator.verisign;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.google.common.base.Charsets;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;

class VerisignMDNSEncoder implements Encoder {

  private final JAXBHelper jaxbHelper;

  @Inject
  public VerisignMDNSEncoder(JAXBHelper jaxbHelper) {
    this.jaxbHelper = jaxbHelper;
  }

  @Override
  public void encode(Object obj, Type bodyType, RequestTemplate template) throws EncodeException {

    @SuppressWarnings("unchecked")
    Map<String, JAXBElement<?>> formParams = Map.class.cast(obj);

    if (formParams.containsKey("soapObject")) {

      JAXBElement<?> soapObject = formParams.get("soapObject");

      ByteArrayOutputStream baos = null;

      try {

        JAXBContext jc = jaxbHelper.getJAXBContext(soapObject.getValue().getClass());

        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

        baos = new ByteArrayOutputStream();

        marshaller.marshal(soapObject, baos);

        template.body(baos.toByteArray(), Charsets.UTF_8);

      } catch (JAXBException e) {
        throw new EncodeException(e.getMessage(), e);
      } finally {
        close(baos);
      }

    }

  }

  private void close(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }


}
