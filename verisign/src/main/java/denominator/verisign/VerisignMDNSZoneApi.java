package denominator.verisign;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import denominator.model.Zone;
import denominator.verisign.VerisignMDNSContentHandlers.Page;
import denominator.verisign.VerisignMDNSEncoder.Paging;

final class VerisignMDNSZoneApi implements denominator.ZoneApi {

  private static final int PAGE_SIZE = 100;

  private final VerisignMDNS api;

  @Inject
  VerisignMDNSZoneApi(VerisignMDNS api) {
    this.api = api;
  }

  @Override
  public Iterator<Zone> iterator() {

    final Page<Zone> page = api.getZones(paging(1));
    final int pages = (page.count / PAGE_SIZE) + 1;
    final Iterator<Zone> currentIterator = page.list.iterator();

    if (pages == 1) {
      return currentIterator;
    }

    return new Iterator<Zone>() {
      Iterator<Zone> current = currentIterator;
      int i = 1;

      @Override
      public boolean hasNext() {
        while (!current.hasNext() && i <= pages) {
          Page<Zone> page = api.getZones(paging(++i));
          current = page.list.iterator();
        }
        return current.hasNext();
      }

      @Override
      public Zone next() {
        return current.next();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  public Iterator<Zone> iterateByName(String name) {
    List<Zone> zones = new ArrayList<Zone>();

    Zone zone = null;
    try {
      zone = api.getZone(name);
    } catch (VerisignMDNSException e) {
      if (!e.code().equalsIgnoreCase("ERROR_OPERATION_FAILURE")) {
        throw e;
      }
    }
    
    if (zone != null)
      zones.add(zone);

    return zones.iterator();
  }

  @Override
  public String put(Zone zone) {
    try {
      api.createZone(zone);
    } catch (VerisignMDNSException e) {
      if (!e.code().equalsIgnoreCase("ERROR_OPERATION_FAILURE")) {
        throw e;
      }
    }

    api.updateSoa(zone);    
    return zone.name();
  }

  @Override
  public void delete(String zone) {
    try {
      api.deleteZone(zone);
    } catch (VerisignMDNSException e) {
      if (!e.code().equalsIgnoreCase("ERROR_OPERATION_FAILURE")) {
        throw e;
      }
    }
  }

  private Paging paging(int pageNumber) {
    Paging paging = new Paging();
    paging.pageSize = PAGE_SIZE;
    paging.pageNumber = pageNumber;

    return paging;
  }
}
