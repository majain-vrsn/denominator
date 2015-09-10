package denominator.verisign;

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
import feign.Param;
import feign.RequestLine;

interface VerisignMDNS {

  @RequestLine("POST")
  void updateResourceRecords(
      @Param("soapObject") JAXBElement<BulkUpdateSingleZone> resourceRecordsType);

  @RequestLine("POST")
  void createResourceRecords(
      @Param("soapObject") JAXBElement<CreateResourceRecordsType> resourceRecordType);

  @RequestLine("POST")
  void createZone(@Param("soapObject") JAXBElement<CreateZoneType> createZoneType);

  @RequestLine("POST")
  void deleteZone(@Param("soapObject") JAXBElement<DeleteZoneType> deleteZoneType);

  @RequestLine("POST")
  void cloneZone(@Param("soapObject") JAXBElement<CloneZoneType> cloneZoneType);

  @RequestLine("POST")
  GetZoneListResType getZones(@Param("soapObject") JAXBElement<GetZoneListType> zoneListType);

  @RequestLine("POST")
  GetZoneInfoResTypeV2 getZone(@Param("soapObject") JAXBElement<GetZoneInfoTypeV2> zoneInfoType);

  @RequestLine("POST")
  GetResourceRecordListResType getResourceRecords(
      @Param("soapObject") JAXBElement<GetResourceRecordListType> rrType);

  @RequestLine("POST")
  GetResourceRecordListGenericResType searchResourceRecords(
      @Param("soapObject") JAXBElement<GetResourceRecordListGenericType> rrSearchType);

}
