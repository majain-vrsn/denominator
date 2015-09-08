package denominator.verisign;

import java.util.Iterator;

import javax.inject.Inject;
import javax.xml.soap.Detail;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPFault;

import feign.Response;
import feign.codec.DecodeException;
import feign.codec.ErrorDecoder;

class VerisignMDNSErrorDecoder implements ErrorDecoder {

    @Inject
    VerisignMDNSErrorDecoder() { }

	@Override
	public Exception decode(String methodKey, Response response) {
		
		
		try {
			
			SOAPFault soapFault = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage(null, response.body().asInputStream()).getSOAPBody().getFault();
			
			Detail detail = soapFault.getDetail();
			
			if(detail != null) {
				
				@SuppressWarnings("unchecked")
				final Iterator<SOAPElement> detailEntryIter = detail.getDetailEntries();
				
				while (detailEntryIter.hasNext()) {
					
					SOAPElement detailEntry = detailEntryIter.next();
					
					if("dnsaWSRes".equals(detailEntry.getLocalName())) {
				
						@SuppressWarnings("unchecked")
						final Iterator<SOAPElement> childElementsIter = detailEntry.getChildElements();
			            
						while (childElementsIter.hasNext()) {
						
							final SOAPElement soapElement = childElementsIter.next();
							
							if("reason".equals(soapElement.getLocalName())) {
								return new VerisignMDNSException(soapElement.getAttribute("description"), soapElement.getAttribute("code"));
							}
						
			            }
					}
					
					
				}
		
			}

			return new  VerisignMDNSException("Error getting completing Verisign MDNS SOAP Request");
			

		} catch (Exception e) {
			throw new DecodeException(e.getMessage(), e);
		}
	}


}
