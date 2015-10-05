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

final class VerisignMDNSEncoder implements Encoder {

  private static final String NS_API_1 = "api1";
  private static final String NS_API_2 = "api2";

  @SuppressWarnings("unchecked")
  @Override
  public void encode(Object object, Type bodyType, RequestTemplate template) throws EncodeException {

    Map<String, ?> params = (Map<String, ?>) object;

    Node node = null;
    if (params.containsKey("rrSet")) {
      node = encodeRRSet(params);
    } else if (params.containsKey("createZone")) {
      node = encodeCreateZone(params);
    } else if (params.containsKey("getZone")) {
      node = encodeGetZone(params);
    } else if (params.containsKey("getZoneList")) {
      node = encodeGetZoneList(params);
    } else if (params.containsKey("deleteZone")) {
      node = encodeDeleteZone(params);
    } else if (params.containsKey("getRRList")) {
      node = encodeGetRRList(params);
    } else if (params.containsKey("deleteRRSet")) {
      node = encodeDeleteRRSet(params);
    } else {
      throw new EncodeException("Unsupported param key");
    }

    template.body(node.toXml());
  }

  private Node encodeCreateZone(Map<String, ?> params) {

    Object createZoneObj = params.get("createZone");
    if (createZoneObj == null) {
      return null;
    }

    Zone zone = Zone.class.cast(createZoneObj);
    TagNode zoneNode = new TagNode(NS_API_1, "createZone");
    zoneNode.add(new TagNode(NS_API_1, "domainName").add(new TextNode(zone.name())));
    zoneNode.add(new TagNode(NS_API_1, "type").add(new TextNode("DNS Hosting")));
    return zoneNode;
  }

  private Node encodeGetZone(Map<String, ?> params) {
    
    Object getZoneObj = params.get("getZone");
    if (getZoneObj == null) {
      return null;
    }

    String zoneName = (String) getZoneObj;
    TagNode zoneNode = new TagNode(NS_API_1, "getZoneInfo");
    zoneNode.add(new TagNode(NS_API_1, "domainName").add(new TextNode(zoneName)));
    return zoneNode;
  }

  private Node encodeGetZoneList(Map<String, ?> params) {

    Object getZoneListObj = params.get("getZoneList");
    if (getZoneListObj == null) {
      return null;
    }

    Paging paging = Paging.class.cast(getZoneListObj);
    TagNode zoneListNode = new TagNode(NS_API_1, "getZoneList");
    Node pagingNode = toPagingNode(paging);
    zoneListNode.add(pagingNode);
    return zoneListNode;
  }

  private Node encodeDeleteZone(Map<String, ?> params) {

    Object delteZoneObject = params.get("deleteZone");
    if (delteZoneObject == null) {
      return null;
    }

    String zoneName = (String) delteZoneObject;
    return new TagNode(NS_API_1, "deleteZone").add(new TagNode(NS_API_1, "domainName")
        .add(new TextNode(zoneName)));
  }

  private Node encodeDeleteRRSet(Map<String, ?> params) {

    Object delteRrSetObject = params.get("deleteRRSet");
    if (delteRrSetObject == null) {
      return null;
    }

    String zoneName = (String) params.get("zone");
    Node deleteRRs = null;
    if (delteRrSetObject != null) {
      ResourceRecordSet<?> oldRRSet = ResourceRecordSet.class.cast(delteRrSetObject);
      deleteRRs = toRRNode(NS_API_2, "deleteResourceRecords", oldRRSet, false);
    }

    TagNode bulkUpdateZoneNode = new TagNode(NS_API_2, "bulkUpdateSingleZone");
    bulkUpdateZoneNode.add(new TagNode(NS_API_2, "domainName").add(new TextNode(zoneName)));
    bulkUpdateZoneNode.add(deleteRRs);
    return bulkUpdateZoneNode;
  }

  private Node toPagingNode(Paging paging) {

    TagNode pagingNode = new TagNode(NS_API_1, "listPagingInfo");
    pagingNode.add(new TagNode(NS_API_1, "pageNumber").add(new TextNode(Integer
        .toString(paging.pageNumber))));
    pagingNode.add(new TagNode(NS_API_1, "pageSize").add(new TextNode(Integer
        .toString(paging.pageSize))));

    return pagingNode;
  }

  private Node encodeGetRRList(Map<String, ?> params) {

    Object getRRListObj = params.get("getRRList");
    String zoneName = (String) params.get("zone");
    if (getRRListObj == null) {
      return null;
    }

    GetRRList getRRList = GetRRList.class.cast(getRRListObj);
    TagNode getRRListNode = new TagNode(NS_API_1, "getResourceRecordList");
    getRRListNode.add(new TagNode(NS_API_1, "domainName").add(new TextNode(zoneName)));

    if (getRRList.ownerName != null) {
      getRRListNode.add(new TagNode(NS_API_1, "owner").add(new TextNode(getRRList.ownerName)));
    }

    if (getRRList.type != null) {
      getRRListNode.add(new TagNode(NS_API_1, "resourceRecordType").add(new TextNode(getRRList.type)));
    }

    if (getRRList.paging != null) {
      getRRListNode.add(toPagingNode(getRRList.paging));
    }

    return getRRListNode;
  }

  private Node encodeRRSet(Map<String, ?> params) {

    Object rrSetObject = params.get("rrSet");
    if (rrSetObject == null) {
      return null;
    }

    Object oldRRSetObject = params.get("oldRRSet");
    String zoneName = (String) params.get("zone");
    Node deleteRRs = null;
    if (oldRRSetObject != null) {
      ResourceRecordSet<?> oldRRSet = ResourceRecordSet.class.cast(oldRRSetObject);
      deleteRRs = toRRNode(NS_API_2, "deleteResourceRecords", oldRRSet, false);
    }

    ResourceRecordSet<?> rrSet = ResourceRecordSet.class.cast(rrSetObject);
    Node createRRs = toRRNode(NS_API_2, "createResourceRecords", rrSet, true);
    TagNode bulkUpdateZoneNode = new TagNode(NS_API_2, "bulkUpdateSingleZone");
    bulkUpdateZoneNode.add(new TagNode(NS_API_2, "domainName").add(new TextNode(zoneName)));
    bulkUpdateZoneNode.add(createRRs);
    if (deleteRRs != null) {
      bulkUpdateZoneNode.add(deleteRRs);
    }
    return bulkUpdateZoneNode;
  }

  private Node toRRNode(String ns, String tag, ResourceRecordSet<?> rrSet, boolean includeTtl) {

    String name = rrSet.name();
    String type = rrSet.type();
    Integer ttl = rrSet.ttl();
    TagNode rrsNode = new TagNode(ns, tag);
    for (Map<String, Object> record : rrSet.records()) {
      TagNode rrNode = new TagNode(ns, "resourceRecord");
      rrNode.add(new TagNode(ns, "owner").add(new TextNode(name)));
      rrNode.add(new TagNode(ns, "type").add(new TextNode(type)));
      rrNode.add(new TagNode(ns, "rData").add(new TextNode(Util.flatten(record))));
      if (includeTtl && ttl != null) {
        rrNode.add(new TagNode(ns, "ttl").add(new TextNode(ttl.toString())));
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
    private final String ns;
    private final List<Node> children;

    TagNode(String ns, String tag) {
      this.tag = tag;
      this.ns = ns;
      this.children = new ArrayList<Node>();
    }

    String getTag() {
      return tag;
    }

    List<Node> getChildren() {
      return children;
    }

    TagNode add(Node node) {
      children.add(node);
      return this;
    }

    @Override
    public String toXml() {

      StringBuilder sb = new StringBuilder();
      sb.append("<").append(ns).append(":").append(tag).append(">");
      for (Node child : children) {
        sb.append(child.toXml());
      }
      sb.append("</").append(ns).append(":").append(tag).append(">");

      return sb.toString();
    }
  }
}
