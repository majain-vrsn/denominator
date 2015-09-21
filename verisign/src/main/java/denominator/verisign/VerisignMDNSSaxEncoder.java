package denominator.verisign;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import denominator.common.Util;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;

public class VerisignMDNSSaxEncoder implements Encoder {
  
  private static final String BULK_UPDATE_TAG = "urn2:bulkUpdateSingleZone";
  
  private static final String DOMAIN_NAME_TAG = "urn2:domainName";
 
  private static final String OWNER_TAG = "urn2:owner";
  private static final String TTL_TAG = "urn2:ttl";
  private static final String TYPE_TAG = "urn2:type";
  private static final String RDATA_TAG = "urn2:rData";
  
  private static final String RR_TAG = "urn2:resourceRecord";
  
  private static final String UPDATE_RR_TAG = "urn2:updateResourceRecord";
  
  private static final String OLD_RR_TAG = "urn2:oldResourceRecord";
  
  private static final String NEW_RR_TAG = "urn2:newResourceRecord";
  
  private static final String CREATE_RRS_TAG = "urn2:createResourceRecords";
  
  private static final String UPDATE_RRS_TAG = "urn2:updateResourceRecords";
  
  private static final String DELETE_RRS_TAG = "urn2:deleteResourceRecords";
  
  private static final String GET_RR_LIST_TAG = "urn2:getResourceRecordList";
  
  private static final String RR_TYPE_TAG = "urn2:resourceRecordType";
  
  private static final String PAGING_INFO_TAG = "urn2:listPagingInfo";
  private static final String PAGE_NUMBER_TAG = "urn2:pageNumber";
  private static final String PAGE_SIZE_TAG = "urn2:pageSize";
  
  private static final String CREATE_ZONE_TAG = "urn2:createZone";
  private static final String GET_ZONE_LIZE_TAG = "urn2:getZoneList";
  
  

  @SuppressWarnings("unchecked")
  @Override
  public void encode(Object object, Type bodyType, RequestTemplate template) throws EncodeException {
    
    Map<String, ?> params = (Map<String, ?>) object;
    
    String xml = null;
    
    if(params.containsKey("rrSet")) {
      xml = encodeRRSet(params);
    } else if(params.containsKey("getRRList")) {
      xml = encodeGetRRList(params);
    } else if(params.containsKey("createZone")) {
      xml = encodeCreateZone(params);
    } else if(params.containsKey("getZoneList")) {
      xml = encodeGetZoneList(params);
    } else if(params.containsKey("deleteRRSet")) {
      xml = encodeDeleteRRSet(params);
    }
    
    template.body(xml);
    
  }
  
  private String encodeDeleteRRSet(Map<String, ?> params) {
    
    Object delteRrSetObject = params.get("deleteRRSet");
    
    if(delteRrSetObject == null) {
      return null;
    }
    
    String zoneName = (String) params.get("zone");
    
    Node deleteRRs = null;
    
    if(delteRrSetObject != null) {
      
      ResourceRecordSet<?> oldRRSet = ResourceRecordSet.class.cast(delteRrSetObject);
      
      deleteRRs = toRRNode(DELETE_RRS_TAG, oldRRSet, false);
      
    }
    
    TagNode bulkUpdateZoneNode = new TagNode(BULK_UPDATE_TAG);
    
    bulkUpdateZoneNode.add(new TagNode(DOMAIN_NAME_TAG).add(new TextNode(zoneName)));
    bulkUpdateZoneNode.add(deleteRRs);

    return bulkUpdateZoneNode.toXml();
    
  }

  private String encodeGetZoneList(Map<String, ?> params) {
    
    Object getZoneListObj = params.get("getZoneList");
    
    if(getZoneListObj == null) {
      return null;
    }
    
    Paging paging = Paging.class.cast(getZoneListObj);
    
    TagNode zoneListNode = new TagNode(GET_ZONE_LIZE_TAG);
    
    Node pagingNode = toPagingNode(paging);
    
    zoneListNode.add(pagingNode);
    
    return zoneListNode.toXml();
    
  }
  
  private Node toPagingNode(Paging paging) {
    
    TagNode pagingNode = new TagNode(PAGING_INFO_TAG);
    pagingNode.add(new TagNode(PAGE_NUMBER_TAG).add(new TextNode(Integer.toString(paging.pageNumber))));
    pagingNode.add(new TagNode(PAGE_SIZE_TAG).add(new TextNode(Integer.toString(paging.pageSize))));
    
    return pagingNode;
    
  }

  private String encodeCreateZone(Map<String, ?> params) {
    
    Object createZoneObj = params.get("createZone");
    
    if(createZoneObj == null) {
      return null;
    }
    
    Zone zone = Zone.class.cast(createZoneObj);
    
    TagNode zoneNode = new TagNode(CREATE_ZONE_TAG);
    zoneNode.add(new TagNode(DOMAIN_NAME_TAG).add(new TextNode(zone.name())));
    zoneNode.add(new TagNode(TYPE_TAG).add(new TextNode("DNS Hosting")));
    
    return zoneNode.toXml();
    
  }
  
  private String encodeGetRRList(Map<String, ?> params) {
    
    Object getRRListObj = params.get("getRRList");
    
    String zoneName = (String) params.get("zone");
    
    if(getRRListObj == null) {
      return null;
    }
    
    GetRRList getRRList = GetRRList.class.cast(getRRListObj);
    
    TagNode getRRListNode = new TagNode(GET_RR_LIST_TAG);
    getRRListNode.add(new TagNode(DOMAIN_NAME_TAG).add(new TextNode(zoneName)));
    
    if(getRRList.ownerName != null) {
      getRRListNode.add(new TagNode(OWNER_TAG).add(new TextNode(getRRList.ownerName)));
    }
    
    if(getRRList.type != null) {
      getRRListNode.add(new TagNode(RR_TYPE_TAG).add(new TextNode(getRRList.type)));
    }
    
    if(getRRList.paging != null) {
      getRRListNode.add(toPagingNode(getRRList.paging));
    }
    
    return getRRListNode.toXml();
  }

  private String encodeRRSet(Map<String, ?> params) {
    
    Object rrSetObject = params.get("rrSet");
    
    if(rrSetObject == null) {
      return null;
    }
    
    Object oldRRSetObject = params.get("oldRRSet");
    
    String zoneName = (String) params.get("zone");
    
    Node deleteRRs = null;
    
    if(oldRRSetObject != null) {
      
      ResourceRecordSet<?> oldRRSet = ResourceRecordSet.class.cast(oldRRSetObject);
      
      deleteRRs = toRRNode(DELETE_RRS_TAG, oldRRSet, false);
      
    }
    
    ResourceRecordSet<?> rrSet = ResourceRecordSet.class.cast(rrSetObject);
    
    Node createRRs = toRRNode(CREATE_RRS_TAG, rrSet, true);
    
    TagNode bulkUpdateZoneNode = new TagNode(BULK_UPDATE_TAG);
    
    bulkUpdateZoneNode.add(new TagNode(DOMAIN_NAME_TAG).add(new TextNode(zoneName)));
    bulkUpdateZoneNode.add(createRRs);
    
    if(deleteRRs != null) {
      bulkUpdateZoneNode.add(deleteRRs);
    }
    
    return bulkUpdateZoneNode.toXml();
    
  }
  
  private Node toRRNode(String tag, ResourceRecordSet<?> rrSet, boolean includeTtl) {

    String name = rrSet.name();
    String type = rrSet.type();
    Integer ttl = rrSet.ttl();

    TagNode rrsNode = new TagNode(tag);

    for(Map<String, Object> record : rrSet.records()) {

      TagNode rrNode = new TagNode(RR_TAG);

      rrNode.add(new TagNode(OWNER_TAG).add(new TextNode(name)));
      rrNode.add(new TagNode(TYPE_TAG).add(new TextNode(type)));
      rrNode.add(new TagNode(RDATA_TAG).add(new TextNode(Util.flatten(record))));

      if(includeTtl) {
        rrNode.add(new TagNode(TTL_TAG).add(new TextNode(ttl.toString())));
      }

      rrsNode.add(rrNode);

    }

    return rrsNode;


  }
  
  static class GetRRList {
    String zoneName;
    String ownerName;
    String type;
    String viewName;
    Paging paging;
  }
  
  static class Paging {
    int pageNumber;
    int pageSize;
  }
  
  interface Node {
    String toXml();
  }
  
  class TextNode implements Node {
    
    private final String value;
    
    TextNode(String value) {
      this.value = value;
    }

    @Override
    public String toXml() {
      return value;
    }

  }
  
  class TagNode implements Node {
    
    private final String tag;
    private final List<Node> children;
    
    TagNode(String tag) {
      this.tag = tag;
      this.children = new ArrayList<Node>();
    }

    String getTag() {
      return tag;
    }

    List<Node> getChildren() {
      return children;
    }
    
    TagNode add(Node node){
      children.add(node);
      return this;
    }

    @Override
    public String toXml() {
      
      StringBuilder sb = new StringBuilder();
      sb.append("<").append(tag).append(">");
      for(Node child: children) {
        sb.append(child.toXml());
      }
      sb.append("</").append(tag).append(">");
      
      return sb.toString();
      
    }

  }

  

}
