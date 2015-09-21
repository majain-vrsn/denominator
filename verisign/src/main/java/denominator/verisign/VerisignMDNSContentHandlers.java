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

class VerisignMDNSContentHandlers {
  
  static abstract class ElementHandler extends DefaultHandler {

    protected Deque<String> elements = null;
    String parentEl;

    ElementHandler(String parentEl) {
      this.parentEl = parentEl;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      
      if(parentEl.equals(qName)) {
        elements = new ArrayDeque<String>();
      }
      
      if(elements != null) {
        elements.push(qName);
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
     
      if(elements != null) {
        elements.pop();
      }

      if(parentEl.equals(qName)) {
        elements = null;
      }

    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      if(elements == null) {
        return;
      }
      
      processElValue(elements.peek(), ch, start, length);

    }
    
    protected abstract void processElValue(String currentEl, char[] ch, int start, int length);
  }
  
  static class ZoneListHandler extends ElementHandler implements ContentHandlerWithResult<Page<Zone>> {
    
    List<Zone> zones = new ArrayList<Zone>();
    int count = 0;
    
    ZoneListHandler() {
      super("getZoneListRes");
    }
    
    @Override
    protected void processElValue(String currentEl, char[] ch, int start, int length) {
     
      if("totalCount".equals(currentEl)) {
        String value = val(ch, start, length);
        count = Integer.valueOf(value);
        
      } else if("domainName".equals(currentEl)) {
        String value = val(ch, start, length);
        zones.add(Zone.create(value, value, 0, "nil@" + value));
      }
      
    }

    @Override
    public Page<Zone> result() {
      return new Page<Zone>(zones, count);
    }

  }
  
  static class RRHandler extends ElementHandler implements ContentHandlerWithResult<Page<ResourceRecord>> {
    
    int count = 0;
    List<ResourceRecord> rrList = new ArrayList<ResourceRecord>();
    
    RRHandler() {
      super("getResourceRecordListRes");
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      super.startElement(uri, localName, qName, attributes);
      if("resourceRecord".equals(qName)) {
        rrList.add(new ResourceRecord());
      }
    }


    @Override
    protected void processElValue(String currentEl, char[] ch, int start, int length) {
      
      ResourceRecord resourceRecord = rrList.get(rrList.size() - 1);
      String value = val(ch, start, length);
      
      if("totalCount".equals(currentEl)) {
        count = Integer.valueOf(value);
      } else if("resourceRecordId".equals(currentEl)) {
        resourceRecord.id = value;
      } else if("owner".equals(currentEl)) {
        resourceRecord.name = value;
      } else if("type".equals(currentEl)) {
        resourceRecord.type = value;
      } else if("rdata".equals(currentEl)) {
        resourceRecord.rdata = value;
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
