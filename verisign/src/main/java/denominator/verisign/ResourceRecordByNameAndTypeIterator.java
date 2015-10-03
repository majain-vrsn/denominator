package denominator.verisign;

import static denominator.common.Util.peekingIterator;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import denominator.common.PeekingIterator;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.verisign.VerisignMDNSContentHandlers.Page;
import denominator.verisign.VerisignMDNSContentHandlers.ResourceRecord;
import denominator.verisign.VerisignMDNSEncoder.GetRRList;
import denominator.verisign.VerisignMDNSEncoder.Paging;

class ResourceRecordByNameAndTypeIterator implements Iterator<ResourceRecordSet<?>> {

  private static final int PAGE_SIZE = 100;

  private final VerisignMDNS api;
  private final GetRRList getRRList;
  private PeekingIterator<ResourceRecord> peekingIterator;
  private int currentPage;
  private int totalPages;

  public ResourceRecordByNameAndTypeIterator(VerisignMDNS api, GetRRList getRRList) {
    this.api = api;
    this.getRRList = getRRList;
  }

  @Override
  public boolean hasNext() {
    if (peekingIterator == null || (!peekingIterator.hasNext() && currentPage < totalPages)) {
      initPeekingIterator();
    }
    return peekingIterator.hasNext();
  }

  private void initPeekingIterator() {
    Paging paging = new Paging();
    paging.pageSize = PAGE_SIZE;
    paging.pageNumber = ++currentPage;
    getRRList.paging = paging;
    Page<ResourceRecord> rrPage = api.getResourceRecords(getRRList.zoneName, getRRList);
    totalPages = (rrPage.count / PAGE_SIZE) + 1;
    this.peekingIterator = peekingIterator(rrPage.list.iterator());
  }

  @Override
  public ResourceRecordSet<?> next() {
    if (peekingIterator == null) {
      initPeekingIterator();
    }
    ResourceRecord record = peekingIterator.next();
    if (record == null) {
      return null;
    }

    String type = record.type;
    Builder<Map<String, Object>> builder =
        ResourceRecordSet.builder().name(record.name).type(type).ttl(Integer.valueOf(record.ttl));
    builder.add(getRRTypeAndRdata(type, record.rdata));

    while (hasNext()) {
      ResourceRecord next = peekingIterator.peek();
      if (fqdnAndTypeEquals(next, record)) {
        peekingIterator.next();
        builder.add(getRRTypeAndRdata(type, next.rdata));
      } else {
        break;
      }
    }
    return builder.build();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  private static boolean fqdnAndTypeEquals(ResourceRecord actual, ResourceRecord expected) {
    return actual.name.equals(expected.name) && actual.type.equals(expected.type);
  }

  private static Map<String, Object> getRRTypeAndRdata(String type, String rdata) {
    try {
      return Util.toMap(type, rdata);
    } catch (IllegalArgumentException e) {
      Map<String, Object> map = new LinkedHashMap<String, Object>();
      map.put(type, rdata);
      return map;
    }
  }
}
