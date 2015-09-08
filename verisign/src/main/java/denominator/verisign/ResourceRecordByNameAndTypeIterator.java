package denominator.verisign;

import static denominator.common.Util.peekingIterator;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.collect.Range;

import mdns.wsdl.GetResourceRecordListResType;
import mdns.wsdl.GetResourceRecordListType;
import mdns.wsdl.ListPagingInfo;
import mdns.wsdl.ObjectFactory;
import mdns.wsdl.ResourceRecord;

import denominator.common.PeekingIterator;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;

class ResourceRecordByNameAndTypeIterator implements Iterator<ResourceRecordSet<?>> {

  private static final int PAGE_SIZE = 100;

  private final int limit;
  private final VerisignMDNS api;
  private final GetResourceRecordListType rrListType;
  private PeekingIterator<ResourceRecord> peekingIterator;
  private int offset;
  private int totalCount;
  private boolean isPagingRequired = false;

  public ResourceRecordByNameAndTypeIterator(VerisignMDNS api, GetResourceRecordListType rrListType) {
    this.offset = 0;
    this.limit = PAGE_SIZE;
    this.api = api;
    this.rrListType = rrListType;
  }

  public ResourceRecordByNameAndTypeIterator(int offset, int limit, VerisignMDNS api,
      GetResourceRecordListType rrListType) {
    this.offset = offset;
    this.limit = limit;
    this.api = api;
    this.rrListType = rrListType;
    this.isPagingRequired = true;
  }

  @Override
  public boolean hasNext() {

    if (peekingIterator == null
        || (!peekingIterator.hasNext() && !isPagingRequired && offset < totalCount)) {
      initPeekingIterator();
    }

    return peekingIterator.hasNext();

  }

  private void initPeekingIterator() {

    int mdnsLimit = limit;
    int remainder = offset % limit;
    int pageNumber = (offset / limit) + 1;
    int firstIndex = 0;

    if (remainder >= 1) {
      // MDNS is paging by page numbers not offset, account for remainder in the same query

      firstIndex = (offset / limit) * limit;

      Range<Integer> requiredRange = Range.closed(offset, offset + limit);

      Range<Integer> movingRange = Range.closed(firstIndex, firstIndex + mdnsLimit);

      while (!movingRange.encloses(requiredRange)) {
        mdnsLimit++;
        firstIndex = (offset / limit) * mdnsLimit;
        pageNumber = (offset / mdnsLimit) + 1;
        movingRange = Range.closed(firstIndex, firstIndex + mdnsLimit);
      }

    }

    ListPagingInfo paging = new ListPagingInfo();
    paging.setPageSize(mdnsLimit);
    paging.setPageNumber(pageNumber);

    rrListType.setListPagingInfo(paging);

    GetResourceRecordListResType response =
        api.getResourceRecords(new ObjectFactory().createGetResourceRecordList(rrListType));
    this.totalCount = response.getTotalCount();

    int newOffset = Math.abs(firstIndex - offset);

    int returnedRRSetSize = response.getResourceRecord().size();

    if (remainder >= 1 && returnedRRSetSize >= newOffset + limit) {
      this.peekingIterator =
          peekingIterator(response.getResourceRecord().subList(newOffset, newOffset + limit)
              .iterator());
    } else if (remainder >= 1 && totalCount >= newOffset) {
      this.peekingIterator =
          peekingIterator(response.getResourceRecord()
              .subList(newOffset, Math.min(totalCount, newOffset + limit)).iterator());
    } else {
      this.peekingIterator = peekingIterator(response.getResourceRecord().iterator());
    }

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

    String type = record.getType().value();

    Builder<Map<String, Object>> builder =
        ResourceRecordSet.builder().name(record.getOwner()).type(type)
            .ttl(record.getTtl().intValue());

    builder.add(getRRTypeAndRdata(type, record.getRData()));

    offset++;

    while (hasNext()) {

      ResourceRecord next = peekingIterator.peek();

      if (fqdnAndTypeEquals(next, record)) {
        peekingIterator.next();
        offset++;
        builder.add(getRRTypeAndRdata(type, next.getRData()));
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

  public int getTotalCount() {
    return totalCount;
  }

  public static boolean fqdnAndTypeEquals(ResourceRecord actual, ResourceRecord expected) {
    return actual.getOwner().equals(expected.getOwner())
        && actual.getType().equals(expected.getType());
  }

  public static Map<String, Object> getRRTypeAndRdata(String type, String rdata) {

    try {

      return Util.toMap(type, rdata);

    } catch (IllegalArgumentException e) {

      Map<String, Object> map = new LinkedHashMap<String, Object>();
      map.put(type, rdata);
      return map;

    }

  }

}
