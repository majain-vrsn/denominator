package denominator.verisign;

import javax.inject.Named;
import javax.xml.bind.JAXBElement;

import mdns.wsdl.BulkUpdateSingleZone;
import mdns.wsdl.CloneZoneType;
import mdns.wsdl.CreateResourceRecordsType;
import mdns.wsdl.CreateZoneType;
import mdns.wsdl.DeleteZoneType;
import mdns.wsdl.GetResourceRecordListGenericResType;
import mdns.wsdl.GetResourceRecordListGenericType;
import mdns.wsdl.GetResourceRecordListResType;
import mdns.wsdl.GetResourceRecordListType;
import mdns.wsdl.GetZoneInfoResTypeV2;
import mdns.wsdl.GetZoneInfoTypeV2;
import mdns.wsdl.GetZoneListResType;
import mdns.wsdl.GetZoneListType;
import feign.RequestLine;

interface VerisignMDNS {

  @RequestLine("POST")
  void updateResourceRecords(
      @Named("soapObject") JAXBElement<BulkUpdateSingleZone> resourceRecordsType);

  @RequestLine("POST")
  void createResourceRecords(
      @Named("soapObject") JAXBElement<CreateResourceRecordsType> resourceRecordType);

  @RequestLine("POST")
  void createZone(@Named("soapObject") JAXBElement<CreateZoneType> createZoneType);

  @RequestLine("POST")
  void deleteZone(@Named("soapObject") JAXBElement<DeleteZoneType> deleteZoneType);

  @RequestLine("POST")
  void cloneZone(@Named("soapObject") JAXBElement<CloneZoneType> cloneZoneType);

  @RequestLine("POST")
  GetZoneListResType getZones(@Named("soapObject") JAXBElement<GetZoneListType> zoneListType);

  @RequestLine("POST")
  GetZoneInfoResTypeV2 getZone(@Named("soapObject") JAXBElement<GetZoneInfoTypeV2> zoneInfoType);

  @RequestLine("POST")
  GetResourceRecordListResType getResourceRecords(
      @Named("soapObject") JAXBElement<GetResourceRecordListType> rrType);

  @RequestLine("POST")
  GetResourceRecordListGenericResType searchResourceRecords(
      @Named("soapObject") JAXBElement<GetResourceRecordListGenericType> rrSearchType);

}
