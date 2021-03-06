package denominator.verisign;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import denominator.model.Zone;
import feign.sax.SAXDecoder.ContentHandlerWithResult;

final class VerisignMDNSContentHandlers {

  static abstract class ElementHandler extends DefaultHandler {

    protected Deque<String> elements = null;
    String parentEl;

    ElementHandler(String parentEl) {
      this.parentEl = parentEl;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {

      if (parentEl.equals(qName)) {
        elements = new ArrayDeque<String>();
      }

      if (elements != null) {
        elements.push(qName);
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

      if (elements != null) {
        elements.pop();
      }

      if (parentEl.equals(qName)) {
        elements = null;
      }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      if (elements == null) {
        return;
      }

      processElValue(elements.peek(), ch, start, length);
    }

    protected abstract void processElValue(String currentEl, char[] ch, int start, int length);
  }

  static class ZoneHandler extends ElementHandler implements ContentHandlerWithResult<Zone> {
    Zone zone = null;
    String domainName;
    String email;
    int ttl;
    int count = 0;

    ZoneHandler() {
      super("ns4:getZoneInfoRes");
    }

    @Override
    protected void processElValue(String currentEl, char[] ch, int start, int length) {
      if ("ns4:domainName".equals(currentEl)) {
        domainName = val(ch, start, length);
      } else if ("ns4:email".equals(currentEl)) {
        email = val(ch, start, length);
      } else if ("ns4:ttl".equals(currentEl)) {
        ttl = Integer.valueOf(val(ch, start, length));
      }
    }

    @Override
    public Zone result() {
      if(domainName != null)
        zone = Zone.create(domainName, domainName, ttl, email);
      return zone;
    }
  }

  static class ZoneListHandler extends ElementHandler implements
      ContentHandlerWithResult<Page<Zone>> {

    int count = 0;
    List<Zone> zones = new ArrayList<Zone>();

    ZoneListHandler() {
      super("ns4:getZoneListRes");
    }

    @Override
    protected void processElValue(String currentEl, char[] ch, int start, int length) {

      if ("ns4:totalCount".equals(currentEl)) {
        String value = val(ch, start, length);
        count = Integer.valueOf(value);
      } else if ("ns4:domainName".equals(currentEl)) {
        String value = val(ch, start, length);
        zones.add(Zone.create(value, value, 86400, "nil@" + value));
      }
    }

    @Override
    public Page<Zone> result() {
      return new Page<Zone>(zones, count);
    }
  }

  static class RRHandler extends ElementHandler implements
      ContentHandlerWithResult<Page<ResourceRecord>> {

    int count = 0;
    List<ResourceRecord> rrList = new ArrayList<ResourceRecord>();

    RRHandler() {
      super("ns4:getResourceRecordListRes");
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {
      super.startElement(uri, localName, qName, attributes);
      if ("ns4:resourceRecord".equals(qName)) {
        rrList.add(new ResourceRecord());
      }
    }

    @Override
    protected void processElValue(String currentEl, char[] ch, int start, int length) {
      if (rrList.isEmpty()) {
        return;
      }

      ResourceRecord resourceRecord = rrList.get(rrList.size() - 1);
      String value = val(ch, start, length);
      if ("ns4:totalCount".equals(currentEl)) {
        count = Integer.valueOf(value);
      } else if ("ns4:resourceRecordId".equals(currentEl)) {
        resourceRecord.id = value;
      } else if ("ns4:owner".equals(currentEl)) {
        resourceRecord.name = value;
      } else if ("ns4:type".equals(currentEl)) {
        resourceRecord.type = value;
      } else if ("ns4:rData".equals(currentEl)) {
        resourceRecord.rdata = value;
      } else if ("ns4:ttl".equals(currentEl)) {
        resourceRecord.ttl = value;
      }
    }

    @Override
    public Page<ResourceRecord> result() {
      return new Page<ResourceRecord>(rrList, count);
    }
  }

  static String val(char[] ch, int start, int length) {
    return new String(ch, start, length).trim();
  }

  static class Page<T> {
    final List<T> list;
    final int count;

    Page(List<T> list, int count) {
      this.list = list;
      this.count = count;
    }
  }

  static class ResourceRecord {
    String id;
    String name;
    String type;
    String rdata;
    String ttl;
  }
}
